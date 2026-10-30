package com.example

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class FpAccessibilityService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private lateinit var kg: KeyguardManager
    private lateinit var pw: PowerManager
    private lateinit var dm: DisplayManager

    private val ui = Handler(Looper.getMainLooper())
    private var bgThread: HandlerThread? = null
    private var bgHandler: Handler? = null

    private var view: OverlayAnimHostView? = null
    private var viewLp: WindowManager.LayoutParams? = null
    private var viewAttached = false
    private var aodWakeLock: PowerManager.WakeLock? = null

    @Volatile private var promptVisible = false
    @Volatile private var shownOnLock = false
    @Volatile private var wasKeyguardLockedOnTouch = false
    @Volatile private var isShowingAnim = false
    @Volatile private var fingerDown = false
    @Volatile private var suppressUntilFingerUp = false
    @Volatile private var touchDownTimeMs = 0L
    @Volatile private var retryCount = 0
    @Volatile private var isInAodOrDoze = false

    // Single optical scan attempt timeout: if a finger is held down and not recognized after 1.25s,
    // disable the animation until the user lifts their finger and places it again.
    private val singleScanTimeout = Runnable {
        if (fingerDown && isShowingAnim) {
            suppressUntilFingerUp = true
            hide("fingerprint not recognized (scan timeout)")
        }
    }

    private val watchdog = Runnable { hide("watchdog") }

    // Ensures the overlay surface is actively composited by WindowManagerService when waking from AOD/Doze
    private val aodRefreshTask = Runnable {
        if (!isShowingAnim) return@Runnable
        ensureOverlayAttached()
        val v = view ?: return@Runnable
        val lp = viewLp ?: return@Runnable
        lp.flags = buildOverlayFlags(activePlayback = true)
        v.keepScreenOn = true
        v.alpha = 1f
        if (v.visibility != View.VISIBLE) {
            v.visibility = View.VISIBLE
        }
        v.playImmediate()
        runCatching { wm.updateViewLayout(v, lp) }
        v.invalidate()
    }

    // Tracks Display state transitions (STATE_DOZE, STATE_DOZE_SUSPEND, STATE_ON, STATE_OFF)
    // so the overlay renders reliably over the Always-On Display (AOD) state.
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            val display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return
            val state = display.state
            val nowDoze = state == Display.STATE_DOZE || state == Display.STATE_DOZE_SUSPEND
            val nowOff = state == Display.STATE_OFF
            val nowOn = state == Display.STATE_ON || state == Display.STATE_VR

            isInAodOrDoze = nowDoze
            ShizukuBridge.setScreenInteractive(nowOn && pw.isInteractive)

            when {
                nowDoze -> {
                    // Always-On Display (AOD) active: ensure overlay remains attached with
                    // FLAG_SHOW_WHEN_LOCKED | FLAG_ALLOW_LOCK_WHILE_SCREEN_ON and primed at frame 0.
                    ShizukuBridge.tryBind()
                    ui.postAtFrontOfQueue {
                        ensureOverlayAttached()
                        if (fingerDown && !suppressUntilFingerUp) {
                            if (!isShowingAnim) {
                                showImmediate(onLock = true)
                            } else {
                                aodRefreshTask.run()
                            }
                        } else if (!isShowingAnim) {
                            view?.primeForImmediateStart()
                        }
                    }
                }
                nowOn -> {
                    // Display stepped from AOD/Doze or Off to STATE_ON
                    if (isShowingAnim && fingerDown) {
                        ui.postAtFrontOfQueue(aodRefreshTask)
                    } else {
                        ui.post {
                            ensureOverlayAttached()
                            view?.primeForImmediateStart()
                        }
                    }
                }
                nowOff -> {
                    if (!fingerDown && isShowingAnim) {
                        ui.postAtFrontOfQueue { hide("display state off") }
                    }
                }
            }
        }
    }

    // Background task that checks Accessibility windows off the UI thread so the main thread
    // NEVER blocks or hangs on Binder IPC calls, while pre-caching `promptVisible = true`
    // and priming the pre-warmed animation at frame 0 as soon as a BiometricPrompt appears.
    private val bgCheckPromptTask = Runnable {
        val onLockOrAod = isLockOrAodState()
        val wasVisible = promptVisible
        val now = if (onLockOrAod) false else promptShowing()
        promptVisible = now

        if (now && !wasVisible && !isShowingAnim) {
            // BiometricPrompt just appeared: prime the pre-warmed LottieDrawable or Video at frame 0 before touch
            ui.post {
                if (!isShowingAnim) {
                    ensureOverlayAttached()
                    view?.primeForImmediateStart()
                }
            }
        }

        if (now && fingerDown && !isShowingAnim && !suppressUntilFingerUp) {
            ui.postAtFrontOfQueue {
                if (fingerDown && !isShowingAnim && !suppressUntilFingerUp) {
                    showImmediate(onLock = false)
                }
            }
        } else if (!now && isShowingAnim && !shownOnLock) {
            ui.postAtFrontOfQueue {
                if (isShowingAnim && !shownOnLock) {
                    hide("biometric verification completed")
                }
            }
        }
    }

    private val bgFollowUpPromptCheck1 = Runnable { bgCheckPromptTask.run() }
    private val bgFollowUpPromptCheck2 = Runnable { bgCheckPromptTask.run() }

    // Event-driven check for SystemUI error nodes (only runs when SystemUI emits an event after >=240ms of touch)
    private val bgCheckErrorEventTask = Runnable {
        if (!fingerDown || !isShowingAnim) return@Runnable
        val elapsed = SystemClock.elapsedRealtime() - touchDownTimeMs
        if (elapsed < 240L) return@Runnable
        if (hasActiveBiometricErrorNode()) {
            suppressUntilFingerUp = true
            ui.postAtFrontOfQueue { hide("fingerprint not recognized") }
        }
    }

    // Ultra-fast 20ms background retry when the user touches the sensor at the exact moment a BiometricPrompt is opening
    private val bgRetryShowTask = object : Runnable {
        override fun run() {
            if (!fingerDown || isShowingAnim || suppressUntilFingerUp) return
            val onLockOrAod = isLockOrAodState()
            val nowPrompt = if (onLockOrAod) false else promptShowing()
            promptVisible = nowPrompt
            if (onLockOrAod || nowPrompt) {
                ui.postAtFrontOfQueue {
                    if (fingerDown && !isShowingAnim && !suppressUntilFingerUp) {
                        showImmediate(onLock = onLockOrAod)
                    }
                }
                return
            }
            if (retryCount < 10 && fingerDown && !suppressUntilFingerUp) {
                retryCount++
                bgHandler?.postDelayed(this, 20L)
            }
        }
    }

    // Lightweight monitor that runs ONLY while the animation is visible:
    // - On Lock Screen / AOD: checks KeyguardManager state in <0.05ms with ZERO Accessibility IPC overhead!
    // - On Unlocked Screen: checks if BiometricPrompt closed so verification completion hides immediately.
    private val bgActiveMonitor = object : Runnable {
        override fun run() {
            if (!isShowingAnim) return

            val currentlyLocked = kg.isKeyguardLocked
            val interactive = pw.isInteractive
            if (shownOnLock) {
                if (currentlyLocked) {
                    wasKeyguardLockedOnTouch = true
                }
                // Hide as soon as the keyguard transitions from locked -> unlocked while interactive
                if (wasKeyguardLockedOnTouch && interactive && !currentlyLocked) {
                    promptVisible = false
                    ui.postAtFrontOfQueue { hide("phone unlocked") }
                    return
                }
                if (isShowingAnim) {
                    bgHandler?.postDelayed(this, 35L)
                }
            } else {
                if (!interactive && !isInAodOrDoze) {
                    ui.postAtFrontOfQueue { hide("screen off") }
                    return
                }
                val stillPrompt = !currentlyLocked && promptShowing()
                promptVisible = stillPrompt
                if (!stillPrompt) {
                    ui.postAtFrontOfQueue { hide("verification completed") }
                    return
                }
                if (isShowingAnim) {
                    bgHandler?.postDelayed(this, 55L)
                }
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isInAodOrDoze = false
                    ShizukuBridge.setScreenInteractive(true)
                    ShizukuBridge.tryBind()
                    if (isShowingAnim && fingerDown) {
                        ui.postAtFrontOfQueue(aodRefreshTask)
                    } else {
                        ui.post {
                            ensureOverlayAttached()
                            view?.primeForImmediateStart()
                        }
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    promptVisible = false
                    bgHandler?.removeCallbacks(bgRetryShowTask)
                    val displayState = dm.getDisplay(Display.DEFAULT_DISPLAY)?.state ?: Display.STATE_OFF
                    isInAodOrDoze = displayState == Display.STATE_DOZE || displayState == Display.STATE_DOZE_SUSPEND
                    ShizukuBridge.setScreenInteractive(false)
                    ShizukuBridge.tryBind()
                    if (!fingerDown) {
                        shownOnLock = false
                        wasKeyguardLockedOnTouch = false
                        suppressUntilFingerUp = false
                        hide("screen off")
                    } else if (isShowingAnim) {
                        // Finger is actively pressing the sensor as AOD/screen-off broadcast arrives
                        ui.postAtFrontOfQueue(aodRefreshTask)
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    isInAodOrDoze = false
                    promptVisible = false
                    shownOnLock = false
                    wasKeyguardLockedOnTouch = false
                    suppressUntilFingerUp = false
                    bgHandler?.removeCallbacks(bgRetryShowTask)
                    ui.postAtFrontOfQueue { hide("user present") }
                }
                Intent.ACTION_DREAMING_STARTED -> {
                    // Android Always-On Display (AOD) runs inside SystemUI's Doze DreamService
                    isInAodOrDoze = true
                    ShizukuBridge.setScreenInteractive(false)
                    ShizukuBridge.tryBind()
                    ui.postAtFrontOfQueue {
                        ensureOverlayAttached()
                        if (fingerDown && !suppressUntilFingerUp) {
                            if (!isShowingAnim) {
                                showImmediate(onLock = true)
                            } else {
                                aodRefreshTask.run()
                            }
                        } else {
                            view?.primeForImmediateStart()
                        }
                    }
                }
                Intent.ACTION_DREAMING_STOPPED -> {
                    isInAodOrDoze = false
                    if (isShowingAnim && fingerDown) {
                        ui.postAtFrontOfQueue(aodRefreshTask)
                    }
                }
            }
        }
    }

    private fun isLockOrAodState(): Boolean {
        if (kg.isKeyguardLocked || !pw.isInteractive || isInAodOrDoze) return true
        val state = runCatching { dm.getDisplay(Display.DEFAULT_DISPLAY)?.state }.getOrNull()
        return state == Display.STATE_DOZE || state == Display.STATE_DOZE_SUSPEND || state == Display.STATE_OFF
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        runCatching {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        kg = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        pw = getSystemService(POWER_SERVICE) as PowerManager
        dm = getSystemService(DISPLAY_SERVICE) as DisplayManager

        val ht = HandlerThread("FPAnim-A11y", Process.THREAD_PRIORITY_DISPLAY).apply { start() }
        bgThread = ht
        bgHandler = Handler(ht.looper)

        // Pre-warm the active Lottie composition & transparent Pro Video frame atlas immediately
        LottiePrewarmCache.prewarmAsync(this)
        ProVideoFrameCache.prewarmAsync(this)

        runCatching {
            dm.registerDisplayListener(displayListener, ui)
        }

        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_DREAMING_STARTED)
                addAction(Intent.ACTION_DREAMING_STOPPED)
            },
            ContextCompat.RECEIVER_EXPORTED
        )

        val currentDisplayState = runCatching { dm.getDisplay(Display.DEFAULT_DISPLAY)?.state }.getOrNull()
        isInAodOrDoze = currentDisplayState == Display.STATE_DOZE || currentDisplayState == Display.STATE_DOZE_SUSPEND
        ShizukuBridge.setScreenInteractive(pw.isInteractive && !isInAodOrDoze)

        ShizukuBridge.onTouch = { down ->
            fingerDown = down
            if (down) {
                touchDownTimeMs = SystemClock.elapsedRealtime()
            } else {
                // Lifting the finger re-arms the animation for the next touch
                suppressUntilFingerUp = false
            }
            // Post at the very front of the Main UI Looper for <1ms response time
            ui.postAtFrontOfQueue {
                if (down) {
                    if (suppressUntilFingerUp || isShowingAnim) return@postAtFrontOfQueue
                    val onLockOrAod = isLockOrAodState()
                    if (onLockOrAod || promptVisible) {
                        // Instant 0ms show on Lock Screen, Always-On Display (AOD), or BiometricPrompt!
                        showImmediate(onLock = onLockOrAod)
                    } else {
                        // Fast background check in case a BiometricPrompt just opened
                        retryCount = 0
                        bgHandler?.removeCallbacks(bgRetryShowTask)
                        bgHandler?.postAtFrontOfQueue(bgRetryShowTask)
                    }
                } else {
                    bgHandler?.removeCallbacks(bgRetryShowTask)
                    hide("finger up")
                }
            }
        }
        ShizukuBridge.tryBind()
        reload()
        Log.d(TAG, "FpAccessibilityService connected")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ui.post {
            if (!isShowingAnim) {
                reload()
            } else {
                // Update coordinates if display metrics changed while keeping active animation running
                val c = Prefs.load(this)
                val b = wm.currentWindowMetrics.bounds
                val d = (c.size * b.width()).toInt().coerceAtLeast(64)
                val v = view
                val lp = viewLp
                if (v != null && lp != null && viewAttached) {
                    lp.width = d
                    lp.height = d
                    lp.x = (c.cx * b.width() - d / 2f).toInt()
                    lp.y = (c.cy * b.height() - d / 2f).toInt()
                    runCatching { wm.updateViewLayout(v, lp) }
                }
            }
        }
    }

    private fun ensureOverlayAttached() {
        val v = view
        val lp = viewLp
        if (v == null || lp == null) {
            reload()
            return
        }
        if (!viewAttached || !v.isAttachedToWindow) {
            runCatching {
                wm.addView(v, lp)
                viewAttached = true
            }.onFailure {
                // If already added under WindowManager, mark attached and update layout
                viewAttached = v.isAttachedToWindow
                runCatching { wm.updateViewLayout(v, lp) }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun reload() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            ui.post { reload() }
            return
        }
        isShowingAnim = false
        val c = Prefs.load(this)
        val b = wm.currentWindowMetrics.bounds
        val d = (c.size * b.width()).toInt().coerceAtLeast(64)

        val targetX = (c.cx * b.width() - d / 2f).toInt()
        val targetY = (c.cy * b.height() - d / 2f).toInt()

        val existingView = view
        val existingLp = viewLp

        if (existingView != null && existingLp != null && viewAttached && existingView.isAttachedToWindow) {
            existingView.pauseAndResetToFrameZero()
            existingView.keepScreenOn = false
            existingView.alpha = 0f
            existingView.visibility = View.VISIBLE
            existingView.bindActiveAnimation(
                context = this,
                speed = c.speed,
                targetSizePx = d,
                autoPlay = false
            )
            existingLp.width = d
            existingLp.height = d
            existingLp.x = targetX
            existingLp.y = targetY
            existingLp.flags = buildOverlayFlags(activePlayback = false)
            runCatching { wm.updateViewLayout(existingView, existingLp) }
        } else {
            existingView?.let {
                runCatching { wm.removeView(it) }
                viewAttached = false
            }
            val v = OverlayAnimHostView(this).apply {
                bindActiveAnimation(
                    context = this@FpAccessibilityService,
                    speed = c.speed,
                    targetSizePx = d,
                    autoPlay = false
                )
                alpha = 0f
                visibility = View.VISIBLE
                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(attachedView: View) {
                        viewAttached = true
                    }

                    override fun onViewDetachedFromWindow(detachedView: View) {
                        viewAttached = false
                        // If WindowManager detaches the overlay during an AOD/Doze transition while
                        // the service is still alive, re-attach it on the next UI frame.
                        if (instance === this@FpAccessibilityService) {
                            ui.post {
                                if (instance === this@FpAccessibilityService && !viewAttached) {
                                    ensureOverlayAttached()
                                }
                            }
                        }
                    }
                })
            }
            val lp = buildOverlayLayoutParams(
                widthPx = d,
                heightPx = d,
                xPx = targetX,
                yPx = targetY,
                activePlayback = false
            )
            runCatching {
                wm.addView(v, lp)
                viewAttached = true
            }
            view = v
            viewLp = lp
        }

        ShizukuBridge.applyZone()
        ShizukuBridge.setScreenInteractive(pw.isInteractive && !isInAodOrDoze)
    }

    @Suppress("DEPRECATION")
    private fun wakeDisplayForAodIfNeeded(): Boolean {
        val displayState = runCatching { dm.getDisplay(Display.DEFAULT_DISPLAY)?.state }.getOrNull()
        val inDoze = isInAodOrDoze ||
            displayState == Display.STATE_DOZE ||
            displayState == Display.STATE_DOZE_SUSPEND ||
            displayState == Display.STATE_OFF
        if (!pw.isInteractive || inDoze) {
            runCatching {
                val wl = aodWakeLock ?: pw.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                    "fpanim:aod_wake"
                ).apply {
                    setReferenceCounted(false)
                    aodWakeLock = this
                }
                wl.acquire(6000L)
            }
            return true
        }
        return false
    }

    private fun releaseAodWakeLock() {
        runCatching {
            aodWakeLock?.let {
                if (it.isHeld) it.release()
            }
        }
    }

    private fun showImmediate(onLock: Boolean) {
        if (isShowingAnim || suppressUntilFingerUp) return
        ensureOverlayAttached()
        val wasAod = isLockOrAodState() && (!pw.isInteractive || isInAodOrDoze)
        shownOnLock = onLock
        wasKeyguardLockedOnTouch = kg.isKeyguardLocked || wasAod
        isShowingAnim = true

        // If finger touches sensor on Always-On Display (AOD / Doze), step display out of
        // Doze suspend so Choreographer renders the animation at full 60/120fps.
        wakeDisplayForAodIfNeeded()

        val v = view
        val lp = viewLp
        if (v != null && lp != null) {
            lp.flags = buildOverlayFlags(activePlayback = true)
            v.keepScreenOn = true
            if (v.visibility != View.VISIBLE) {
                v.visibility = View.VISIBLE
            }
            v.alpha = 1f
            v.playImmediate()
            if (wasAod || onLock) {
                runCatching { wm.updateViewLayout(v, lp) }
            }
        }

        if (wasAod) {
            ui.removeCallbacks(aodRefreshTask)
            ui.postDelayed(aodRefreshTask, 16L)
            ui.postDelayed(aodRefreshTask, 48L)
            ui.postDelayed(aodRefreshTask, 110L)
        }

        ui.removeCallbacks(singleScanTimeout)
        ui.postDelayed(singleScanTimeout, 1250L)
        ui.removeCallbacks(watchdog)
        ui.postDelayed(watchdog, 6000L)
        bgHandler?.removeCallbacks(bgActiveMonitor)
        bgHandler?.postDelayed(bgActiveMonitor, if (onLock) 35L else 55L)
    }

    private fun hide(why: String) {
        ui.removeCallbacks(singleScanTimeout)
        ui.removeCallbacks(watchdog)
        ui.removeCallbacks(aodRefreshTask)
        bgHandler?.removeCallbacks(bgActiveMonitor)
        bgHandler?.removeCallbacks(bgCheckErrorEventTask)
        releaseAodWakeLock()
        if (!isShowingAnim && (view?.alpha ?: 0f) == 0f) return
        isShowingAnim = false

        val v = view
        val lp = viewLp
        if (v != null) {
            v.keepScreenOn = false
            v.alpha = 0f
            v.pauseAndResetToFrameZero()
            if (lp != null && viewAttached) {
                val idleFlags = buildOverlayFlags(activePlayback = false)
                if (lp.flags != idleFlags) {
                    lp.flags = idleFlags
                    runCatching { wm.updateViewLayout(v, lp) }
                }
            }
        }
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null) return

        val isSysUi = e.packageName?.toString() == "com.android.systemui"

        // If SystemUI emits window state changes during AOD/Doze while finger is down, ensure overlay stays visible
        if (isSysUi && fingerDown && !suppressUntilFingerUp && isLockOrAodState()) {
            if (!isShowingAnim) {
                ui.postAtFrontOfQueue {
                    if (fingerDown && !isShowingAnim && !suppressUntilFingerUp) {
                        showImmediate(onLock = true)
                    }
                }
            }
        }

        // Instant zero-IPC check for biometric failure text announced/updated by SystemUI while finger is down
        if (fingerDown && isShowingAnim && isSysUi) {
            val elapsed = SystemClock.elapsedRealtime() - touchDownTimeMs
            if (elapsed >= 220L) {
                val cd = e.contentDescription?.toString() ?: ""
                var failed = isBiometricFailureText(cd)
                if (!failed) {
                    for (t in e.text) {
                        if (isBiometricFailureText(t?.toString() ?: "")) {
                            failed = true
                            break
                        }
                    }
                }
                if (failed) {
                    suppressUntilFingerUp = true
                    ui.postAtFrontOfQueue { hide("fingerprint not recognized") }
                    return
                }
                // Also schedule a single background check of the error view node for this event
                bgHandler?.let { h ->
                    h.removeCallbacks(bgCheckErrorEventTask)
                    h.post(bgCheckErrorEventTask)
                }
            }
        }

        // Only run BiometricPrompt window checks when not on the lock screen (or when unlocked prompt is active)
        if (!shownOnLock || !kg.isKeyguardLocked) {
            bgHandler?.let { h ->
                h.removeCallbacks(bgCheckPromptTask)
                h.removeCallbacks(bgFollowUpPromptCheck1)
                h.removeCallbacks(bgFollowUpPromptCheck2)
                h.post(bgCheckPromptTask)
                h.postDelayed(bgFollowUpPromptCheck1, 70L)
                h.postDelayed(bgFollowUpPromptCheck2, 160L)
            }
        }
    }

    private fun isBiometricFailureText(raw: String): Boolean {
        if (raw.isBlank()) return false
        val s = raw.lowercase()
        return s.contains("not recognized") ||
            s.contains("try again") ||
            s.contains("didn't match") ||
            s.contains("doesn't match") ||
            s.contains("mismatch") ||
            s.contains("unrecognized") ||
            s.contains("partial") ||
            s.contains("too fast") ||
            s.contains("too slow") ||
            s.contains("move finger") ||
            s.contains("press firmer") ||
            s.contains("too many attempts") ||
            s.contains("couldn't verify") ||
            s.contains("cannot recognize") ||
            s.contains("unable to process")
    }

    private fun hasActiveBiometricErrorNode(): Boolean {
        val windowList = try {
            windows
        } catch (_: Exception) {
            null
        } ?: return false

        for (w in windowList) {
            val r = w.root ?: continue
            if (r.packageName?.toString() != "com.android.systemui") continue

            val tryAgainNodes = try {
                r.findAccessibilityNodeInfosByViewId("com.android.systemui:id/button_try_again")
            } catch (_: Exception) {
                null
            }
            if (tryAgainNodes != null) {
                for (n in tryAgainNodes) {
                    if (n.isVisibleToUser) return true
                }
            }

            val errNodes = try {
                r.findAccessibilityNodeInfosByViewId("com.android.systemui:id/error")
            } catch (_: Exception) {
                null
            }
            if (errNodes != null) {
                for (n in errNodes) {
                    if (n.isVisibleToUser) {
                        val txt = (n.text?.toString() ?: "").trim()
                        if (isBiometricFailureText(txt)) return true
                    }
                }
            }
        }
        return false
    }

    private fun promptShowing(): Boolean {
        val windowList = try {
            windows
        } catch (_: Exception) {
            null
        } ?: return false

        for (w in windowList) {
            val r = w.root ?: continue
            if (r.packageName?.toString() != "com.android.systemui") continue
            for (id in PROMPT_IDS) {
                val nodes = try {
                    r.findAccessibilityNodeInfosByViewId("com.android.systemui:id/$id")
                } catch (_: Exception) {
                    null
                } ?: continue
                for (node in nodes) {
                    if (node.isVisibleToUser) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onInterrupt() {
        hide("accessibility service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanupServiceResources()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanupServiceResources()
        super.onDestroy()
    }

    private fun cleanupServiceResources() {
        instance = null
        ShizukuBridge.onTouch = null
        ui.removeCallbacks(singleScanTimeout)
        ui.removeCallbacks(watchdog)
        ui.removeCallbacks(aodRefreshTask)
        releaseAodWakeLock()
        runCatching {
            if (::dm.isInitialized) {
                dm.unregisterDisplayListener(displayListener)
            }
        }
        bgHandler?.removeCallbacksAndMessages(null)
        bgThread?.quitSafely()
        bgThread = null
        bgHandler = null
        val currentView = view
        view = null
        viewLp = null
        viewAttached = false
        currentView?.videoView?.releaseAll()
        currentView?.let {
            if (::wm.isInitialized) {
                runCatching { wm.removeView(it) }
            }
        }
        runCatching { unregisterReceiver(screenReceiver) }
    }

    companion object {
        var instance: FpAccessibilityService? = null
        const val TAG = "FPAnim"
        val PROMPT_IDS = listOf(
            "biometric_icon",
            "biometric_icon_frame",
            "button_negative",
            "button_use_credential",
            "biometric_scrollview"
        )

        @Suppress("DEPRECATION")
        fun buildOverlayFlags(activePlayback: Boolean = false): Int {
            var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            if (activePlayback) {
                flags = flags or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            }
            return flags
        }

        fun buildOverlayLayoutParams(
            widthPx: Int,
            heightPx: Int,
            xPx: Int,
            yPx: Int,
            activePlayback: Boolean = false
        ): WindowManager.LayoutParams {
            return WindowManager.LayoutParams(
                widthPx,
                heightPx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                buildOverlayFlags(activePlayback = activePlayback),
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = xPx
                y = yPx
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }
}

package com.example

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import kotlin.math.roundToInt

class FpAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = BiometricSignatures.LOG_TAG

        @Volatile
        var instance: FpAccessibilityService? = null
            private set

        /**
         * Stores the human-readable reason for the most recent Show / Hide / Blocked decision.
         * Also logged to Logcat under tag `FPAnim`.
         */
        @Volatile
        var lastTriggerSource: String = "Idle"

        @Suppress("DEPRECATION")
        fun buildOverlayFlags(activePlayback: Boolean): Int {
            var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
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
                xPx,
                yPx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                buildOverlayFlags(activePlayback),
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
    }

    private lateinit var wm: WindowManager
    private lateinit var km: KeyguardManager
    private lateinit var pm: PowerManager
    private var dm: DisplayManager? = null
    private lateinit var prefs: SharedPreferences
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayContainer: FrameLayout? = null
    private var animHostView: OverlayAnimHostView? = null
    private var overlayLp: WindowManager.LayoutParams? = null
    private var isOverlayAttached = false
    private var isShowing = false
    private var activeWakeLock: PowerManager.WakeLock? = null

    // Pre-cached calibration speed so showImmediate() does zero SharedPreferences or WindowMetrics reads on touch-down
    @Volatile
    private var cachedSpeed: Float = 1.0f

    /**
     * Sole owner of `isBiometricPromptActive`, `fingerDown`, and `latchedTerminal`.
     */
    val tracker = BiometricRequestTracker()

    /**
     * Dispatches critical touch/biometric callbacks to the very front of the main Looper queue
     * with `isAsynchronous = true` so VSYNC sync barriers and normal UI messages never delay it.
     */
    private inline fun postImmediateMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
            return
        }
        val msg = Message.obtain(mainHandler) { block() }
        msg.isAsynchronous = true
        mainHandler.sendMessageAtFrontOfQueue(msg)
    }

    /**
     * Hard maximum duration fail-safe (5,000 ms):
     * Unconditionally hides the overlay even if the finger is still down, and latches until finger lifts.
     */
    private val hardMaxDurationFailSafe = Runnable {
        if (isShowing || tracker.fingerDown) {
            tracker.onHardMaxDurationReached()
            recordDecision(tracker.lastDecisionReason)
            hideImmediate(reason = tracker.lastDecisionReason)
        }
    }

    /**
     * Stale-state watchdog: if `isBiometricPromptActive` was set true, expires after `STALE_PROMPT_WATCHDOG_MS`.
     */
    private val stalePromptWatchdogRunnable = Runnable {
        if (tracker.isBiometricPromptActive && !tracker.fingerDown) {
            tracker.onPromptStopped("stale_watchdog_timeout")
            recordDecision("Hide: biometric prompt stale watchdog expired")
            syncOverlayWithTracker()
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "debug_diag_enabled") {
            DiagnosticLogger.enabled = Prefs.isDebugDiagEnabled(this)
            DiagnosticLogger.log("DIAG", "DEBUG Diagnostic Mode toggled=${DiagnosticLogger.enabled}", tracker)
            return@OnSharedPreferenceChangeListener
        }
        mainHandler.post {
            reload()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                    tracker.onDeviceUnlocked()
                    recordDecision(tracker.lastDecisionReason)
                    syncOverlayWithTracker()
                    ShizukuBridge.setScreenInteractive(true)
                }
                Intent.ACTION_SCREEN_ON -> {
                    tracker.syncLockState(isLockOrAodState())
                    DiagnosticLogger.log(
                        "BROADCAST",
                        "ACTION_SCREEN_ON (keyguard=${km.isKeyguardLocked}, interactive=${pm.isInteractive})",
                        tracker
                    )
                    syncOverlayWithTracker()
                    ensurePersistentOverlayAttached()
                    ShizukuBridge.setScreenInteractive(true)
                    ShizukuBridge.tryBind()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                    tracker.onScreenOffOrReset("screen off (ACTION_SCREEN_OFF)")
                    recordDecision(tracker.lastDecisionReason)
                    syncOverlayWithTracker()
                    ShizukuBridge.setScreenInteractive(false)
                }
            }
        }
    }

    private fun isLockOrAodState(): Boolean {
        return km.isKeyguardLocked || !pm.isInteractive
    }

    private fun recordDecision(reason: String) {
        lastTriggerSource = reason
        Log.d(TAG, "$reason | $tracker")
        if (DiagnosticLogger.enabled) {
            DiagnosticLogger.log("DECISION", reason, tracker)
        }
    }

    private fun syncOverlayWithTracker() {
        if (tracker.visible) {
            showImmediate(onLock = tracker.onLockOrAod, reason = tracker.lastDecisionReason)
        } else {
            hideImmediate(reason = tracker.lastDecisionReason)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        wm = getSystemService(WindowManager::class.java)
        km = getSystemService(KeyguardManager::class.java)
        pm = getSystemService(PowerManager::class.java)
        dm = getSystemService(DisplayManager::class.java)

        DiagnosticLogger.enabled = Prefs.isDebugDiagEnabled(this)

        prefs = getSharedPreferences("fp", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        ProVideoFrameCache.pinActiveOverlayAtlas(this, Prefs.getSelectedVideoAsset(this))
        LottiePrewarmCache.prewarmAsync(this)
        ensurePersistentOverlayAttached()

        tracker.syncLockState(isLockOrAodState())
        recordDecision("Service connected (keyguard=${km.isKeyguardLocked}, interactive=${pm.isInteractive})")

        setupShizukuCallbacks()
        ShizukuBridge.init(this)
        ShizukuBridge.tryBind()
    }

    private fun setupShizukuCallbacks() {
        // 1. Raw kernel touch inside calibrated inner sensor circle — front-of-queue async dispatch (<0.2ms)
        ShizukuBridge.onTouch = { down ->
            postImmediateMain {
                val onLock = isLockOrAodState()
                if (down) {
                    val shouldShow = tracker.onFingerDown(onLock)
                    if (shouldShow) {
                        // Show BEFORE logging so the GPU frame is scheduled with zero delay
                        showImmediate(onLock = onLock, reason = tracker.lastDecisionReason)
                    } else {
                        hideImmediate(reason = tracker.lastDecisionReason)
                    }
                    recordDecision(tracker.lastDecisionReason)
                } else {
                    val wasShowing = isShowing
                    tracker.onFingerUp()
                    hideImmediate(reason = "Hide: finger lifted")
                    if (wasShowing) {
                        recordDecision("Hide: finger lifted")
                    } else if (DiagnosticLogger.enabled) {
                        DiagnosticLogger.log("TOUCH", "Finger lifted (overlay was already hidden)", tracker)
                    }
                }
            }
        }

        // 2. Real-time mismatch / error signal from TouchUserService logcat monitor
        ShizukuBridge.onFingerprintError = {
            postImmediateMain {
                val accepted = tracker.onMismatchError(source = "shizuku_logcat")
                if (accepted) {
                    syncOverlayWithTracker()
                }
                recordDecision(tracker.lastDecisionReason)
            }
        }

        // 3. Real-time biometric prompt start/stop signal from TouchUserService logcat monitor
        ShizukuBridge.onBiometricPromptStateChanged = { active ->
            postImmediateMain {
                val onLock = isLockOrAodState()
                if (active) {
                    mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                    tracker.onPromptStarted(
                        source = "shizuku_logcat",
                        windowId = -1,
                        isLockOrAodNow = onLock
                    )
                    syncOverlayWithTracker()
                    if (!onLock) {
                        mainHandler.postDelayed(
                            stalePromptWatchdogRunnable,
                            BiometricSignatures.STALE_PROMPT_WATCHDOG_MS
                        )
                    }
                    if (tracker.visible) {
                        recordDecision(tracker.lastDecisionReason)
                    } else if (DiagnosticLogger.enabled) {
                        DiagnosticLogger.log("SHIZUKU_BIO", "Prompt start signal (onLock=$onLock)", tracker)
                    }
                } else {
                    mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                    val wasShowing = isShowing
                    tracker.onPromptStopped(reason = "shizuku_logcat_stop")
                    syncOverlayWithTracker()
                    if (wasShowing || tracker.fingerDown) {
                        recordDecision(tracker.lastDecisionReason)
                    } else if (DiagnosticLogger.enabled) {
                        DiagnosticLogger.log("SHIZUKU_BIO", "Prompt stop signal", tracker)
                    }
                }
            }
        }
    }

    /**
     * Fast window-ID check without calling `win.root` (avoids cross-process ViewRootImpl IPC).
     */
    private fun isTrackedBiometricWindowStillOnScreen(trackedId: Int): Boolean {
        if (trackedId <= 0) return true
        return try {
            val winList = windows
            if (winList.isNullOrEmpty()) true else winList.any { it.id == trackedId }
        } catch (_: Throwable) {
            true
        }
    }

    fun reload() {
        mainHandler.post {
            syncPrewarmedOverlayPositionAndAsset()
            ShizukuBridge.applyZone()
        }
    }

    fun triggerTestPreview(durationMs: Long = 2400L) {
        mainHandler.post {
            ensurePersistentOverlayAttached()
            val view = overlayContainer ?: return@post
            val anim = animHostView ?: return@post
            val lp = overlayLp ?: return@post

            val calib = Prefs.load(this)
            cachedSpeed = calib.speed
            val bounds = Prefs.getRealDisplayBounds(this)
            val w = bounds.width().coerceAtLeast(1080)
            val h = bounds.height().coerceAtLeast(2400)
            val sizePx = (calib.size * w).roundToInt().coerceAtLeast(120)

            lp.width = sizePx
            lp.height = sizePx
            lp.x = (calib.cx * w - sizePx / 2f).roundToInt()
            lp.y = (calib.cy * h - sizePx / 2f).roundToInt()

            anim.bindActiveAnimation(this, calib.speed, sizePx, autoPlay = false)
            view.visibility = View.VISIBLE
            view.alpha = 1f
            anim.playImmediate()
            try {
                wm.updateViewLayout(view, lp)
            } catch (_: Throwable) {}

            recordDecision("Show: manual preview test")
            mainHandler.postDelayed({
                if (!tracker.visible) {
                    hideImmediate(reason = "Hide: manual preview test ended")
                }
            }, durationMs)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val pkgName = ev.packageName?.toString() ?: ""

        // Fast-path 1: Immediately ignore all events from our own app so scrolling/sliders in FP Animator have zero overhead
        if (pkgName.isEmpty() || pkgName == packageName) return

        val eventType = ev.eventType
        val isBioPkg = BiometricSignatures.isBiometricPackage(pkgName)
        val diagEnabled = DiagnosticLogger.enabled

        // Fast-path 2: If not a biometric/SystemUI package, only proceed if we need a window-close check or diagnostic log
        if (!isBioPkg) {
            if (!diagEnabled &&
                !(tracker.isBiometricPromptActive && eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED)
            ) {
                return
            }
        }

        val clsName = ev.className?.toString() ?: ""
        val onLock = isLockOrAodState()
        tracker.syncLockState(onLock)

        // Fast O(1) parcel read: read ev.text and ev.contentDescription directly from the event parcel (0 IPC!)
        var sourceViewId: String? = null
        val sb = StringBuilder()
        if (isBioPkg) {
            for (t in ev.text) {
                if (!t.isNullOrEmpty()) {
                    if (sb.isNotEmpty()) sb.append(" | ")
                    sb.append(t)
                }
            }
            if (!ev.contentDescription.isNullOrEmpty()) {
                if (sb.isNotEmpty()) sb.append(" | ")
                sb.append(ev.contentDescription)
            }

            // Only query ev.source on TYPE_WINDOW_STATE_CHANGED (when a dialog opens), never on continuous content events
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                try {
                    val src = ev.source
                    if (src != null) {
                        sourceViewId = src.viewIdResourceName
                        if (sb.isEmpty() && !src.text.isNullOrEmpty()) {
                            sb.append(src.text)
                        }
                        @Suppress("DEPRECATION")
                        src.recycle()
                    }
                } catch (_: Throwable) {}
            }
        }

        val combinedText = sb.toString()

        if (diagEnabled && (isBioPkg ||
                eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED)
        ) {
            val typeLabel = when (eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WIN_STATE"
                AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHG"
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WIN_CONTENT"
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "NOTIF"
                AccessibilityEvent.TYPE_ANNOUNCEMENT -> "ANNOUNCE"
                else -> "EV_$eventType"
            }
            DiagnosticLogger.log(
                "A11Y",
                "type=$typeLabel pkg=$pkgName cls=$clsName winId=${ev.windowId} viewId=${sourceViewId ?: "-"} " +
                    "text=\"${combinedText.take(120)}\" keyguard=${km.isKeyguardLocked} interactive=${pm.isInteractive}",
                tracker
            )
        }

        if (isBioPkg) {
            // 1. Check lockout / PIN-only / after-reboot text
            if (combinedText.isNotEmpty() && BiometricSignatures.matchesLockoutOrPinOnlyText(combinedText)) {
                tracker.onLockoutOrPinOnly(source = "$pkgName:${combinedText.take(40)}")
                syncOverlayWithTracker()
                recordDecision(tracker.lastDecisionReason)
                return
            }

            // 2. Check mismatch / "not recognized" / "try again" error text (only latches if fingerDown or <=1.5s ago)
            if (combinedText.isNotEmpty() && BiometricSignatures.matchesMismatchText(combinedText)) {
                val accepted = tracker.onMismatchError(source = "a11y:${combinedText.take(40)}")
                if (accepted) {
                    syncOverlayWithTracker()
                }
                recordDecision(tracker.lastDecisionReason)
                return
            }

            // 3. Check prompt cancel / dismissal text
            if (combinedText.isNotEmpty() && BiometricSignatures.matchesPromptDismissText(combinedText)) {
                mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                tracker.onPromptStopped(reason = "a11y_dismiss:${combinedText.take(40)}")
                syncOverlayWithTracker()
                recordDecision(tracker.lastDecisionReason)
                return
            }

            // 4. Check BiometricPrompt / UDFPS / AppLock start (Priority: Class -> ViewId -> Text)
            val matchedByClass = BiometricSignatures.matchesPromptClass(clsName)
            val matchedByViewId = BiometricSignatures.matchesPromptViewId(sourceViewId)
            val matchedByText = combinedText.isNotEmpty() && BiometricSignatures.matchesPromptText(combinedText)

            if (matchedByClass || matchedByViewId || matchedByText) {
                if (onLock && matchedByText) {
                    tracker.clearLockScreenRestriction()
                }
                if (!onLock) {
                    mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                    val srcDesc = when {
                        matchedByClass -> "class:$clsName"
                        matchedByViewId -> "viewId:$sourceViewId"
                        else -> "text:${combinedText.take(30)}"
                    }
                    tracker.onPromptStarted(
                        source = srcDesc,
                        windowId = ev.windowId,
                        isLockOrAodNow = false
                    )
                    syncOverlayWithTracker()
                    mainHandler.postDelayed(
                        stalePromptWatchdogRunnable,
                        BiometricSignatures.STALE_PROMPT_WATCHDOG_MS
                    )
                    if (tracker.visible) {
                        recordDecision(tracker.lastDecisionReason)
                    } else if (diagEnabled) {
                        DiagnosticLogger.log("PROMPT_START", "Unlocked prompt active ($srcDesc)", tracker)
                    }
                }
                return
            }
        }

        // 5. Stale-state window watchdog on TYPE_WINDOWS_CHANGED:
        //    Fast windowId check with zero cross-process root node IPC.
        if (!onLock && tracker.isBiometricPromptActive &&
            eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            tracker.activePromptWindowId > 0
        ) {
            if (!isTrackedBiometricWindowStillOnScreen(tracker.activePromptWindowId)) {
                mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                tracker.onPromptStopped(reason = "prompt_window_closed")
                syncOverlayWithTracker()
                recordDecision(tracker.lastDecisionReason)
            }
        }
    }

    private fun ensurePersistentOverlayAttached() {
        if (isOverlayAttached && overlayContainer != null && animHostView != null) {
            return
        }

        val calib = Prefs.load(this)
        cachedSpeed = calib.speed
        val bounds = Prefs.getRealDisplayBounds(this)
        val w = bounds.width().coerceAtLeast(1080)
        val h = bounds.height().coerceAtLeast(2400)
        val sizePx = (calib.size * w).roundToInt().coerceAtLeast(120)

        // Keep container View.INVISIBLE with LAYER_TYPE_NONE when idle so SurfaceFlinger completely
        // skips compositing this overlay layer during normal phone use (0% GPU/HWC overhead).
        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            visibility = View.INVISIBLE
            alpha = 0f
            setLayerType(View.LAYER_TYPE_NONE, null)
        }

        val hostView = OverlayAnimHostView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        }

        hostView.bindActiveAnimation(this, calib.speed, sizePx, autoPlay = false)
        hostView.primeForImmediateStart()
        container.addView(hostView)

        val lp = buildOverlayLayoutParams(
            widthPx = sizePx,
            heightPx = sizePx,
            xPx = (calib.cx * w - sizePx / 2f).roundToInt(),
            yPx = (calib.cy * h - sizePx / 2f).roundToInt(),
            activePlayback = false
        )

        try {
            wm.addView(container, lp)
            overlayContainer = container
            animHostView = hostView
            overlayLp = lp
            isOverlayAttached = true
        } catch (_: Throwable) {
            isOverlayAttached = false
        }
    }

    private fun syncPrewarmedOverlayPositionAndAsset() {
        ensurePersistentOverlayAttached()
        val container = overlayContainer ?: return
        val hostView = animHostView ?: return
        val lp = overlayLp ?: return

        val calib = Prefs.load(this)
        cachedSpeed = calib.speed
        val bounds = Prefs.getRealDisplayBounds(this)
        val w = bounds.width().coerceAtLeast(1080)
        val h = bounds.height().coerceAtLeast(2400)
        val sizePx = (calib.size * w).roundToInt().coerceAtLeast(120)

        lp.width = sizePx
        lp.height = sizePx
        lp.x = (calib.cx * w - sizePx / 2f).roundToInt()
        lp.y = (calib.cy * h - sizePx / 2f).roundToInt()

        hostView.bindActiveAnimation(this, calib.speed, sizePx, autoPlay = isShowing)
        if (!isShowing) {
            hostView.primeForImmediateStart()
        }

        try {
            wm.updateViewLayout(container, lp)
        } catch (_: Throwable) {}
    }

    @Suppress("DEPRECATION")
    private fun acquireAodWakeLockIfNeeded(onLock: Boolean) {
        if (!onLock && pm.isInteractive) return
        try {
            if (activeWakeLock == null) {
                activeWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                    "FPAnimator:AODOverlayWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (activeWakeLock?.isHeld != true) {
                activeWakeLock?.acquire(BiometricSignatures.HARD_MAX_OVERLAY_DURATION_MS)
            }
        } catch (_: Throwable) {}
    }

    private fun releaseAodWakeLock() {
        try {
            if (activeWakeLock?.isHeld == true) {
                activeWakeLock?.release()
            }
        } catch (_: Throwable) {}
    }

    /**
     * Ultra-fast show path:
     * - No SharedPreferences or WindowMetrics reads on the critical path
     * - No WindowManager.updateViewLayout() IPC
     * - Sets View.VISIBLE + alpha = 1f + pre-primed Choreographer start
     */
    private fun showImmediate(onLock: Boolean = isLockOrAodState(), reason: String = "") {
        ensurePersistentOverlayAttached()
        val view = overlayContainer ?: return
        val anim = animHostView ?: return

        if (!isShowing) {
            isShowing = true
            view.alpha = 1f
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
            }
            anim.setSpeed(cachedSpeed)
            anim.playImmediate()

            acquireAodWakeLockIfNeeded(onLock)

            // Arm the hard 5-second maximum duration fail-safe timer (fires even if finger stays down)
            mainHandler.removeCallbacks(hardMaxDurationFailSafe)
            mainHandler.postDelayed(
                hardMaxDurationFailSafe,
                BiometricSignatures.HARD_MAX_OVERLAY_DURATION_MS
            )

            Log.d(TAG, "OVERLAY_SHOW ($reason)")
        }
    }

    /**
     * Ultra-fast hide path:
     * - Immediately sets View.INVISIBLE + alpha = 0f so SurfaceFlinger removes the layer from composition,
     *   and resets to frame 0 so it is primed for next touch.
     */
    private fun hideImmediate(reason: String = "") {
        mainHandler.removeCallbacks(hardMaxDurationFailSafe)
        if (!isShowing && overlayContainer?.visibility != View.VISIBLE) {
            releaseAodWakeLock()
            return
        }
        isShowing = false
        val view = overlayContainer ?: return
        val anim = animHostView ?: return

        view.alpha = 0f
        anim.pauseAndResetToFrameZero()
        view.visibility = View.INVISIBLE
        releaseAodWakeLock()
        Log.d(TAG, "OVERLAY_HIDE ($reason)")
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
        tracker.onScreenOffOrReset("service interrupted (onInterrupt)")
        recordDecision(tracker.lastDecisionReason)
        hideImmediate(reason = "onInterrupt")
    }

    override fun onDestroy() {
        instance = null
        mainHandler.removeCallbacks(hardMaxDurationFailSafe)
        mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
        tracker.onScreenOffOrReset("service destroyed (onDestroy)")
        try {
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        } catch (_: Throwable) {}
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Throwable) {}
        ShizukuBridge.onTouch = null
        ShizukuBridge.onFingerprintError = null
        ShizukuBridge.onBiometricPromptStateChanged = null
        releaseAodWakeLock()
        overlayContainer?.let {
            try {
                wm.removeViewImmediate(it)
            } catch (_: Throwable) {}
        }
        overlayContainer = null
        animHostView = null
        overlayLp = null
        isOverlayAttached = false
        super.onDestroy()
    }
}

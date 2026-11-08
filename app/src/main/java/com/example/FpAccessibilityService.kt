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

        private const val STANDALONE_OPTICAL_RELEASE_MS = 1_500L
        private const val MIN_HBM_WINDOW_HOLD_MS = 220L

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

        @Suppress("DEPRECATION")
        fun buildSentinelFlags(watchOutsideTouch: Boolean): Int {
            val base = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            return if (watchOutsideTouch) {
                base or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            } else {
                base or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
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

    // 1x1 Standalone zero-interference outside-touch sentinel window (active ONLY when requestActive == true)
    private var touchSentinelView: View? = null
    private var touchSentinelLp: WindowManager.LayoutParams? = null
    private var isSentinelAttached = false
    private var isSentinelWatchingTouch = false
    private var standaloneTouchActive = false
    private var lastWindowCountWhileShowing = -1

    // Pre-cached calibration metrics so showImmediate() & zone checks do zero SharedPreferences/WindowMetrics reads
    @Volatile private var cachedSpeed: Float = 1.0f
    @Volatile private var cachedCxPx: Float = 540f
    @Volatile private var cachedCyPx: Float = 1968f
    @Volatile private var cachedSensorRadiusPx: Float = 68f

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
            standaloneTouchActive = false
            tracker.onHardMaxDurationReached()
            recordDecision(tracker.lastDecisionReason)
            hideImmediate(reason = tracker.lastDecisionReason)
            syncSentinelWatchState()
        }
    }

    /**
     * Standalone optical scan release timer:
     * When running without Shizuku's raw `/dev/input` `BTN_TOUCH 0` signal, releases the touch state
     * cleanly after an optical scan cycle if neither unlock nor mismatch text already ended it.
     */
    private val standaloneHoldReleaseRunnable = Runnable {
        if (standaloneTouchActive && !ShizukuBridge.isConnectedAndActive()) {
            onStandaloneSensorTouchUp("optical_scan_complete")
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
            syncSentinelWatchState()
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
                    mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
                    standaloneTouchActive = false
                    tracker.onDeviceUnlocked()
                    recordDecision(tracker.lastDecisionReason)
                    syncOverlayWithTracker()
                    syncSentinelWatchState()
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
                    ensureTouchSentinelAttached()
                    syncSentinelWatchState()
                    ShizukuBridge.setScreenInteractive(true)
                    ShizukuBridge.tryBind()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                    mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
                    standaloneTouchActive = false
                    tracker.onScreenOffOrReset("screen off (ACTION_SCREEN_OFF)")
                    recordDecision(tracker.lastDecisionReason)
                    syncOverlayWithTracker()
                    syncSentinelWatchState()
                    ShizukuBridge.setScreenInteractive(false)
                }
            }
        }
    }

    private fun isLockOrAodState(): Boolean {
        return km.isKeyguardLocked || !pm.isInteractive
    }

    private fun isPointInsideSensorZone(rawX: Float, rawY: Float): Boolean {
        // Allow 1.30x tolerance around the calibrated inner sensor radius for natural thumb pad contact
        val r = cachedSensorRadiusPx * 1.30f
        val dx = rawX - cachedCxPx
        val dy = rawY - cachedCyPx
        return (dx * dx + dy * dy) <= (r * r)
    }

    private fun onStandaloneSensorTouchDown(source: String) {
        val onLock = isLockOrAodState()
        tracker.syncLockState(onLock)
        if (!tracker.requestActive) {
            syncSentinelWatchState()
            return
        }
        // If a previous standalone touch cycle was still latched or active, reset it cleanly for this fresh press
        if (standaloneTouchActive && tracker.latchedTerminal) {
            tracker.onFingerUp()
        }
        standaloneTouchActive = true
        val shouldShow = tracker.onFingerDown(onLock)
        if (shouldShow) {
            showImmediate(onLock = onLock, reason = "$source -> ${tracker.lastDecisionReason}")
            lastWindowCountWhileShowing = runCatching { windows?.size ?: -1 }.getOrDefault(-1)
            mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
            mainHandler.postDelayed(standaloneHoldReleaseRunnable, STANDALONE_OPTICAL_RELEASE_MS)
        } else {
            hideImmediate(reason = tracker.lastDecisionReason)
        }
        recordDecision(tracker.lastDecisionReason)
    }

    private fun onStandaloneSensorTouchUp(reason: String) {
        mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
        standaloneTouchActive = false
        lastWindowCountWhileShowing = -1
        val wasShowing = isShowing
        tracker.onFingerUp()
        hideImmediate(reason = "Hide: $reason")
        if (wasShowing) {
            recordDecision("Hide: $reason")
        }
        syncSentinelWatchState()
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
        ensureTouchSentinelAttached()

        tracker.syncLockState(isLockOrAodState())
        syncSentinelWatchState()
        recordDecision("Standalone Service Ready (keyguard=${km.isKeyguardLocked}, interactive=${pm.isInteractive})")

        setupShizukuCallbacks()
        ShizukuBridge.init(this)
        ShizukuBridge.tryBind()
    }

    private fun setupShizukuCallbacks() {
        // Optional Shizuku hardware /dev/input acceleration (used automatically if Shizuku is running;
        // when Shizuku is not installed/running, the built-in Standalone Engine handles everything).
        ShizukuBridge.onTouch = { down ->
            postImmediateMain {
                mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
                standaloneTouchActive = false
                val onLock = isLockOrAodState()
                if (down) {
                    val shouldShow = tracker.onFingerDown(onLock)
                    if (shouldShow) {
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
                syncSentinelWatchState()
            }
        }

        ShizukuBridge.onFingerprintError = {
            postImmediateMain {
                mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
                val accepted = tracker.onMismatchError(source = "shizuku_logcat")
                if (accepted) {
                    syncOverlayWithTracker()
                }
                recordDecision(tracker.lastDecisionReason)
            }
        }

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
                    syncSentinelWatchState()
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
                    syncSentinelWatchState()
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
            syncSentinelWatchState()
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

            cachedCxPx = calib.cx * w
            cachedCyPx = calib.cy * h
            cachedSensorRadiusPx = (calib.sensorSize * 0.5f * w).coerceAtLeast(42f)

            lp.width = sizePx
            lp.height = sizePx
            lp.x = (calib.cx * w - sizePx / 2f).roundToInt()
            lp.y = (calib.cy * h - sizePx / 2f).roundToInt()

            anim.bindActiveAnimation(this, calib.speed, sizePx, autoPlay = false)
            view.visibility = View.VISIBLE
            view.alpha = 1f
            anim.alpha = 1f
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
        val eventType = ev.eventType

        // Standalone Finger-Up signal from Accessibility Touch Interaction End
        if (eventType == AccessibilityEvent.TYPE_TOUCH_INTERACTION_END) {
            if (standaloneTouchActive || (tracker.fingerDown && !ShizukuBridge.isConnectedAndActive())) {
                onStandaloneSensorTouchUp("a11y_touch_interaction_end")
            }
            return
        }

        val pkgName = ev.packageName?.toString() ?: ""

        // Fast-path 1: Immediately ignore all events from our own app so scrolling/sliders in FP Animator have zero overhead
        if (pkgName.isEmpty() || pkgName == packageName) return

        val isBioPkg = BiometricSignatures.isBiometricPackage(pkgName)
        val diagEnabled = DiagnosticLogger.enabled

        // Fast-path 2: If not a biometric/SystemUI package, only proceed if we need a window-close check or diagnostic log
        if (!isBioPkg) {
            if (!diagEnabled &&
                !(tracker.isBiometricPromptActive && eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) &&
                !(standaloneTouchActive && isShowing && eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED)
            ) {
                return
            }
        }

        val clsName = ev.className?.toString() ?: ""
        val onLock = isLockOrAodState()
        val wasOnLock = tracker.onLockOrAod
        tracker.syncLockState(onLock)

        // If device just transitioned from lock screen to unlocked, immediately hide overlay and disarm sentinel
        if (wasOnLock && !onLock) {
            mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
            standaloneTouchActive = false
            tracker.onDeviceUnlocked()
            syncOverlayWithTracker()
            syncSentinelWatchState()
            recordDecision(tracker.lastDecisionReason)
            return
        }

        syncSentinelWatchState()

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

            // Query ev.source only on discrete state/click events, never on continuous content events
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
            ) {
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
                AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICK"
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "VIEW_LONG_CLICK"
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
                mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
                standaloneTouchActive = false
                tracker.onLockoutOrPinOnly(source = "$pkgName:${combinedText.take(40)}")
                syncOverlayWithTracker()
                syncSentinelWatchState()
                recordDecision(tracker.lastDecisionReason)
                return
            }

            // 2. Check mismatch / "not recognized" / "try again" error text (only latches if fingerDown or <=1.5s ago)
            if (combinedText.isNotEmpty() && BiometricSignatures.matchesMismatchText(combinedText)) {
                mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
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
                mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
                standaloneTouchActive = false
                tracker.onPromptStopped(reason = "a11y_dismiss:${combinedText.take(40)}")
                syncOverlayWithTracker()
                syncSentinelWatchState()
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
                    syncSentinelWatchState()
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

                // Standalone direct UDFPS view click/long-click trigger when Shizuku is not active
                if (!ShizukuBridge.isConnectedAndActive() &&
                    (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                        eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED ||
                        eventType == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) &&
                    (matchedByClass || matchedByViewId)
                ) {
                    onStandaloneSensorTouchDown("a11y_udfps_view_touch")
                }
                return
            }
        }

        // 5. Window changes check:
        //    a) In standalone mode, if the UDFPS HBM illumination window closes after finger-down, release touch.
        //    b) For unlocked BiometricPrompt, if the tracked prompt window closes, stop the prompt immediately.
        if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (!onLock && tracker.isBiometricPromptActive && tracker.activePromptWindowId > 0) {
                if (!isTrackedBiometricWindowStillOnScreen(tracker.activePromptWindowId)) {
                    mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
                    mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
                    standaloneTouchActive = false
                    tracker.onPromptStopped(reason = "prompt_window_closed")
                    syncOverlayWithTracker()
                    syncSentinelWatchState()
                    recordDecision(tracker.lastDecisionReason)
                    return
                }
            }

            if (standaloneTouchActive && isShowing && !ShizukuBridge.isConnectedAndActive()) {
                val holdElapsed = System.currentTimeMillis() - tracker.lastFingerDownAtMs
                if (holdElapsed >= MIN_HBM_WINDOW_HOLD_MS) {
                    val curCount = runCatching { windows?.size ?: -1 }.getOrDefault(-1)
                    if (curCount > 0 && lastWindowCountWhileShowing > 0 && curCount < lastWindowCountWhileShowing) {
                        onStandaloneSensorTouchUp("udfps_hbm_window_closed")
                    } else if (curCount > lastWindowCountWhileShowing) {
                        lastWindowCountWhileShowing = curCount
                    }
                }
            }
        }
    }

    /**
     * Attaches the 1x1 transparent outside-touch sentinel window used by the Standalone Engine.
     * It is completely untouchable (`FLAG_NOT_TOUCHABLE`) during normal unlocked phone use,
     * and only enables `FLAG_WATCH_OUTSIDE_TOUCH` while `tracker.requestActive == true` (lock screen or active BiometricPrompt).
     */
    @Suppress("ClickableViewAccessibility")
    private fun ensureTouchSentinelAttached() {
        if (isSentinelAttached && touchSentinelView != null && touchSentinelLp != null) {
            return
        }
        val sentinel = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            alpha = 0f
            setOnTouchListener { _, event ->
                if (!ShizukuBridge.isConnectedAndActive()) {
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_OUTSIDE,
                        android.view.MotionEvent.ACTION_DOWN -> {
                            val rx = event.rawX
                            val ry = event.rawY
                            val hasCoords = rx > 1f || ry > 1f
                            if (!hasCoords || isPointInsideSensorZone(rx, ry)) {
                                onStandaloneSensorTouchDown("standalone_sensor_touch")
                            } else if (standaloneTouchActive) {
                                // User tapped outside the sensor circle (e.g. Cancel button or PIN pad)
                                onStandaloneSensorTouchUp("touch_outside_sensor_circle")
                            }
                        }
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            if (standaloneTouchActive) {
                                onStandaloneSensorTouchUp("sentinel_touch_up")
                            }
                        }
                    }
                }
                false
            }
        }

        val lp = WindowManager.LayoutParams(
            1,
            1,
            0,
            0,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            buildSentinelFlags(watchOutsideTouch = false),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            wm.addView(sentinel, lp)
            touchSentinelView = sentinel
            touchSentinelLp = lp
            isSentinelAttached = true
            isSentinelWatchingTouch = false
        } catch (_: Throwable) {
            isSentinelAttached = false
        }
    }

    /**
     * Dynamically enables `FLAG_WATCH_OUTSIDE_TOUCH` on the 1x1 sentinel ONLY when
     * `tracker.requestActive` is true (lock screen or active unlocked BiometricPrompt) AND Shizuku is not active.
     * During normal unlocked phone use, the sentinel has `FLAG_NOT_TOUCHABLE` (0% touch overhead).
     */
    private fun syncSentinelWatchState() {
        ensureTouchSentinelAttached()
        val sentinel = touchSentinelView ?: return
        val lp = touchSentinelLp ?: return
        val shouldWatch = tracker.requestActive && !ShizukuBridge.isConnectedAndActive()
        if (shouldWatch != isSentinelWatchingTouch) {
            isSentinelWatchingTouch = shouldWatch
            lp.flags = buildSentinelFlags(watchOutsideTouch = shouldWatch)
            try {
                wm.updateViewLayout(sentinel, lp)
            } catch (_: Throwable) {}
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

        cachedCxPx = calib.cx * w
        cachedCyPx = calib.cy * h
        cachedSensorRadiusPx = (calib.sensorSize * 0.5f * w).coerceAtLeast(42f)

        // Keep root container View.VISIBLE with alpha=0f when idle so WindowManagerService keeps the
        // SurfaceControl allocated (zero relayoutWindow() Binder IPC on finger-down -> <2ms response),
        // while RenderThread skips drawing/blending when alpha==0f and Choreographer is unscheduled.
        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            visibility = View.VISIBLE
            alpha = 0f
            setLayerType(View.LAYER_TYPE_NONE, null)
        }

        val hostView = OverlayAnimHostView(this).apply {
            alpha = 0f
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

        cachedCxPx = calib.cx * w
        cachedCyPx = calib.cy * h
        cachedSensorRadiusPx = (calib.sensorSize * 0.5f * w).coerceAtLeast(42f)

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
     * Ultra-fast show path (<2ms):
     * - No SharedPreferences or WindowMetrics reads on the critical path
     * - No WindowManager.updateViewLayout() or relayoutWindow() IPC
     * - Flips pre-allocated Surface alpha to 1f + starts pre-primed Choreographer loop immediately
     */
    private fun showImmediate(onLock: Boolean = isLockOrAodState(), reason: String = "") {
        ensurePersistentOverlayAttached()
        val view = overlayContainer ?: return
        val anim = animHostView ?: return

        if (!isShowing) {
            isShowing = true
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
            }
            view.alpha = 1f
            anim.alpha = 1f
            anim.setSpeed(cachedSpeed)
            anim.playImmediate()
            view.postInvalidateOnAnimation()

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
     * - Immediately sets alpha = 0f so RenderThread/SurfaceFlinger skips blending with zero relayout IPC,
     *   unschedules Choreographer callbacks (0% idle CPU/GPU), and resets to frame 0 primed for next touch.
     */
    private fun hideImmediate(reason: String = "") {
        mainHandler.removeCallbacks(hardMaxDurationFailSafe)
        if (!isShowing && (overlayContainer?.alpha ?: 0f) <= 0.001f) {
            releaseAodWakeLock()
            return
        }
        isShowing = false
        val view = overlayContainer ?: return
        val anim = animHostView ?: return

        anim.alpha = 0f
        view.alpha = 0f
        anim.pauseAndResetToFrameZero()
        view.postInvalidateOnAnimation()
        releaseAodWakeLock()
        Log.d(TAG, "OVERLAY_HIDE ($reason)")
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
        mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
        standaloneTouchActive = false
        tracker.onScreenOffOrReset("service interrupted (onInterrupt)")
        recordDecision(tracker.lastDecisionReason)
        hideImmediate(reason = "onInterrupt")
    }

    override fun onDestroy() {
        instance = null
        mainHandler.removeCallbacks(hardMaxDurationFailSafe)
        mainHandler.removeCallbacks(stalePromptWatchdogRunnable)
        mainHandler.removeCallbacks(standaloneHoldReleaseRunnable)
        standaloneTouchActive = false
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
        touchSentinelView?.let {
            try {
                wm.removeViewImmediate(it)
            } catch (_: Throwable) {}
        }
        touchSentinelView = null
        touchSentinelLp = null
        isSentinelAttached = false
        isSentinelWatchingTouch = false
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

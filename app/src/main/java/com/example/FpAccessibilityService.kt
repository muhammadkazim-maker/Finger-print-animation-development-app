package com.example

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.roundToInt

class FpAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FpAccService"
        @Volatile var instance: FpAccessibilityService? = null
        @Volatile var isRunning: Boolean = false
        @Volatile var lastTriggerSource: String = "Idle (waiting for sensor)"

        fun refreshOverlayIfRunning() {
            val inst = instance ?: return
            inst.reload()
        }

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
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                buildOverlayFlags(activePlayback),
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = xPx
                y = yPx
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
    private lateinit var dm: DisplayManager
    private val ui = Handler(Looper.getMainLooper())

    private var bgThread: HandlerThread? = null
    private var bgHandler: Handler? = null

    private var overlayView: OverlayAnimHostView? = null
    private var lp: WindowManager.LayoutParams? = null
    private var attached = false

    @Volatile private var fingerDownOnSensor = false
    @Volatile private var isShowingAnim = false
    @Volatile private var shownOnLockscreen = false
    @Volatile private var shownAtUptimeMs = 0L
    @Volatile private var lastFingerDownAtMs = 0L
    @Volatile private var isBiometricPromptActive = false
    @Volatile private var suppressedByError = false

    private var currentAnimIdLoaded: String? = null
    private var currentVideoAssetLoaded: String? = null
    private var currentSpeedLoaded: Float = -1f
    private var currentSizeFracLoaded: Float = -1f
    private var currentCenterXFracLoaded: Float = -1f
    private var currentCenterYFracLoaded: Float = -1f

    private var lockStateReceiverRegistered = false

    private val lockStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    ShizukuBridge.setScreenInteractive(true)
                    isBiometricPromptActive = false
                    // When the phone gets unlocked it disappears immediately!
                    hide("device unlocked")
                }
                Intent.ACTION_SCREEN_ON -> {
                    ShizukuBridge.setScreenInteractive(true)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    ShizukuBridge.setScreenInteractive(false)
                    isBiometricPromptActive = false
                    hide("screen off")
                }
            }
        }
    }

    private val fingerUpHideTask = Runnable {
        if (!fingerDownOnSensor && isShowingAnim) {
            hide("finger lifted")
        }
    }

    private val safetyMaxDurationHideTask = Runnable {
        if (isShowingAnim && !fingerDownOnSensor) {
            hide("max duration reached")
        }
    }

    // Ultra-lightweight active monitor (only runs while animation is visibly playing)
    private val bgActiveMonitor = object : Runnable {
        override fun run() {
            if (!isShowingAnim) return
            if (!fingerDownOnSensor) {
                hide("touch released")
                return
            }
            bgHandler?.postDelayed(this, 100L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        lastTriggerSource = "Service active"
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        pm = getSystemService(POWER_SERVICE) as PowerManager
        dm = getSystemService(DISPLAY_SERVICE) as DisplayManager

        bgThread = HandlerThread("FpOverlayWorker").apply {
            start()
            bgHandler = Handler(looper)
        }

        val activeVideo = Prefs.getSelectedVideoAsset(this)
        ProVideoFrameCache.pinActiveOverlayAtlas(this, activeVideo)

        buildStandbyOverlay()
        registerScreenAndUnlockReceivers()
        setupShizukuCallbacks()
        ShizukuBridge.init(this)
        Log.i(TAG, "FpAccessibilityService connected (Pro Folder HD zero-lag mode)")
    }

    fun reload() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            rebuildOverlayWindow(forceReloadAnim = true)
        } else {
            ui.post { rebuildOverlayWindow(forceReloadAnim = true) }
        }
    }

    private fun registerScreenAndUnlockReceivers() {
        if (lockStateReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(lockStateReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(lockStateReceiver, filter)
            }
            lockStateReceiverRegistered = true
        } catch (t: Throwable) {
            Log.w(TAG, "registerReceiver failed: ${t.message}")
        }
    }

    private fun buildStandbyOverlay() {
        rebuildOverlayWindow(forceReloadAnim = true)
    }

    fun rebuildOverlayWindow(forceReloadAnim: Boolean = false) {
        if (!this::wm.isInitialized) return
        val cfg = Prefs.load(this)
        val bounds = Prefs.getRealDisplayBounds(this)
        val sw = bounds.width().coerceAtLeast(1)
        val sh = bounds.height().coerceAtLeast(1)

        val sizePx = (sw * cfg.size).roundToInt().coerceIn(80, sw)
        val cx = (sw * cfg.cx).roundToInt()
        val cy = (sh * cfg.cy).roundToInt()

        ShizukuBridge.applyZone()

        val selectedId = Prefs.getSelectedAnimId(this)
        val selectedVideo = Prefs.getSelectedVideoAsset(this)
        ProVideoFrameCache.pinActiveOverlayAtlas(this, selectedVideo)

        val needAnimReload = forceReloadAnim ||
                overlayView == null ||
                selectedId != currentAnimIdLoaded ||
                selectedVideo != currentVideoAssetLoaded ||
                cfg.speed != currentSpeedLoaded ||
                cfg.size != currentSizeFracLoaded

        val hostView = overlayView ?: OverlayAnimHostView(this).also {
            overlayView = it
        }

        if (needAnimReload) {
            hostView.bindActiveAnimation(
                context = this,
                speed = cfg.speed,
                targetSizePx = sizePx,
                autoPlay = isShowingAnim
            )
            currentAnimIdLoaded = selectedId
            currentVideoAssetLoaded = selectedVideo
            currentSpeedLoaded = cfg.speed
            currentSizeFracLoaded = cfg.size
        } else {
            hostView.setSpeed(cfg.speed)
        }

        if (isShowingAnim) {
            hostView.visibility = View.VISIBLE
            hostView.alpha = 1f
            hostView.playImmediate()
        } else {
            hostView.pauseAndResetToFrameZero()
            hostView.alpha = 0f
            // Set INVISIBLE when idle so SurfaceFlinger skips compositing the overlay window completely!
            hostView.visibility = View.INVISIBLE
        }

        val newLp = buildOverlayLayoutParams(
            widthPx = sizePx,
            heightPx = sizePx,
            xPx = cx - sizePx / 2,
            yPx = cy - sizePx / 2,
            activePlayback = isShowingAnim
        )

        try {
            if (!attached) {
                wm.addView(hostView, newLp)
                attached = true
            } else if (
                lp?.width != newLp.width ||
                lp?.height != newLp.height ||
                lp?.x != newLp.x ||
                lp?.y != newLp.y ||
                lp?.flags != newLp.flags
            ) {
                wm.updateViewLayout(hostView, newLp)
            }
            lp = newLp
            currentCenterXFracLoaded = cfg.cx
            currentCenterYFracLoaded = cfg.cy
        } catch (t: Throwable) {
            Log.e(TAG, "Overlay attach/update error: ${t.message}")
            attached = false
        }
    }

    /**
     * Returns true when the device is on the Lockscreen, Keyguard, or Always-On Display (AOD).
     */
    private fun isLockOrAodState(): Boolean {
        val keyguardLocked = try {
            km.isKeyguardLocked || km.isDeviceLocked
        } catch (_: Throwable) {
            false
        }
        if (keyguardLocked) return true

        val interactive = try {
            pm.isInteractive
        } catch (_: Throwable) {
            true
        }
        if (!interactive) return true

        val displayState = try {
            dm.getDisplay(Display.DEFAULT_DISPLAY)?.state ?: Display.STATE_ON
        } catch (_: Throwable) {
            Display.STATE_ON
        }
        return displayState == Display.STATE_DOZE ||
                displayState == Display.STATE_DOZE_SUSPEND ||
                displayState == Display.STATE_OFF
    }

    private fun setupShizukuCallbacks() {
        // Real-time kernel touch callback (strictly constrained to the inner sensor circle)
        ShizukuBridge.onTouch = { down ->
            fingerDownOnSensor = down
            val now = SystemClock.uptimeMillis()
            if (down) {
                lastFingerDownAtMs = now
                ui.removeCallbacks(fingerUpHideTask)
                // 1. Lock screen / AOD: always active for unlock
                // 2. Unlocked phone: active when inside the inner sensor circle (unless currently inside our own FP Animator app UI)
                // 3. Error suppression: do not re-show while finger is still held down after a mismatch error
                val onLock = isLockOrAodState()
                val allowedOnUnlocked = !isInOurOwnApp || isBiometricPromptActive
                if ((onLock || allowedOnUnlocked) && !suppressedByError) {
                    showImmediate(onLock = onLock)
                }
            } else {
                ui.removeCallbacks(fingerUpHideTask)
                // Finger lifted off: reset error suppression so the next touch down triggers again cleanly!
                suppressedByError = false
                hide("finger lifted")
            }
        }

        // Real-time hardware/logcat fingerprint authentication mismatch callback
        ShizukuBridge.onFingerprintError = {
            suppressedByError = true
            ui.post {
                hide("fingerprint mismatch error")
            }
        }

        // Real-time biometric verification session start/stop from system services
        ShizukuBridge.onBiometricPromptStateChanged = { active ->
            isBiometricPromptActive = active
            if (!active && !isLockOrAodState() && isShowingAnim) {
                ui.post { hide("biometric prompt dismissed") }
            }
        }
    }

    @Volatile private var isInOurOwnApp = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val pkgName = ev.packageName?.toString() ?: ""

        if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkgName.isNotEmpty()) {
            // Track whether the user is currently inside our own app UI so touching controls in our app doesn't pop the overlay
            if (!pkgName.contains("systemui", ignoreCase = true) && !pkgName.contains("android", ignoreCase = true)) {
                isInOurOwnApp = (pkgName == packageName)
            }
        }

        if (pkgName == packageName) return

        // 1. Fast zero-IPC text check for biometric error / mismatch messages
        val textList = ev.text
        val eventText = buildString {
            textList?.forEach { append(it).append(" ") }
            ev.contentDescription?.let { append(it) }
        }.lowercase()

        if (isMismatchErrorString(eventText)) {
            suppressedByError = true
            hide("fingerprint mismatch error")
            return
        }

        // 2. Detect unlocked transition when keyguard is dismissed
        if (shownOnLockscreen && !isLockOrAodState()) {
            hide("device unlocked")
            return
        }

        // 3. Detect biometric verification dialog / prompt when unlocked (zero IPC overhead)
        val className = ev.className?.toString() ?: ""
        val isDirectBiometricUi = className.contains("BiometricPrompt", ignoreCase = true) ||
                className.contains("AuthContainer", ignoreCase = true) ||
                className.contains("FingerprintDialog", ignoreCase = true) ||
                className.contains("ConfirmDeviceCredential", ignoreCase = true) ||
                className.contains("BiometricActivity", ignoreCase = true) ||
                className.contains("AppLock", ignoreCase = true) ||
                eventText.contains("touch the fingerprint sensor") ||
                eventText.contains("scan your fingerprint") ||
                eventText.contains("place your finger") ||
                eventText.contains("verify your identity") ||
                eventText.contains("confirm with fingerprint")

        if (isDirectBiometricUi) {
            isBiometricPromptActive = true
        } else if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !isLockOrAodState()) {
            isBiometricPromptActive = false
        }
    }

    private fun isMismatchErrorString(s: String): Boolean {
        if (s.isEmpty()) return false
        return s.contains("not recognized") ||
                s.contains("try again") ||
                s.contains("didn't recognize") ||
                s.contains("couldn't recognize") ||
                s.contains("not match") ||
                s.contains("doesn't match") ||
                s.contains("does not match") ||
                s.contains("mismatch") ||
                s.contains("partial fingerprint") ||
                s.contains("fingerprint error") ||
                s.contains("sensor is dirty") ||
                s.contains("clean the sensor") ||
                s.contains("wrong fingerprint") ||
                s.contains("finger moved") ||
                s.contains("too fast") ||
                s.contains("authentication failed") ||
                s.contains("verification failed") ||
                s.contains("unrecognized")
    }

    /**
     * Ultra-fast `<1ms` path: posts at the very front of the main thread queue and
     * immediately makes the pre-attached hardware-accelerated overlay visible.
     * Works identically on both Lock Screen/AOD and Unlocked Screen!
     */
    fun showImmediate(onLock: Boolean = isLockOrAodState()) {
        val runnable = Runnable {
            ui.removeCallbacks(fingerUpHideTask)
            ui.removeCallbacks(safetyMaxDurationHideTask)

            if (!attached || overlayView == null) {
                rebuildOverlayWindow(forceReloadAnim = true)
            }

            val v = overlayView ?: return@Runnable
            shownOnLockscreen = onLock
            shownAtUptimeMs = SystemClock.uptimeMillis()

            if (!isShowingAnim || v.visibility != View.VISIBLE || v.alpha < 0.99f) {
                isShowingAnim = true
                lastTriggerSource = if (onLock) "Lockscreen / AOD sensor touch" else "Unlocked screen sensor touch"
                v.visibility = View.VISIBLE
                v.alpha = 1f
                v.playImmediate()
                bgHandler?.removeCallbacks(bgActiveMonitor)
                bgHandler?.postDelayed(bgActiveMonitor, 100L)
            }

            ui.postDelayed(safetyMaxDurationHideTask, 10_000L)
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            ui.postAtFrontOfQueue(runnable)
        }
    }

    private fun hide(reason: String = "") {
        val runnable = Runnable {
            ui.removeCallbacks(fingerUpHideTask)
            ui.removeCallbacks(safetyMaxDurationHideTask)
            bgHandler?.removeCallbacks(bgActiveMonitor)

            if (!isShowingAnim && overlayView?.visibility != View.VISIBLE) return@Runnable
            isShowingAnim = false
            shownOnLockscreen = false
            if (reason.isNotEmpty()) {
                lastTriggerSource = "Idle ($reason)"
            }
            overlayView?.let { v ->
                v.alpha = 0f
                v.visibility = View.INVISIBLE
                v.pauseAndResetToFrameZero()
                v.primeForImmediateStart()
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            ui.postAtFrontOfQueue(runnable)
        }
    }

    override fun onInterrupt() {
        hide("interrupted")
    }

    override fun onDestroy() {
        isRunning = false
        instance = null
        fingerDownOnSensor = false
        ui.removeCallbacksAndMessages(null)
        bgHandler?.removeCallbacksAndMessages(null)
        try {
            bgThread?.quitSafely()
        } catch (_: Throwable) {}
        bgThread = null
        bgHandler = null

        if (lockStateReceiverRegistered) {
            try {
                unregisterReceiver(lockStateReceiver)
            } catch (_: Throwable) {}
            lockStateReceiverRegistered = false
        }

        try {
            if (attached && overlayView != null) {
                wm.removeViewImmediate(overlayView)
            }
        } catch (_: Throwable) {}
        overlayView = null
        attached = false
        ShizukuBridge.onTouch = null
        super.onDestroy()
    }
}

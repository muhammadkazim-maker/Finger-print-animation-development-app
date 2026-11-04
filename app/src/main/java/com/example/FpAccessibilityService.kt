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
import android.os.PowerManager
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import kotlin.math.roundToInt

class FpAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: FpAccessibilityService? = null
            private set

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

    /**
     * Single event-driven gate state holder:
     *   visible = fingerDown && requestActive && !latchedTerminal
     */
    val gate = BiometricGateState()

    /**
     * Tracks the windowId of the currently active unlocked-screen BiometricPrompt / UDFPS dialog
     * so we can detect when that window closes via TYPE_WINDOWS_CHANGED or TYPE_WINDOW_STATE_CHANGED.
     */
    @Volatile
    private var activeBiometricPromptWindowId: Int = -1

    /**
     * Hard fail-safe runnable: guarantees the overlay never stays visible longer than
     * BiometricSignatures.MAX_OVERLAY_DURATION_MS (3,000 ms) if no end signal arrives.
     */
    private val hardMaxDurationFailSafe = Runnable {
        if (isShowing || gate.fingerDown) {
            DiagnosticLogger.log(
                "FAILSAFE",
                "Hard max duration (${BiometricSignatures.MAX_OVERLAY_DURATION_MS}ms) reached; latching terminal & hiding overlay",
                gate
            )
            gate.latchTerminal("hard_timeout_3s")
            syncOverlayWithGate(reason = "hard_timeout_3s")
        }
    }

    private val clearUnlockedPromptRunnable = Runnable {
        if (!gate.fingerDown && gate.unlockedPromptRequestActive) {
            activeBiometricPromptWindowId = -1
            gate.updateEnvironment(
                isLockOrAod = isLockOrAodState(),
                canAcceptLockScreenFp = canAcceptLockScreenFingerprint(),
                isUnlockedPromptActive = false
            )
            DiagnosticLogger.log("PROMPT_EXPIRE", "Unlocked biometric prompt expired after inactivity", gate)
            syncOverlayWithGate(reason = "prompt_expired")
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "debug_diag_enabled") {
            DiagnosticLogger.enabled = Prefs.isDebugDiagEnabled(this)
            DiagnosticLogger.log("DIAG", "DEBUG Diagnostic Mode toggled On", gate)
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
                    // Device unlocked! Immediately latch terminal and hide overlay.
                    activeBiometricPromptWindowId = -1
                    gate.clearLockScreenRestriction()
                    gate.updateEnvironment(
                        isLockOrAod = false,
                        canAcceptLockScreenFp = false,
                        isUnlockedPromptActive = false,
                        isLockScreenPinOnly = false
                    )
                    gate.latchTerminal("unlock_success")
                    DiagnosticLogger.log(
                        "BROADCAST",
                        "ACTION_USER_PRESENT (device unlocked) -> latchTerminal(unlock_success)",
                        gate
                    )
                    syncOverlayWithGate(reason = "ACTION_USER_PRESENT")
                    ShizukuBridge.setScreenInteractive(true)
                }
                Intent.ACTION_SCREEN_ON -> {
                    val onLock = isLockOrAodState()
                    gate.updateEnvironment(
                        isLockOrAod = onLock,
                        canAcceptLockScreenFp = canAcceptLockScreenFingerprint(),
                        isUnlockedPromptActive = gate.unlockedPromptRequestActive
                    )
                    DiagnosticLogger.log(
                        "BROADCAST",
                        "ACTION_SCREEN_ON (keyguardLocked=${km.isKeyguardLocked}, interactive=${pm.isInteractive})",
                        gate
                    )
                    syncOverlayWithGate(reason = "ACTION_SCREEN_ON")
                    ensurePersistentOverlayAttached()
                    ShizukuBridge.setScreenInteractive(true)
                    ShizukuBridge.tryBind()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    // Screen turned off: fail-safe requires removing/hiding the overlay immediately.
                    activeBiometricPromptWindowId = -1
                    gate.updateEnvironment(
                        isLockOrAod = true,
                        canAcceptLockScreenFp = false,
                        isUnlockedPromptActive = false
                    )
                    gate.latchTerminal("screen_off")
                    DiagnosticLogger.log(
                        "BROADCAST",
                        "ACTION_SCREEN_OFF (keyguardLocked=${km.isKeyguardLocked}) -> hide immediately",
                        gate
                    )
                    syncOverlayWithGate(reason = "ACTION_SCREEN_OFF")
                    ShizukuBridge.setScreenInteractive(false)
                }
            }
        }
    }

    private fun isLockOrAodState(): Boolean {
        return km.isKeyguardLocked || !pm.isInteractive
    }

    /**
     * Checks whether the lock screen can currently accept fingerprint authentication.
     * Returns false if the lock screen is not locked, or if we detected a lockout / PIN-only /
     * first-unlock-after-reboot message on the keyguard.
     */
    private fun canAcceptLockScreenFingerprint(): Boolean {
        if (!isLockOrAodState()) return false
        if (gate.lockScreenRestrictedPinOnly) return false
        return true
    }

    /**
     * Evaluates `gate.visible` (`fingerDown && requestActive && !latchedTerminal`)
     * and shows or hides the overlay accordingly.
     */
    private fun syncOverlayWithGate(reason: String) {
        val shouldShow = gate.visible
        if (shouldShow) {
            showImmediate(onLock = gate.onLockOrAod, reason = reason)
        } else {
            hideImmediate(reason = reason)
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

        // Pre-decode active animation on startup
        ProVideoFrameCache.pinActiveOverlayAtlas(this, Prefs.getSelectedVideoAsset(this))
        LottiePrewarmCache.prewarmAsync(this)
        ensurePersistentOverlayAttached()

        val onLockNow = isLockOrAodState()
        gate.updateEnvironment(
            isLockOrAod = onLockNow,
            canAcceptLockScreenFp = canAcceptLockScreenFingerprint(),
            isUnlockedPromptActive = false,
            isLockScreenPinOnly = false
        )
        DiagnosticLogger.log(
            "SERVICE",
            "FpAccessibilityService connected (keyguardLocked=${km.isKeyguardLocked}, interactive=${pm.isInteractive})",
            gate
        )

        // 1. Kernel-level touch callback via Shizuku UserService
        ShizukuBridge.onTouch = { down ->
            mainHandler.post {
                val onLock = isLockOrAodState()
                if (down) {
                    gate.updateEnvironment(
                        isLockOrAod = onLock,
                        canAcceptLockScreenFp = canAcceptLockScreenFingerprint(),
                        isUnlockedPromptActive = gate.unlockedPromptRequestActive
                    )
                    gate.onFingerDown()
                    DiagnosticLogger.log(
                        "TOUCH",
                        "fingerDown=true (onLock=$onLock, keyguard=${km.isKeyguardLocked}, interactive=${pm.isInteractive})",
                        gate
                    )
                    syncOverlayWithGate(reason = "touch_down")
                } else {
                    // Finger lifted: clears fingerDown AND clears latchedTerminal
                    gate.onFingerUp()
                    DiagnosticLogger.log(
                        "TOUCH",
                        "fingerDown=false (finger lifted -> latchedTerminal cleared)",
                        gate
                    )
                    syncOverlayWithGate(reason = "touch_up")
                }
            }
        }

        // 2. Real-time fingerprint mismatch / error callback from Shizuku logcat monitor
        ShizukuBridge.onFingerprintError = {
            mainHandler.post {
                gate.latchTerminal("mismatch_error")
                DiagnosticLogger.log(
                    "SHIZUKU_ERR",
                    "onFingerprintError -> latchTerminal(mismatch_error)",
                    gate
                )
                syncOverlayWithGate(reason = "shizuku_mismatch_error")
            }
        }

        // 3. Real-time BiometricPrompt / UDFPS session state callback from Shizuku logcat monitor
        ShizukuBridge.onBiometricPromptStateChanged = { active ->
            mainHandler.post {
                val onLock = isLockOrAodState()
                if (active) {
                    mainHandler.removeCallbacks(clearUnlockedPromptRunnable)
                    // Only mark unlockedPromptRequestActive when on the unlocked screen
                    val unlockedPrompt = !onLock
                    gate.updateEnvironment(
                        isLockOrAod = onLock,
                        canAcceptLockScreenFp = canAcceptLockScreenFingerprint(),
                        isUnlockedPromptActive = unlockedPrompt
                    )
                    DiagnosticLogger.log("SHIZUKU_BIO", "onBiometricPromptStateChanged(active=true, onLock=$onLock)", gate)
                    syncOverlayWithGate(reason = "shizuku_prompt_active")
                    if (unlockedPrompt) {
                        mainHandler.postDelayed(clearUnlockedPromptRunnable, 20_000L)
                    }
                } else {
                    mainHandler.removeCallbacks(clearUnlockedPromptRunnable)
                    activeBiometricPromptWindowId = -1
                    gate.latchTerminal("prompt_dismissed")
                    DiagnosticLogger.log("SHIZUKU_BIO", "onBiometricPromptStateChanged(active=false) -> latchTerminal", gate)
                    syncOverlayWithGate(reason = "shizuku_prompt_ended")
                }
            }
        }

        ShizukuBridge.init(this)
        ShizukuBridge.tryBind()
    }

    /**
     * Reloads calibration, sensor trigger radius, and active animation asset whenever changed in UI.
     */
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
            val bounds = Prefs.getRealDisplayBounds(this)
            val w = bounds.width().coerceAtLeast(1080)
            val h = bounds.height().coerceAtLeast(2400)
            val sizePx = (calib.size * w).roundToInt().coerceAtLeast(120)

            lp.width = sizePx
            lp.height = sizePx
            lp.x = (calib.cx * w - sizePx / 2f).roundToInt()
            lp.y = (calib.cy * h - sizePx / 2f).roundToInt()
            lp.flags = buildOverlayFlags(activePlayback = true)

            anim.bindActiveAnimation(this, calib.speed, sizePx, autoPlay = false)
            anim.playImmediate()
            view.alpha = 1f
            view.visibility = View.VISIBLE
            try {
                wm.updateViewLayout(view, lp)
            } catch (_: Throwable) {}

            mainHandler.postDelayed({
                if (!gate.visible) {
                    hideImmediate(reason = "test_preview_end")
                }
            }, durationMs)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val pkgName = ev.packageName?.toString() ?: ""
        val clsName = ev.className?.toString() ?: ""
        val eventType = ev.eventType

        val onLock = isLockOrAodState()
        val isBioPkg = BiometricSignatures.isBiometricPackage(pkgName)

        // Query the event's shallow source node for viewIdResourceName and text when it's a SystemUI/biometric package
        var sourceViewId: String? = null
        val sb = StringBuilder()
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

        if (isBioPkg && (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        ) {
            try {
                val src = ev.source
                if (src != null) {
                    sourceViewId = src.viewIdResourceName
                    if (!src.text.isNullOrEmpty()) {
                        if (sb.isNotEmpty()) sb.append(" | ")
                        sb.append(src.text)
                    }
                    if (!src.contentDescription.isNullOrEmpty()) {
                        if (sb.isNotEmpty()) sb.append(" | ")
                        sb.append(src.contentDescription)
                    }
                    // Also check up to 6 immediate children (O(1) bounded, no deep recursion)
                    val count = src.childCount.coerceAtMost(6)
                    for (i in 0 until count) {
                        val child = src.getChild(i) ?: continue
                        try {
                            if (sourceViewId == null && !child.viewIdResourceName.isNullOrEmpty()) {
                                sourceViewId = child.viewIdResourceName
                            }
                            if (!child.text.isNullOrEmpty()) {
                                if (sb.isNotEmpty()) sb.append(" | ")
                                sb.append(child.text)
                            }
                        } finally {
                            @Suppress("DEPRECATION")
                            child.recycle()
                        }
                    }
                    @Suppress("DEPRECATION")
                    src.recycle()
                }
            } catch (_: Throwable) {}
        }

        val combinedText = sb.toString()

        // Log SystemUI / biometric events in DEBUG Diagnostic Mode
        if (DiagnosticLogger.enabled && (isBioPkg || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
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
                gate
            )
        }

        // 1. Check for lockout / PIN-only / after-reboot restriction texts from SystemUI or Settings
        if (isBioPkg && combinedText.isNotEmpty() && BiometricSignatures.matchesLockoutOrPinOnlyText(combinedText)) {
            gate.updateEnvironment(
                isLockOrAod = onLock,
                canAcceptLockScreenFp = false,
                isUnlockedPromptActive = false,
                isLockScreenPinOnly = true
            )
            gate.latchTerminal("lockout_or_pin_only")
            DiagnosticLogger.log(
                "GATE_LOCKOUT",
                "Detected lockout / PIN-only state from text=\"$combinedText\"",
                gate
            )
            syncOverlayWithGate(reason = "lockout_or_pin_only")
            return
        }

        // 2. Check for fingerprint mismatch / "not recognized" / "try again" error texts
        if (isBioPkg && combinedText.isNotEmpty() && BiometricSignatures.matchesMismatchText(combinedText)) {
            gate.latchTerminal("mismatch_error")
            DiagnosticLogger.log(
                "GATE_MISMATCH",
                "Detected fingerprint mismatch error from text=\"$combinedText\" -> latchTerminal",
                gate
            )
            syncOverlayWithGate(reason = "a11y_mismatch_error")
            return
        }

        // 3. Check for prompt dismissal / cancel text
        if (isBioPkg && combinedText.isNotEmpty() && BiometricSignatures.matchesPromptDismissText(combinedText)) {
            activeBiometricPromptWindowId = -1
            gate.latchTerminal("prompt_dismissed")
            DiagnosticLogger.log(
                "GATE_DISMISS",
                "Detected biometric prompt cancel/dismiss text=\"$combinedText\"",
                gate
            )
            syncOverlayWithGate(reason = "a11y_prompt_dismiss")
            return
        }

        // 4. Detect BiometricPrompt / UDFPS overlay appearing
        val matchedByClass = isBioPkg && BiometricSignatures.matchesPromptClass(clsName)
        val matchedByViewId = isBioPkg && BiometricSignatures.matchesPromptViewId(sourceViewId)
        val matchedByText = isBioPkg && combinedText.isNotEmpty() && BiometricSignatures.matchesPromptText(combinedText)

        if (matchedByClass || matchedByViewId || matchedByText) {
            mainHandler.removeCallbacks(clearUnlockedPromptRunnable)
            if (!onLock && ev.windowId > 0) {
                activeBiometricPromptWindowId = ev.windowId
            }
            if (onLock && matchedByText) {
                gate.clearLockScreenRestriction()
            }
            // Only set isUnlockedPromptActive = true when the phone is unlocked (!onLock)
            val unlockedPrompt = !onLock
            gate.updateEnvironment(
                isLockOrAod = onLock,
                canAcceptLockScreenFp = canAcceptLockScreenFingerprint(),
                isUnlockedPromptActive = unlockedPrompt
            )
            DiagnosticLogger.log(
                "GATE_PROMPT_ON",
                "Biometric request active (byClass=$matchedByClass, byViewId=$matchedByViewId, byText=$matchedByText, onLock=$onLock, winId=${ev.windowId})",
                gate
            )
            syncOverlayWithGate(reason = "a11y_prompt_detected")
            if (unlockedPrompt) {
                mainHandler.postDelayed(clearUnlockedPromptRunnable, 20_000L)
            }
            return
        }

        // 5. Detect when an unlocked-screen BiometricPrompt disappears / closes:
        //    (a) TYPE_WINDOWS_CHANGED when the tracked activeBiometricPromptWindowId is no longer in `windows`, OR
        //    (b) TYPE_WINDOW_STATE_CHANGED moving focus to a non-biometric app window while unlocked.
        if (!onLock && gate.unlockedPromptRequestActive) {
            if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED && activeBiometricPromptWindowId > 0) {
                try {
                    val currentWinIds = windows?.map { it.id } ?: emptyList()
                    if (currentWinIds.isNotEmpty() && activeBiometricPromptWindowId !in currentWinIds) {
                        activeBiometricPromptWindowId = -1
                        mainHandler.removeCallbacks(clearUnlockedPromptRunnable)
                        gate.latchTerminal("prompt_dismissed")
                        DiagnosticLogger.log(
                            "GATE_PROMPT_OFF",
                            "Biometric window closed (TYPE_WINDOWS_CHANGED, current=$currentWinIds)",
                            gate
                        )
                        syncOverlayWithGate(reason = "biometric_window_removed")
                        return
                    }
                } catch (_: Throwable) {}
            } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                pkgName.isNotEmpty() &&
                pkgName != packageName &&
                !isBioPkg
            ) {
                // Foreground transitioned to a normal app / launcher window (prompt finished or dismissed)
                activeBiometricPromptWindowId = -1
                mainHandler.removeCallbacks(clearUnlockedPromptRunnable)
                gate.latchTerminal("prompt_dismissed")
                DiagnosticLogger.log(
                    "GATE_PROMPT_OFF",
                    "Foreground switched to non-biometric window ($pkgName / $clsName) -> prompt ended",
                    gate
                )
                syncOverlayWithGate(reason = "foreground_app_switched")
                return
            }
        }

        // 6. Keep lock-screen environment state synchronized on window state changes
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val prevLock = gate.onLockOrAod
            if (prevLock != onLock) {
                gate.updateEnvironment(
                    isLockOrAod = onLock,
                    canAcceptLockScreenFp = canAcceptLockScreenFingerprint(),
                    isUnlockedPromptActive = if (onLock) false else gate.unlockedPromptRequestActive
                )
                DiagnosticLogger.log("ENV_SYNC", "Lock state changed: prev=$prevLock -> now=$onLock", gate)
                syncOverlayWithGate(reason = "lock_state_changed")
            }
        }
    }

    private fun ensurePersistentOverlayAttached() {
        if (isOverlayAttached && overlayContainer != null && animHostView != null) {
            return
        }

        val calib = Prefs.load(this)
        val bounds = Prefs.getRealDisplayBounds(this)
        val w = bounds.width().coerceAtLeast(1080)
        val h = bounds.height().coerceAtLeast(2400)
        val sizePx = (calib.size * w).roundToInt().coerceAtLeast(120)

        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            visibility = View.INVISIBLE
            alpha = 0f
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        val hostView = OverlayAnimHostView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        }

        hostView.bindActiveAnimation(this, calib.speed, sizePx, autoPlay = false)
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
        val bounds = Prefs.getRealDisplayBounds(this)
        val w = bounds.width().coerceAtLeast(1080)
        val h = bounds.height().coerceAtLeast(2400)
        val sizePx = (calib.size * w).roundToInt().coerceAtLeast(120)

        lp.width = sizePx
        lp.height = sizePx
        lp.x = (calib.cx * w - sizePx / 2f).roundToInt()
        lp.y = (calib.cy * h - sizePx / 2f).roundToInt()

        hostView.bindActiveAnimation(this, calib.speed, sizePx, autoPlay = isShowing)

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
                activeWakeLock?.acquire(BiometricSignatures.MAX_OVERLAY_DURATION_MS)
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

    private fun showImmediate(onLock: Boolean = isLockOrAodState(), reason: String = "") {
        ensurePersistentOverlayAttached()
        val view = overlayContainer ?: return
        val anim = animHostView ?: return
        val lp = overlayLp ?: return

        acquireAodWakeLockIfNeeded(onLock)

        if (!isShowing) {
            isShowing = true
            val calib = Prefs.load(this)
            val bounds = Prefs.getRealDisplayBounds(this)
            val w = bounds.width().coerceAtLeast(1080)
            val h = bounds.height().coerceAtLeast(2400)
            val sizePx = (calib.size * w).roundToInt().coerceAtLeast(120)

            val newFlags = buildOverlayFlags(activePlayback = true)
            val geomChanged = lp.width != sizePx ||
                lp.height != sizePx ||
                lp.x != (calib.cx * w - sizePx / 2f).roundToInt() ||
                lp.y != (calib.cy * h - sizePx / 2f).roundToInt() ||
                lp.flags != newFlags

            if (geomChanged) {
                lp.width = sizePx
                lp.height = sizePx
                lp.x = (calib.cx * w - sizePx / 2f).roundToInt()
                lp.y = (calib.cy * h - sizePx / 2f).roundToInt()
                lp.flags = newFlags
                try {
                    wm.updateViewLayout(view, lp)
                } catch (_: Throwable) {}
            }

            anim.setSpeed(calib.speed)
            anim.playImmediate()
            view.alpha = 1f
            view.visibility = View.VISIBLE

            // Arm the hard 3-second maximum duration fail-safe timer
            mainHandler.removeCallbacks(hardMaxDurationFailSafe)
            mainHandler.postDelayed(hardMaxDurationFailSafe, BiometricSignatures.MAX_OVERLAY_DURATION_MS)

            DiagnosticLogger.log("OVERLAY", "SHOW overlay (reason=$reason, onLock=$onLock)", gate)
        }
    }

    private fun hideImmediate(reason: String = "") {
        mainHandler.removeCallbacks(hardMaxDurationFailSafe)
        if (!isShowing && overlayContainer?.visibility != View.VISIBLE) {
            releaseAodWakeLock()
            return
        }
        isShowing = false
        val view = overlayContainer ?: return
        val anim = animHostView ?: return
        val lp = overlayLp

        anim.pauseAndResetToFrameZero()
        view.alpha = 0f
        view.visibility = View.INVISIBLE

        if (lp != null) {
            val idleFlags = buildOverlayFlags(activePlayback = false)
            if (lp.flags != idleFlags) {
                lp.flags = idleFlags
                try {
                    wm.updateViewLayout(view, lp)
                } catch (_: Throwable) {}
            }
        }
        releaseAodWakeLock()
        DiagnosticLogger.log("OVERLAY", "HIDE overlay (reason=$reason)", gate)
    }

    override fun onInterrupt() {
        gate.resetAll("service_interrupt")
        hideImmediate(reason = "onInterrupt")
    }

    override fun onDestroy() {
        instance = null
        mainHandler.removeCallbacks(hardMaxDurationFailSafe)
        mainHandler.removeCallbacks(clearUnlockedPromptRunnable)
        gate.resetAll("service_destroy")
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

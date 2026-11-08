package com.example

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth for all biometric packages, class names, view resource IDs,
 * prompt strings, mismatch strings, logcat patterns, and timeout constants on the itel S25.
 *
 * Update the lists below with exact values captured by the "Record scenario (15s)" tool
 * in StockAnimationInspectorScreen.
 */
object BiometricSignatures {

    const val LOG_TAG = "FPAnim"

    /**
     * Hard maximum duration (5 seconds) that the overlay may stay visible during a single touch.
     * Even if the finger remains down and no end signal arrives, the overlay hides at 5,000 ms
     * and latches until the finger lifts.
     */
    const val HARD_MAX_OVERLAY_DURATION_MS: Long = 5_000L

    /**
     * Errors/mismatches may latch ONLY if the finger is currently down or went down within
     * the last 1,500 ms. Stray error texts/logs outside this window are ignored.
     */
    const val ERROR_LATCH_RECENT_TOUCH_WINDOW_MS: Long = 1_500L

    /**
     * Stale-state watchdog timeout for unlocked biometric prompts if no window/log update arrives.
     */
    const val STALE_PROMPT_WATCHDOG_MS: Long = 15_000L

    /**
     * Logcat tags monitored by TouchUserService on AOSP + itel OS / Transsion Android 12–15.
     */
    val LOGCAT_BIOMETRIC_TAGS: Array<String> = arrayOf(
        "FingerprintService:V",
        "Fingerprint21:V",
        "FingerprintProvider:V",
        "BiometricService:V",
        "BiometricScheduler:V",
        "AuthenticationClient:V",
        "AcquisitionClient:V",
        "KeyguardUpdateMonitor:V",
        "KeyguardBiometricLockoutLogger:V",
        "AuthController:V",
        "UdfpsController:V",
        "UdfpsControllerOverlay:V",
        "FodController:V",
        "FingerprintHal:V",
        "BiometricPrompt:V",
        "FingerprintManager:V",
        "*:S"
    )

    /**
     * Packages allowed to emit accessibility events for biometric prompts, UDFPS overlays, or AppLock.
     * Normal third-party apps never get their text inspected here, preventing stray "try again" texts
     * in arbitrary apps from ever latching an error.
     */
    val SYSTEM_BIOMETRIC_PACKAGES: Set<String> = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.transsion.applock",
        "com.transsion.security",
        "com.transsion.biometric",
        "com.transsion.fingerprint",
        "com.transsion.phonemaster",
        "com.itel.systemui",
        "com.itel.security"
    )

    val SYSTEM_PACKAGE_PREFIXES: List<String> = listOf(
        "com.android.systemui",
        "com.transsion.",
        "com.itel."
    )

    /**
     * Priority 1 (Class Names): Substrings of SystemUI / Settings / Transsion classes that host
     * an active unlocked-screen biometric verification request.
     */
    val PROMPT_CLASS_KEYWORDS: List<String> = listOf(
        "biometricprompt",
        "authcontainerview",
        "authbiometric",
        "biometricview",
        "fingerprintdialog",
        "udfpscontrolleroverlay",
        "udfpsview",
        "fodview",
        "confirmdevicecredential",
        "fingerprintenroll",
        "fingerprintsettings",
        "applock",
        "biometricactivity"
    )

    /**
     * Priority 2 (View Resource IDs): Substrings of view IDs inside SystemUI / Settings / AppLock
     * that appear when a biometric prompt is active.
     */
    val PROMPT_VIEW_ID_KEYWORDS: List<String> = listOf(
        "biometric_icon",
        "fingerprint_icon",
        "udfps_view",
        "fod_view",
        "fod_icon",
        "auth_credential",
        "biometric_scrollview"
    )

    /**
     * Priority 3 (Prompt Texts): Lowercase substrings indicating the OS is asking for a fingerprint.
     */
    val PROMPT_TEXT_KEYWORDS: List<String> = listOf(
        "touch the fingerprint sensor",
        "confirm your fingerprint",
        "use fingerprint",
        "scan your fingerprint",
        "verify your identity",
        "verify with fingerprint",
        "place your finger on the sensor",
        "place your finger",
        "confirm with fingerprint",
        "touch sensor",
        "unlock with fingerprint",
        "authenticate with fingerprint"
    )

    /**
     * Lowercase substrings in SystemUI / Settings events indicating a fingerprint mismatch or bad scan.
     */
    val MISMATCH_ERROR_KEYWORDS: List<String> = listOf(
        "not recognized",
        "try again",
        "didn't recognize",
        "couldn't recognize",
        "not match",
        "doesn't match",
        "does not match",
        "mismatch",
        "partial fingerprint",
        "sensor is dirty",
        "clean the sensor",
        "too fast",
        "finger moved",
        "wrong fingerprint",
        "authentication failed",
        "couldn't verify",
        "press firmer",
        "cover the entire sensor"
    )

    /**
     * Lowercase substrings indicating the lock screen or prompt is in PIN-only / lockout / post-reboot mode.
     */
    val LOCKOUT_OR_PIN_ONLY_KEYWORDS: List<String> = listOf(
        "too many attempts",
        "too many unsuccessful attempts",
        "try again in",
        "biometrics disabled",
        "fingerprint disabled",
        "fingerprint unlock disabled",
        "after restart",
        "after reboot",
        "device was restarted",
        "for additional security",
        "enter pin to unlock",
        "enter password to unlock",
        "draw pattern to unlock",
        "pin required",
        "password required",
        "pattern required"
    )

    /**
     * Lowercase substrings indicating the biometric prompt was cancelled or dismissed.
     */
    val PROMPT_DISMISS_OR_FALLBACK_KEYWORDS: List<String> = listOf(
        "authentication canceled",
        "authentication cancelled",
        "user canceled",
        "biometric error"
    )

    /**
     * Logcat lowercase substrings (used in TouchUserService) indicating a fingerprint mismatch / failed scan.
     */
    val LOGCAT_MISMATCH_SUBSTRINGS: List<String> = listOf(
        "onauthenticationfailed",
        "onauthenticated(false)",
        "authenticated=false",
        "authenticated: false",
        "authenticated(false)",
        "auth_failed",
        "handleauthenticated(false)",
        "onfingerprintauthenticated(false)",
        "handlefingerprintauthfailed",
        "onbiometricauthfailed",
        "fingerprint_acquired_partial",
        "fingerprint_acquired_insufficient",
        "fingerprint_acquired_imager_dirty",
        "fingerprint_acquired_too_fast",
        "fingerprint_acquired_too_slow",
        "not recognized",
        "mismatch"
    )

    /**
     * Logcat lowercase substrings (used in TouchUserService) indicating an unlocked biometric prompt started.
     */
    val LOGCAT_PROMPT_START_SUBSTRINGS: List<String> = listOf(
        "showauthenticationdialog",
        "authcontainerview",
        "startauthentication",
        "prepareforauthentication",
        "starting authentication",
        "showudfpsoverlay"
    )

    /**
     * Logcat lowercase substrings (used in TouchUserService) indicating a biometric prompt ended/succeeded/cancelled.
     */
    val LOGCAT_PROMPT_STOP_SUBSTRINGS: List<String> = listOf(
        "stopauthentication",
        "cancelauthentication",
        "dismissauthenticationdialog",
        "hideudfps",
        "onauthenticationsucceeded",
        "onauthenticated(true)",
        "authenticated=true",
        "authenticated: true",
        "ondismissed",
        "finishing client",
        "client stopped"
    )

    fun isBiometricPackage(pkgName: String): Boolean {
        if (pkgName.isEmpty()) return false
        val lower = pkgName.lowercase(Locale.ROOT)
        if (lower in SYSTEM_BIOMETRIC_PACKAGES) return true
        return SYSTEM_PACKAGE_PREFIXES.any { lower.startsWith(it) }
    }

    fun matchesPromptClass(className: String): Boolean {
        if (className.isEmpty()) return false
        val lower = className.lowercase(Locale.ROOT)
        return PROMPT_CLASS_KEYWORDS.any { lower.contains(it) }
    }

    fun matchesPromptViewId(viewIdResourceName: String?): Boolean {
        if (viewIdResourceName.isNullOrEmpty()) return false
        val lower = viewIdResourceName.lowercase(Locale.ROOT)
        return PROMPT_VIEW_ID_KEYWORDS.any { lower.contains(it) }
    }

    fun matchesPromptText(text: String): Boolean {
        if (text.isEmpty()) return false
        val lower = text.lowercase(Locale.ROOT)
        return PROMPT_TEXT_KEYWORDS.any { lower.contains(it) }
    }

    fun matchesMismatchText(text: String): Boolean {
        if (text.isEmpty()) return false
        val lower = text.lowercase(Locale.ROOT)
        return MISMATCH_ERROR_KEYWORDS.any { lower.contains(it) }
    }

    fun matchesLockoutOrPinOnlyText(text: String): Boolean {
        if (text.isEmpty()) return false
        val lower = text.lowercase(Locale.ROOT)
        return LOCKOUT_OR_PIN_ONLY_KEYWORDS.any { lower.contains(it) }
    }

    fun matchesPromptDismissText(text: String): Boolean {
        if (text.isEmpty()) return false
        val lower = text.lowercase(Locale.ROOT)
        return PROMPT_DISMISS_OR_FALLBACK_KEYWORDS.any { lower.contains(it) }
    }
}

/**
 * Sole owner of `isBiometricPromptActive`, `fingerDown`, and `latchedTerminal`.
 * No other class in the project can mutate `isBiometricPromptActive`.
 *
 * Visibility invariant:
 *   `visible = fingerDown && requestActive && !latchedTerminal`
 * where:
 *   `requestActive = if (onLockOrAod) (!lockScreenRestrictedPinOnly) else isBiometricPromptActive`
 */
class BiometricRequestTracker {

    var fingerDown: Boolean = false
        private set

    var lastFingerDownAtMs: Long = 0L
        private set

    var onLockOrAod: Boolean = false
        private set

    var lockScreenRestrictedPinOnly: Boolean = false
        private set

    var isBiometricPromptActive: Boolean = false
        private set

    var activePromptSource: String = "none"
        private set

    var activePromptWindowId: Int = -1
        private set

    var latchedTerminal: Boolean = false
        private set

    var lastTerminalReason: String = "none"
        private set

    var lastDecisionReason: String = "Idle"
        private set

    val requestActive: Boolean
        get() = if (onLockOrAod) {
            !lockScreenRestrictedPinOnly
        } else {
            isBiometricPromptActive
        }

    val visible: Boolean
        get() = fingerDown && requestActive && !latchedTerminal

    /**
     * Updates whether the device is currently on the lock screen / AOD.
     */
    fun syncLockState(isLockOrAodNow: Boolean) {
        if (onLockOrAod != isLockOrAodNow) {
            onLockOrAod = isLockOrAodNow
            if (isLockOrAodNow) {
                // While on lock screen, clear any unlocked prompt state so it cannot leak after unlock
                isBiometricPromptActive = false
                activePromptSource = "none"
                activePromptWindowId = -1
            }
        }
    }

    /**
     * Records finger-down inside the calibrated inner sensor circle.
     */
    fun onFingerDown(isLockOrAodNow: Boolean, nowMs: Long = System.currentTimeMillis()): Boolean {
        syncLockState(isLockOrAodNow)
        if (!fingerDown) {
            // A fresh touch after the finger was up always starts with latchedTerminal = false
            latchedTerminal = false
            lastTerminalReason = "none"
        }
        fingerDown = true
        lastFingerDownAtMs = nowMs

        lastDecisionReason = when {
            latchedTerminal -> "Blocked: terminal latched ($lastTerminalReason) while finger held down"
            onLockOrAod && lockScreenRestrictedPinOnly -> "Blocked: lock screen restricted (PIN-only / lockout)"
            onLockOrAod -> "Show: lock screen finger down"
            isBiometricPromptActive -> "Show: unlocked biometric prompt active ($activePromptSource)"
            else -> "Blocked: unlocked, no biometric request"
        }
        return visible
    }

    /**
     * Records finger lift outside/off the sensor circle.
     * Clears `fingerDown` and resets `latchedTerminal`.
     */
    fun onFingerUp(): Boolean {
        val wasVisible = visible
        fingerDown = false
        latchedTerminal = false
        lastTerminalReason = "none"
        if (wasVisible) {
            lastDecisionReason = "Hide: finger lifted"
        }
        return false
    }

    /**
     * Called when explicit start evidence arrives for an unlocked biometric prompt
     * (from SystemUI / Transsion / Settings accessibility event or TouchUserService logcat).
     */
    fun onPromptStarted(source: String, windowId: Int = -1, isLockOrAodNow: Boolean = onLockOrAod): Boolean {
        syncLockState(isLockOrAodNow)
        if (onLockOrAod) {
            // Lock screen uses onLockOrAod directly; do not set unlocked prompt active on lock screen
            return visible
        }
        isBiometricPromptActive = true
        activePromptSource = source
        if (windowId > 0) {
            activePromptWindowId = windowId
        }
        if (fingerDown && !latchedTerminal) {
            lastDecisionReason = "Show: unlocked biometric prompt active ($source)"
        }
        return visible
    }

    /**
     * Called when explicit stop/cancel/dismiss/success evidence arrives for a biometric request,
     * or when the stale-state watchdog confirms the prompt window is gone.
     */
    fun onPromptStopped(reason: String): Boolean {
        val wasVisible = visible
        isBiometricPromptActive = false
        activePromptSource = "none"
        activePromptWindowId = -1
        if (fingerDown) {
            latchedTerminal = true
            lastTerminalReason = reason
        }
        if (wasVisible) {
            lastDecisionReason = "Hide: biometric request ended ($reason)"
        }
        return visible
    }

    /**
     * Called on device unlock (`ACTION_USER_PRESENT`).
     */
    fun onDeviceUnlocked(): Boolean {
        onLockOrAod = false
        lockScreenRestrictedPinOnly = false
        isBiometricPromptActive = false
        activePromptSource = "none"
        activePromptWindowId = -1
        if (fingerDown) {
            latchedTerminal = true
            lastTerminalReason = "unlock_success"
        }
        lastDecisionReason = "Hide: unlocked (ACTION_USER_PRESENT)"
        return false
    }

    /**
     * Called on fingerprint mismatch / "not recognized" error.
     * Enforces Section 4 requirement:
     *   "Errors may latch only if the finger is currently down (or went down less than ~1.5 s ago);
     *    ignore them otherwise."
     * Returns true if the error was accepted and latched/applied, false if ignored as stray.
     */
    fun onMismatchError(source: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val recentlyDown = lastFingerDownAtMs > 0L &&
            (nowMs - lastFingerDownAtMs) <= BiometricSignatures.ERROR_LATCH_RECENT_TOUCH_WINDOW_MS
        if (!fingerDown && !recentlyDown) {
            lastDecisionReason = "Ignored stray error ($source): finger not down"
            return false
        }
        if (fingerDown) {
            latchedTerminal = true
            lastTerminalReason = "mismatch:$source"
        }
        lastDecisionReason = "Hide: mismatch error ($source)"
        return true
    }

    /**
     * Called when SystemUI / Keyguard displays lockout or PIN-only text ("Too many attempts", "After restart").
     */
    fun onLockoutOrPinOnly(source: String): Boolean {
        lockScreenRestrictedPinOnly = true
        isBiometricPromptActive = false
        activePromptSource = "none"
        activePromptWindowId = -1
        if (fingerDown) {
            latchedTerminal = true
            lastTerminalReason = "lockout_or_pin_only"
        }
        lastDecisionReason = "Hide: lockout or PIN-only ($source)"
        return false
    }

    fun clearLockScreenRestriction() {
        lockScreenRestrictedPinOnly = false
    }

    /**
     * Called when the hard maximum duration (5,000 ms) expires while the overlay is showing.
     * Hides even with the finger down, and latches until the finger lifts.
     */
    fun onHardMaxDurationReached(): Boolean {
        if (fingerDown) {
            latchedTerminal = true
            lastTerminalReason = "hard_max_duration_5s"
        }
        isBiometricPromptActive = false
        activePromptSource = "none"
        activePromptWindowId = -1
        lastDecisionReason = "Hide: hard max duration (${BiometricSignatures.HARD_MAX_OVERLAY_DURATION_MS}ms) reached"
        return false
    }

    /**
     * Called on screen off (`ACTION_SCREEN_OFF`) or service stop/interrupt.
     */
    fun onScreenOffOrReset(reason: String): Boolean {
        isBiometricPromptActive = false
        activePromptSource = "none"
        activePromptWindowId = -1
        if (fingerDown) {
            latchedTerminal = true
            lastTerminalReason = reason
        }
        lastDecisionReason = "Hide: $reason"
        return false
    }

    override fun toString(): String {
        return "Tracker[visible=$visible, fingerDown=$fingerDown, requestActive=$requestActive " +
            "(onLock=$onLockOrAod, prompt=$isBiometricPromptActive[$activePromptSource,win=$activePromptWindowId], pinOnly=$lockScreenRestrictedPinOnly), " +
            "latched=$latchedTerminal($lastTerminalReason), reason=\"$lastDecisionReason\"]"
    }
}

// Typealias for backward compatibility with existing tests
typealias BiometricGateState = BiometricRequestTracker

/**
 * Diagnostic event ring buffer for the 15-second Scenario Recorder and DEBUG Diagnostic Mode.
 */
object DiagnosticLogger {
    private const val TAG = "FPAnimDiag"
    private const val MAX_LINES = 250

    @Volatile
    var enabled: Boolean = false

    private val buffer = ArrayDeque<String>(MAX_LINES)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(category: String, message: String, tracker: BiometricRequestTracker? = null) {
        if (!enabled) return
        val ts = synchronized(timeFormat) { timeFormat.format(Date()) }
        val suffix = if (tracker != null) " | $tracker" else ""
        val line = "$ts [$category] $message$suffix"
        Log.d(TAG, line)
        synchronized(buffer) {
            if (buffer.size >= MAX_LINES) {
                buffer.removeFirst()
            }
            buffer.addLast(line)
        }
    }

    fun dumpLines(maxLines: Int = 120): List<String> {
        synchronized(buffer) {
            return buffer.toList().takeLast(maxLines)
        }
    }

    fun dumpText(): String {
        synchronized(buffer) {
            if (buffer.isEmpty()) {
                return "(No accessibility/gate events recorded in buffer)"
            }
            return buffer.joinToString("\n")
        }
    }

    fun clear() {
        synchronized(buffer) {
            buffer.clear()
        }
    }
}

package com.example

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized signatures and event-driven GateState for inferring when the OS (itel OS / SystemUI)
 * is actively requesting fingerprint verification on an unrooted itel S25.
 *
 * Edit the lists below once you capture exact class names, view IDs, or localized strings
 * from DEBUG Diagnostic Mode on your device.
 */
object BiometricSignatures {

    /**
     * Hard fail-safe maximum duration (in milliseconds) that the animation overlay is allowed
     * to remain visible during a single finger-down session if no end signal arrives.
     */
    const val MAX_OVERLAY_DURATION_MS: Long = 3_000L

    /**
     * Packages allowed to emit biometric prompt, UDFPS/FOD overlay, or AppLock verification events.
     * Normal third-party apps invoking BiometricPrompt delegate the actual dialog to `com.android.systemui`.
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
        "com.itel.security",
        "com.rlk.weathers"
    )

    /**
     * Prefixes for OEM system packages on itel / Transsion ROMs.
     */
    val SYSTEM_PACKAGE_PREFIXES: List<String> = listOf(
        "com.android.systemui",
        "com.transsion.",
        "com.itel."
    )

    /**
     * 1st Priority: Class name substrings identifying a BiometricPrompt, UDFPS/FOD sensor request,
     * Settings fingerprint enrollment/confirmation, or Transsion AppLock fingerprint screen.
     */
    val PROMPT_CLASS_KEYWORDS: List<String> = listOf(
        "biometricprompt",
        "authcontainerview",
        "authbiometric",
        "biometricview",
        "fingerprintdialog",
        "udfps",
        "fodview",
        "fodicon",
        "confirmdevicecredential",
        "fingerprintenroll",
        "fingerprintsettings",
        "applock",
        "biometricactivity"
    )

    /**
     * 1st Priority (View IDs): Resource ID entry substrings inside SystemUI or Settings
     * that only appear when a biometric prompt or in-display fingerprint sensor icon is active.
     */
    val PROMPT_VIEW_ID_KEYWORDS: List<String> = listOf(
        "biometric_icon",
        "fingerprint_icon",
        "udfps_view",
        "udfps_aod_fp",
        "udfps_lockscreen_fp",
        "fod_view",
        "fod_icon",
        "auth_credential",
        "biometric_scrollview"
    )

    /**
     * 2nd Priority (Texts): Lowercase substrings indicating the OS is actively asking the user
     * to touch/scan the fingerprint sensor on an unlocked screen or prompt dialog.
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
        "fingerprint to unlock",
        "unlock with fingerprint",
        "authenticate with fingerprint"
    )

    /**
     * Lowercase substrings indicating a fingerprint mismatch / "not recognized" / acquisition error.
     * Matching any of these immediately sets `latchedTerminal = true` and hides the overlay
     * until the user lifts their finger (`fingerDown = false`).
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
        "move Finger",
        "wrong fingerprint",
        "authentication failed",
        "couldn't verify",
        "unable to process",
        "press firmer",
        "cover the entire sensor"
    )

    /**
     * Lowercase substrings indicating the lock screen or biometric prompt is in a state where
     * fingerprint authentication is NOT accepted (first unlock after reboot, biometric lockout
     * after too many failed attempts, or PIN/Pattern/Password-only bouncer).
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
     * Lowercase substrings indicating the user dismissed/cancelled the biometric prompt
     * or switched to negative button ("Use PIN", "Use password", "Cancel").
     */
    val PROMPT_DISMISS_OR_FALLBACK_KEYWORDS: List<String> = listOf(
        "authentication canceled",
        "authentication cancelled",
        "user canceled",
        "biometric error"
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
 * Single event-driven state holder governing overlay visibility:
 *   `visible = fingerDown && requestActive && !latchedTerminal`
 *
 * Rules enforced:
 * 1. Animation shows ONLY IF (`fingerDown` && `requestActive` && `!latchedTerminal`).
 * 2. When a terminal event occurs (unlock success, mismatch error, cancel, lockout, or 3s fail-safe),
 *    `latchedTerminal` is set to `true` and stays `true` as long as `fingerDown` remains `true`.
 * 3. `latchedTerminal` is cleared ONLY when `fingerDown` transitions to `false` (finger lifts).
 */
class BiometricGateState {
    var fingerDown: Boolean = false
        private set

    var lockScreenRequestActive: Boolean = false
        private set

    var unlockedPromptRequestActive: Boolean = false
        private set

    var lockScreenRestrictedPinOnly: Boolean = false
        private set

    var onLockOrAod: Boolean = false
        private set

    var latchedTerminal: Boolean = false
        private set

    var lastTerminalReason: String = "none"
        private set

    val requestActive: Boolean
        get() = if (onLockOrAod) {
            lockScreenRequestActive && !lockScreenRestrictedPinOnly
        } else {
            unlockedPromptRequestActive
        }

    val visible: Boolean
        get() = fingerDown && requestActive && !latchedTerminal

    fun onFingerDown(): Boolean {
        fingerDown = true
        return visible
    }

    fun onFingerUp(): Boolean {
        fingerDown = false
        latchedTerminal = false
        lastTerminalReason = "none"
        return visible
    }

    fun updateEnvironment(
        isLockOrAod: Boolean,
        canAcceptLockScreenFp: Boolean,
        isUnlockedPromptActive: Boolean,
        isLockScreenPinOnly: Boolean = lockScreenRestrictedPinOnly
    ): Boolean {
        onLockOrAod = isLockOrAod
        lockScreenRequestActive = isLockOrAod && canAcceptLockScreenFp
        unlockedPromptRequestActive = isUnlockedPromptActive
        lockScreenRestrictedPinOnly = isLockScreenPinOnly
        return visible
    }

    fun latchTerminal(reason: String): Boolean {
        latchedTerminal = true
        lastTerminalReason = reason
        if (reason == "unlock_success" || reason == "prompt_dismissed" || reason == "screen_off") {
            unlockedPromptRequestActive = false
        }
        if (reason == "lockout_or_pin_only") {
            lockScreenRestrictedPinOnly = true
        }
        return visible
    }

    fun clearLockScreenRestriction() {
        lockScreenRestrictedPinOnly = false
    }

    fun resetAll(reason: String) {
        fingerDown = false
        lockScreenRequestActive = false
        unlockedPromptRequestActive = false
        latchedTerminal = false
        lastTerminalReason = reason
    }

    override fun toString(): String {
        return "Gate[visible=$visible, fingerDown=$fingerDown, requestActive=$requestActive " +
            "(lock=$lockScreenRequestActive, prompt=$unlockedPromptRequestActive, pinOnly=$lockScreenRestrictedPinOnly, onLock=$onLockOrAod), " +
            "latchedTerminal=$latchedTerminal($lastTerminalReason)]"
    }
}

/**
 * Hidden, off-by-default DEBUG diagnostic logger.
 * When enabled, logs timestamped SystemUI accessibility events, window changes, keyguard/screen states,
 * touch zone triggers, and gate transitions to both Logcat (`tag = FPAnimDiag`) and an in-memory ring buffer
 * so the user can copy and paste logs without needing a PC.
 */
object DiagnosticLogger {
    private const val TAG = "FPAnimDiag"
    private const val MAX_LINES = 250

    @Volatile
    var enabled: Boolean = false

    private val buffer = ArrayDeque<String>(MAX_LINES)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(category: String, message: String, gate: BiometricGateState? = null) {
        if (!enabled) return
        val ts = synchronized(timeFormat) { timeFormat.format(Date()) }
        val gateSuffix = if (gate != null) " | $gate" else ""
        val line = "$ts [$category] $message$gateSuffix"
        Log.d(TAG, line)
        synchronized(buffer) {
            if (buffer.size >= MAX_LINES) {
                buffer.removeFirst()
            }
            buffer.addLast(line)
        }
    }

    fun dumpText(): String {
        synchronized(buffer) {
            if (buffer.isEmpty()) {
                return "No diagnostic events recorded yet.\n" +
                    "1. Enable DEBUG Diagnostic Mode above.\n" +
                    "2. Perform the 4 fingerprint tests (lock screen match, lock screen mismatch, unlocked prompt, home screen touch).\n" +
                    "3. Return here and tap Copy Logs."
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

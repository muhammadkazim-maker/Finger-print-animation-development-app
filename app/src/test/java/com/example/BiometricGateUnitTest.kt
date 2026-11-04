package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricGateUnitTest {

    @Test
    fun `unlocked screen with no active biometric prompt never shows overlay`() {
        val gate = BiometricGateState()
        gate.updateEnvironment(isLockOrAod = false, canAcceptLockScreenFp = false, isUnlockedPromptActive = false)
        assertFalse(gate.requestActive)
        assertFalse(gate.onFingerDown())
        assertFalse(gate.visible)
        gate.onFingerUp()
    }

    @Test
    fun `lock screen shows on touch and latches terminal on mismatch until finger lifts`() {
        val gate = BiometricGateState()
        gate.updateEnvironment(isLockOrAod = true, canAcceptLockScreenFp = true, isUnlockedPromptActive = false)
        assertTrue(gate.requestActive)

        // Finger down -> visible = true
        assertTrue(gate.onFingerDown())
        assertTrue(gate.visible)

        // Mismatch error -> visible immediately false, latchedTerminal = true
        assertFalse(gate.latchTerminal("mismatch_error"))
        assertTrue(gate.latchedTerminal)
        assertFalse(gate.visible)

        // Keeping same finger down must NOT re-show the overlay
        assertFalse(gate.onFingerDown())
        assertFalse(gate.visible)

        // Lifting finger clears latchedTerminal
        assertFalse(gate.onFingerUp())
        assertFalse(gate.latchedTerminal)

        // Touching again while requestActive is true shows overlay again
        assertTrue(gate.onFingerDown())
        assertTrue(gate.visible)
    }

    @Test
    fun `lock screen in PIN-only or lockout restriction shows nothing`() {
        val gate = BiometricGateState()
        gate.updateEnvironment(
            isLockOrAod = true,
            canAcceptLockScreenFp = true,
            isUnlockedPromptActive = false,
            isLockScreenPinOnly = true
        )
        assertFalse(gate.requestActive)
        assertFalse(gate.onFingerDown())
        assertFalse(gate.visible)
    }

    @Test
    fun `unlocked screen with active BiometricPrompt shows on touch and hides on success or dismiss`() {
        val gate = BiometricGateState()
        gate.updateEnvironment(isLockOrAod = false, canAcceptLockScreenFp = false, isUnlockedPromptActive = true)
        assertTrue(gate.requestActive)
        assertTrue(gate.onFingerDown())
        assertTrue(gate.visible)

        // Prompt succeeds -> hides immediately and clears unlockedPromptRequestActive
        assertFalse(gate.latchTerminal("unlock_success"))
        assertFalse(gate.visible)
        assertFalse(gate.requestActive)
        gate.onFingerUp()
    }

    @Test
    fun `BiometricSignatures matches SystemUI packages, classes, view IDs, prompt texts, and mismatch errors`() {
        assertTrue(BiometricSignatures.isBiometricPackage("com.android.systemui"))
        assertTrue(BiometricSignatures.isBiometricPackage("com.transsion.applock"))
        assertFalse(BiometricSignatures.isBiometricPackage("com.whatsapp"))

        assertTrue(BiometricSignatures.matchesPromptClass("com.android.systemui.biometrics.AuthContainerView"))
        assertTrue(BiometricSignatures.matchesPromptViewId("com.android.systemui:id/udfps_view"))
        assertTrue(BiometricSignatures.matchesPromptText("Touch the fingerprint sensor"))
        assertTrue(BiometricSignatures.matchesPromptText("Confirm your fingerprint to continue"))
        assertTrue(BiometricSignatures.matchesMismatchText("Not recognized. Try again."))
        assertTrue(BiometricSignatures.matchesLockoutOrPinOnlyText("Too many attempts. Try again in 30 seconds."))
    }
}

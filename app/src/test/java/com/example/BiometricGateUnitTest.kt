package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricGateUnitTest {

    @Test
    fun `unlocked screen with no active biometric prompt blocks touch and records reason`() {
        val tracker = BiometricRequestTracker()
        assertFalse(tracker.onFingerDown(isLockOrAodNow = false, nowMs = 10_000L))
        assertFalse(tracker.visible)
        assertEquals("Blocked: unlocked, no biometric request", tracker.lastDecisionReason)
        tracker.onFingerUp()
    }

    @Test
    fun `lock screen shows on touch and latches terminal on mismatch until finger lifts`() {
        val tracker = BiometricRequestTracker()

        // Finger down on lock screen -> visible = true
        assertTrue(tracker.onFingerDown(isLockOrAodNow = true, nowMs = 20_000L))
        assertTrue(tracker.visible)
        assertEquals("Show: lock screen finger down", tracker.lastDecisionReason)

        // Mismatch error while finger is down -> latches terminal and hides immediately
        assertTrue(tracker.onMismatchError(source = "test_mismatch", nowMs = 20_400L))
        assertTrue(tracker.latchedTerminal)
        assertFalse(tracker.visible)

        // Keeping the same finger down must NOT re-show the overlay
        assertFalse(tracker.onFingerDown(isLockOrAodNow = true, nowMs = 20_500L))
        assertFalse(tracker.visible)

        // Lifting finger clears latchedTerminal
        assertFalse(tracker.onFingerUp())
        assertFalse(tracker.latchedTerminal)

        // Touching again while on lock screen shows overlay again
        assertTrue(tracker.onFingerDown(isLockOrAodNow = true, nowMs = 21_000L))
        assertTrue(tracker.visible)
    }

    @Test
    fun `stray error when finger is not down and older than 1500ms is ignored and does not block next touch`() {
        val tracker = BiometricRequestTracker()
        // Stray error with no recent touch -> ignored
        assertFalse(tracker.onMismatchError(source = "stray_log", nowMs = 50_000L))
        assertFalse(tracker.latchedTerminal)

        // Next real touch on lock screen succeeds immediately
        assertTrue(tracker.onFingerDown(isLockOrAodNow = true, nowMs = 55_000L))
        assertTrue(tracker.visible)
        tracker.onFingerUp()

        // Stray error 2,000 ms after last touch-down (> 1,500 ms window) -> ignored
        assertFalse(tracker.onMismatchError(source = "late_stray", nowMs = 57_001L))
        assertFalse(tracker.latchedTerminal)
        assertTrue(tracker.onFingerDown(isLockOrAodNow = true, nowMs = 58_000L))
        assertTrue(tracker.visible)
    }

    @Test
    fun `hard max duration hides overlay even with finger down and latches until finger lifts`() {
        val tracker = BiometricRequestTracker()
        assertEquals(5_000L, BiometricSignatures.HARD_MAX_OVERLAY_DURATION_MS)

        assertTrue(tracker.onFingerDown(isLockOrAodNow = true, nowMs = 10_000L))
        assertTrue(tracker.visible)

        // 5s hard max duration fires while finger is still down
        assertFalse(tracker.onHardMaxDurationReached())
        assertTrue(tracker.latchedTerminal)
        assertFalse(tracker.visible)

        // Finger still held down -> stays hidden
        assertFalse(tracker.onFingerDown(isLockOrAodNow = true, nowMs = 15_100L))
        assertFalse(tracker.visible)

        // Lift finger -> clears latch
        tracker.onFingerUp()
        assertFalse(tracker.latchedTerminal)
    }

    @Test
    fun `unlocked screen with active BiometricPrompt shows on touch and hides when prompt ends even if finger is down`() {
        val tracker = BiometricRequestTracker()
        tracker.onPromptStarted(source = "AuthContainerView", windowId = 42, isLockOrAodNow = false)
        assertTrue(tracker.isBiometricPromptActive)
        assertTrue(tracker.requestActive)

        assertTrue(tracker.onFingerDown(isLockOrAodNow = false, nowMs = 30_000L))
        assertTrue(tracker.visible)

        // Prompt ends while finger is still down -> hides immediately and latches until finger lifts
        assertFalse(tracker.onPromptStopped("prompt_dismissed"))
        assertFalse(tracker.isBiometricPromptActive)
        assertFalse(tracker.visible)
        assertTrue(tracker.latchedTerminal)
        tracker.onFingerUp()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `standalone sentinel flags stay non-touchable when idle and watch outside touch when requestActive`() {
        val idleFlags = FpAccessibilityService.buildSentinelFlags(watchOutsideTouch = false)
        assertTrue(
            "Idle sentinel must include FLAG_NOT_TOUCHABLE",
            (idleFlags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0
        )

        val activeFlags = FpAccessibilityService.buildSentinelFlags(watchOutsideTouch = true)
        assertTrue(
            "Active sentinel must include FLAG_NOT_TOUCH_MODAL and FLAG_WATCH_OUTSIDE_TOUCH",
            (activeFlags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) != 0 &&
                (activeFlags and android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH) != 0
        )
        assertEquals(
            "Active sentinel must not include FLAG_NOT_TOUCHABLE",
            0,
            activeFlags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }
}


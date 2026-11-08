package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name is FP Animator`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("FP Animator", context.getString(R.string.app_name))
    }

    @Test
    fun `Prefs loads itel S25 defaults and persists custom calibration and customization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val def = Prefs.load(context)
        assertEquals(0.5f, def.cx, 0.001f)
        assertEquals(0.82f, def.cy, 0.001f)
        assertEquals(0.28f, def.size, 0.001f)
        assertEquals(1.0f, def.speed, 0.001f)
        assertEquals(0.10f, def.sensorSize, 0.001f)

        Prefs.save(context, Calib(cx = 0.51f, cy = 0.84f, size = 0.32f, speed = 1.5f, sensorSize = 0.12f))
        val saved = Prefs.load(context)
        assertEquals(0.51f, saved.cx, 0.001f)
        assertEquals(0.84f, saved.cy, 0.001f)
        assertEquals(0.32f, saved.size, 0.001f)
        assertEquals(1.5f, saved.speed, 0.001f)
        assertEquals(0.12f, saved.sensorSize, 0.001f)
        assertTrue(Prefs.isCalibDone(context))

        Prefs.saveCustomization(context, size = 1.0f, speed = 8.0f, sensorSize = 0.15f)
        val customized = Prefs.load(context)
        assertEquals(1.0f, customized.size, 0.001f)
        assertEquals(8.0f, customized.speed, 0.001f)
        assertEquals(0.15f, customized.sensorSize, 0.001f)

        val bounds = Prefs.getRealDisplayBounds(context)
        assertTrue(bounds.width() > 0)
        assertTrue(bounds.height() > 0)
    }

    @Test
    fun `default_ring json parses cleanly in LottieCompositionFactory`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val input = context.resources.openRawResource(R.raw.default_ring)
        val result = LottieCompositionFactory.fromJsonInputStreamSync(input, "default_ring")
        assertNotNull(result.value)
    }

    @Test
    fun `ProVideoFolder has all 98 unique uploaded videos and real PDF HUD designs in assets pro_videos folder with zero duplicates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val items = ProVideoFolder.items
        assertEquals(98, items.size)
        assertEquals("Every ProVideoItem id must be unique", items.size, items.map { it.id }.toSet().size)
        assertEquals("Every ProVideoItem title must be unique", items.size, items.map { it.title }.toSet().size)
        assertEquals("Every ProVideoItem atlasAssetPath must be unique", items.size, items.map { it.atlasAssetPath }.toSet().size)
        for (item in items) {
            assertTrue("Video/design asset ${item.assetPath} must exist in assets/pro_videos/", ProVideoFolder.assetExists(context, item.assetPath))
            assertTrue("Transparent WebP ${item.atlasAssetPath} must exist in assets/pro_videos/", ProVideoFolder.assetExists(context, item.atlasAssetPath))
            val decodedAtlas = ProVideoFrameCache.getOrDecodeAtlas(context, item.assetPath, preferCompact = false)
            assertNotNull("Decoded transparent WebP must not be null for ${item.id}", decodedAtlas)
            val cornerAlpha = android.graphics.Color.alpha(decodedAtlas!!.getPixel(0, 0))
            assertEquals("Corner background pixel must be 100% transparent (alpha = 0) for ${item.id}", 0, cornerAlpha)
            assertTrue("Decoded HD bitmap width must be positive for ${item.id}", decodedAtlas.width >= 100 && decodedAtlas.height >= 100)
        }

        // Verify lazy concentric ring slicing works for single-frame designs
        val sampleDesign = items.first { !it.isAtlasGrid }
        val sampleLayers = ProVideoFrameCache.getOrBuildDesignLayers(context, sampleDesign.assetPath)
        assertNotNull("Concentric multi-ring layers must be generated on demand for ${sampleDesign.id}", sampleLayers)

        val firstVideo = items.first()
        Prefs.setSelectedVideoAsset(context, firstVideo.assetPath)
        assertEquals(firstVideo.assetPath, Prefs.getSelectedVideoAsset(context))

        val hostView = OverlayAnimHostView(context)
        hostView.bindActiveAnimation(context, speed = 1.0f, targetSizePx = 200, autoPlay = false)
        assertTrue("OverlayAnimHostView should be in video mode for ProVideoFolder asset", hostView.isVideoMode)

        // Clearing video asset defaults back to the first Pro Folder animation
        Prefs.setSelectedVideoAsset(context, null)
        assertEquals(firstVideo.assetPath, Prefs.getSelectedVideoAsset(context))
    }

    @Test
    fun `AnimationCatalog contains only the 98 unique Pro Folder animations and all fallback JSONs parse cleanly`() {
        val proVideos = AnimationCatalog.proVideoDesigns
        assertEquals(98, proVideos.size)
        for (pro in proVideos) {
            val res = LottieCompositionFactory.fromJsonStringSync(pro.lottieJson, pro.id)
            assertNotNull("Failed to parse Pro Video Lottie JSON for ${pro.id}", res.value)
        }

        val categories = AnimationCatalog.categories
        assertEquals(1, categories.size)
        assertEquals("pro_video_folder", categories.first().id)
        assertEquals(98, AnimationCatalog.totalDesignCount)
    }

    @Test
    fun `LottiePrewarmCache synchronously caches composition, reuses warm LottieDrawable, and binds to view`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val proDesign = AnimationCatalog.proVideoDesigns.first()
        File(context.filesDir, "custom.json").writeText(proDesign.lottieJson)

        LottiePrewarmCache.updateActiveAnimationAndPrewarm(context, proDesign.lottieJson)
        val activeKey = LottiePrewarmCache.resolveActiveKey(context)
        assertTrue(LottiePrewarmCache.isCompositionCached(activeKey))

        val d1 = LottiePrewarmCache.getOrCreateWarmOverlayDrawable(context, speed = 1.25f, targetSizePx = 200)
        val d2 = LottiePrewarmCache.getOrCreateWarmOverlayDrawable(context, speed = 1.5f, targetSizePx = 200)
        assertSame("Shared pre-warmed LottieDrawable must be reused for the same active composition", d1, d2)
        assertNotNull(d2.composition)
        assertEquals(1.5f, d2.speed, 0.001f)
        assertEquals(0, d2.frame)

        val lottieView = LottieAnimationView(android.view.ContextThemeWrapper(context, R.style.Theme_MyApplication))
        AnimLoader.load(context, lottieView, speed = 1.5f, targetSizePx = 200)
        assertSame("View composition should match pre-warmed composition immediately", d2.composition, lottieView.composition)
        assertEquals(0, lottieView.frame)
    }

    @Test
    fun `AI Custom Lottie synthesizer and MyCollectionStore persist and parse cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = AnimationCatalog.buildAiCustomLottieJson(
            title = "Quantum Dragon Crown",
            primaryArchetypeName = "ROYAL_CROWN",
            secondaryArchetypeName = "FIRE_CORONA",
            primaryHex = AnimationCatalog.parseHexColor("#FFD700"),
            secondaryHex = AnimationCatalog.parseHexColor("#FF5500"),
            accentHex = AnimationCatalog.parseHexColor("#00E5FF"),
            strokeWidth = 6,
            starPoints = 16,
            dashLen = 14,
            gapLen = 8,
            clockwise = true
        )
        val parsed = LottieCompositionFactory.fromJsonStringSync(json, "ai_test")
        assertNotNull(parsed.value)

        val item = SavedAiAnimation(
            id = "ai_custom_test_1",
            title = "Quantum Dragon Crown",
            prompt = "Golden dragon crown with solar sparks",
            styleBadge = "AI Custom • Crown",
            primaryHex = 0xFFFFD700L,
            secondaryHex = 0xFFFF5500L,
            createdAtMs = 1000L,
            lottieJson = json
        )
        val afterSave = MyCollectionStore.saveNew(context, item)
        assertEquals(1, afterSave.size)
        assertEquals("Quantum Dragon Crown", MyCollectionStore.loadAll(context).first().title)

        val afterDelete = MyCollectionStore.delete(context, "ai_custom_test_1")
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `ImageReferenceAnalyzer extracts colors and radial contour that parses cleanly in Lottie`() {
        val bmp = android.graphics.Bitmap.createBitmap(120, 120, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.BLACK)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(0, 229, 255)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawCircle(60f, 60f, 42f, paint)

        val sig = ImageReferenceAnalyzer.analyzeBitmap(bmp)
        assertEquals(16, sig.contourPoints.size)

        val aiVertices = ImageReferenceAnalyzer.parseVerticesCsv("0:-78,48:-42,76:0,48:42,0:78,-48:42,-76:0,-48:-42")
        assertEquals(8, aiVertices.size)

        val jsonWithContour = AnimationCatalog.buildAiCustomLottieJson(
            title = "Image Guided Neon Crest",
            primaryArchetypeName = "BUTTERFLY_WINGS",
            secondaryArchetypeName = "STAR_BLOOM",
            primaryHex = sig.primaryHex,
            secondaryHex = sig.secondaryHex,
            accentHex = sig.accentHex,
            strokeWidth = 5,
            starPoints = 12,
            dashLen = 12,
            gapLen = 8,
            clockwise = true,
            customContourPoints = sig.contourPoints
        )
        val parsed = LottieCompositionFactory.fromJsonStringSync(jsonWithContour, "ai_contour_test")
        assertNotNull("Custom image contour Lottie JSON must parse cleanly", parsed.value)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `FpAccessibilityService overlay LayoutParams include FLAG_SHOW_WHEN_LOCKED and FLAG_ALLOW_LOCK_WHILE_SCREEN_ON for AOD`() {
        val idleLp = FpAccessibilityService.buildOverlayLayoutParams(
            widthPx = 300,
            heightPx = 300,
            xPx = 390,
            yPx = 1800,
            activePlayback = false
        )
        assertEquals(android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, idleLp.type)
        assertTrue(
            "Overlay flags must include FLAG_SHOW_WHEN_LOCKED",
            (idleLp.flags and android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED) != 0
        )
        assertTrue(
            "Overlay flags must include FLAG_ALLOW_LOCK_WHILE_SCREEN_ON",
            (idleLp.flags and android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON) != 0
        )
        assertTrue(
            "Overlay flags must include FLAG_NOT_FOCUSABLE and FLAG_NOT_TOUCHABLE",
            (idleLp.flags and android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0 &&
                (idleLp.flags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0
        )

        val activeFlags = FpAccessibilityService.buildOverlayFlags(activePlayback = true)
        assertTrue(
            "Active playback flags must include FLAG_SHOW_WHEN_LOCKED",
            (activeFlags and android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED) != 0
        )
        assertTrue(
            "Active playback flags must include FLAG_ALLOW_LOCK_WHILE_SCREEN_ON",
            (activeFlags and android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON) != 0
        )
        assertTrue(
            "Active playback flags must include FLAG_KEEP_SCREEN_ON and FLAG_TURN_SCREEN_ON",
            (activeFlags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0 &&
                (activeFlags and android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) != 0
        )
    }

    @Test
    fun `BiometricGateState enforces visible equals fingerDown and requestActive and not latchedTerminal`() {
        val tracker = BiometricRequestTracker()

        // 1. Unlocked screen with no active biometric prompt: touching sensor must NOT show overlay
        assertEquals(false, tracker.onFingerDown(isLockOrAodNow = false, nowMs = 1000L))
        assertEquals(false, tracker.visible)
        tracker.onFingerUp()

        // 2. Lock screen waiting for fingerprint: touching sensor shows overlay; mismatch latches terminal & hides
        assertEquals(true, tracker.onFingerDown(isLockOrAodNow = true, nowMs = 2000L))
        assertEquals(true, tracker.visible)

        // Mismatch occurs while finger stays down -> hides immediately and stays hidden
        assertEquals(true, tracker.onMismatchError("mismatch_error", nowMs = 2200L))
        assertEquals(true, tracker.latchedTerminal)
        assertEquals(false, tracker.visible)
        // Finger still down -> must NOT reappear
        assertEquals(false, tracker.onFingerDown(isLockOrAodNow = true, nowMs = 2300L))

        // Finger lifts -> clears latchedTerminal
        assertEquals(false, tracker.onFingerUp())
        assertEquals(false, tracker.latchedTerminal)

        // Touching again while lock screen request is still active shows animation again
        assertEquals(true, tracker.onFingerDown(isLockOrAodNow = true, nowMs = 3000L))
        assertEquals(true, tracker.visible)
        tracker.onFingerUp()

        // 3. Unlocked screen WITH BiometricPrompt active: shows on touch, hides on success/dismiss
        tracker.onPromptStarted("AuthContainerView", windowId = 7, isLockOrAodNow = false)
        assertEquals(true, tracker.onFingerDown(isLockOrAodNow = false, nowMs = 4000L))
        assertEquals(false, tracker.onPromptStopped("unlock_success"))
        assertEquals(false, tracker.visible)
        tracker.onFingerUp()

        // 4. Verify BiometricSignatures helpers
        assertTrue(BiometricSignatures.isBiometricPackage("com.android.systemui"))
        assertTrue(BiometricSignatures.isBiometricPackage("com.transsion.applock"))
        assertTrue(BiometricSignatures.matchesPromptClass("com.android.systemui.biometrics.AuthContainerView"))
        assertTrue(BiometricSignatures.matchesPromptViewId("com.android.systemui:id/biometric_icon"))
        assertTrue(BiometricSignatures.matchesPromptText("Touch the fingerprint sensor"))
        assertTrue(BiometricSignatures.matchesMismatchText("Fingerprint not recognized. Try again."))
        assertTrue(BiometricSignatures.matchesLockoutOrPinOnlyText("Too many attempts. Try again in 30 seconds."))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `Standalone Shizuku-free engine does not require Shizuku for wizard steps and toggles sentinel flags cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Without Shizuku installed or running, firstIncompleteStep starts at step 3 (Accessibility Service), not 1 or 2
        val step = firstIncompleteStep(context)
        assertTrue("Wizard must skip Shizuku steps 1 & 2 in standalone mode", step >= 3)

        // Idle sentinel flags must include FLAG_NOT_TOUCHABLE (0% touch overhead during normal unlocked phone usage)
        val idleSentinelFlags = FpAccessibilityService.buildSentinelFlags(watchOutsideTouch = false)
        assertTrue(
            "Idle sentinel must have FLAG_NOT_TOUCHABLE",
            (idleSentinelFlags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0
        )

        // Active biometric request sentinel flags must watch outside touch without blocking sensor touches
        val activeSentinelFlags = FpAccessibilityService.buildSentinelFlags(watchOutsideTouch = true)
        assertTrue(
            "Active sentinel must have FLAG_NOT_TOUCH_MODAL and FLAG_WATCH_OUTSIDE_TOUCH",
            (activeSentinelFlags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) != 0 &&
                (activeSentinelFlags and android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH) != 0
        )
        assertEquals(
            "Active sentinel must not have FLAG_NOT_TOUCHABLE so outside touches are observed",
            0,
            activeSentinelFlags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }
}



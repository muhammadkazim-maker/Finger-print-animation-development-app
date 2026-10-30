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
@Config(sdk = [34])
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

        Prefs.save(context, Calib(cx = 0.51f, cy = 0.84f, size = 0.32f, speed = 1.5f))
        val saved = Prefs.load(context)
        assertEquals(0.51f, saved.cx, 0.001f)
        assertEquals(0.84f, saved.cy, 0.001f)
        assertEquals(0.32f, saved.size, 0.001f)
        assertEquals(1.5f, saved.speed, 0.001f)
        assertTrue(Prefs.isCalibDone(context))

        Prefs.saveCustomization(context, size = 0.40f, speed = 2.0f)
        val customized = Prefs.load(context)
        assertEquals(0.40f, customized.size, 0.001f)
        assertEquals(2.0f, customized.speed, 0.001f)
    }

    @Test
    fun `default_ring json parses cleanly in LottieCompositionFactory`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val input = context.resources.openRawResource(R.raw.default_ring)
        val result = LottieCompositionFactory.fromJsonInputStreamSync(input, "default_ring")
        assertNotNull(result.value)
    }

    @Test
    fun `ProVideoFolder has all 13 uploaded mp4 videos and transparent RGBA webp atlases in assets pro_videos folder and OverlayAnimHostView binds them`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val items = ProVideoFolder.items
        assertEquals(13, items.size)
        for (item in items) {
            assertTrue("Video asset ${item.assetPath} must exist in assets/pro_videos/", ProVideoFolder.assetExists(context, item.assetPath))
            assertTrue("Transparent atlas ${item.atlasAssetPath} must exist in assets/pro_videos/", ProVideoFolder.assetExists(context, item.atlasAssetPath))
            val decodedAtlas = ProVideoFrameCache.getOrDecodeAtlas(context, item.assetPath, preferCompact = true)
            assertNotNull("Decoded transparent frame atlas must not be null for ${item.id}", decodedAtlas)
            val cornerAlpha = android.graphics.Color.alpha(decodedAtlas!!.getPixel(0, 0))
            assertEquals("Corner background pixel must be 100% transparent (alpha = 0) for ${item.id}", 0, cornerAlpha)
        }

        val firstVideo = items.first()
        Prefs.setSelectedVideoAsset(context, firstVideo.assetPath)
        assertEquals(firstVideo.assetPath, Prefs.getSelectedVideoAsset(context))

        val hostView = OverlayAnimHostView(context)
        hostView.bindActiveAnimation(context, speed = 1.0f, targetSizePx = 200, autoPlay = false)
        assertTrue("OverlayAnimHostView should be in video mode when a ProVideoFolder asset is selected", hostView.isVideoMode)

        Prefs.setSelectedVideoAsset(context, null)
        hostView.bindActiveAnimation(context, speed = 1.0f, targetSizePx = 200, autoPlay = false)
        assertTrue("OverlayAnimHostView should switch back to Lottie mode when video asset is cleared", !hostView.isVideoMode)
    }

    @Test
    fun `AnimationCatalog has all 13 Pro Video designs, 20 sheets, and all 922 designs parse cleanly`() {
        val proVideos = AnimationCatalog.proVideoDesigns
        assertEquals(13, proVideos.size)
        for (pro in proVideos) {
            val res = LottieCompositionFactory.fromJsonStringSync(pro.lottieJson, pro.id)
            assertNotNull("Failed to parse Pro Video Lottie JSON for ${pro.id}", res.value)
        }

        val categories = AnimationCatalog.categories
        assertEquals(20, categories.size)
        assertEquals(922, AnimationCatalog.totalDesignCount)
        for (cat in categories) {
            assertTrue("Category ${cat.id} should not be empty", cat.designs.isNotEmpty())
            for (design in cat.designs) {
                val res = LottieCompositionFactory.fromJsonStringSync(design.lottieJson, design.id)
                assertNotNull("Failed to parse Lottie JSON for ${design.id} (${design.name})", res.value)
            }
        }
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

        val lottieView = LottieAnimationView(context)
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
}


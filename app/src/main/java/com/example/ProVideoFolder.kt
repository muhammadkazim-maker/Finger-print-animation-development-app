package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.os.SystemClock
import android.util.LruCache
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.RenderMode
import java.util.concurrent.Executors

data class ProVideoItem(
    val id: String,
    val index: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val assetPath: String,
    val atlasAssetPath: String,
    val accentHex: Long,
    val fallbackLottieDesignId: String
)

object ProVideoFolder {
    const val FOLDER_PATH = "pro_videos"
    const val FOLDER_TITLE = "Pro Video Animations Folder"

    val items: List<ProVideoItem> = listOf(
        ProVideoItem(
            id = "folder_video_1_chrono_reactor",
            index = 1,
            title = "Chrono Telemetry Reactor HUD",
            subtitle = "Crimson inner reactor teeth, white vernier scale, cobalt tachometer ticks & live counter",
            badge = "Original Video #1",
            assetPath = "pro_videos/video_1_chrono_reactor.mp4",
            atlasAssetPath = "pro_videos/video_1_chrono_reactor.webp",
            accentHex = 0xFF0066FFL,
            fallbackLottieDesignId = "pro_video_1_chrono_reactor"
        ),
        ProVideoItem(
            id = "folder_video_2_singularity_vortex",
            index = 2,
            title = "Singularity Vortex & Supernova",
            subtitle = "Incandescent white event-horizon ring with violet spiral vortex & cyan-emerald radial burst",
            badge = "Original Video #2",
            assetPath = "pro_videos/video_2_singularity_vortex.mp4",
            atlasAssetPath = "pro_videos/video_2_singularity_vortex.webp",
            accentHex = 0xFF00F5D4L,
            fallbackLottieDesignId = "pro_video_2_singularity_supernova"
        ),
        ProVideoItem(
            id = "folder_video_3_quantum_sphere",
            index = 3,
            title = "Bioluminescent 3D Particle Sphere",
            subtitle = "Rotating 3D cyan stipple particle sphere with crescent rim & volumetric aqua inner plasma",
            badge = "Original Video #3",
            assetPath = "pro_videos/video_3_quantum_sphere.mp4",
            atlasAssetPath = "pro_videos/video_3_quantum_sphere.webp",
            accentHex = 0xFF00E5FFL,
            fallbackLottieDesignId = "pro_video_3_quantum_particle_sphere"
        ),
        ProVideoItem(
            id = "folder_video_4_atomic_comets",
            index = 4,
            title = "Atomic Electron Comet Orbits",
            subtitle = "3D-tilted elliptical orbits with blazing white-hot sapphire comet heads & tapering light tails",
            badge = "Original Video #4",
            assetPath = "pro_videos/video_4_atomic_comets.mp4",
            atlasAssetPath = "pro_videos/video_4_atomic_comets.webp",
            accentHex = 0xFF2979FFL,
            fallbackLottieDesignId = "pro_video_4_atomic_comet_orbits"
        ),
        ProVideoItem(
            id = "folder_video_5_dichroic_orb",
            index = 5,
            title = "Dichroic Liquid Glass Orb",
            subtitle = "3D glossy dark sphere with smooth subsurface cyan, magenta & violet caustics",
            badge = "Original Video #5",
            assetPath = "pro_videos/video_5_dichroic_orb.mp4",
            atlasAssetPath = "pro_videos/video_5_dichroic_orb.webp",
            accentHex = 0xFFE040FBL,
            fallbackLottieDesignId = "pro_video_5_dichroic_liquid_orb"
        ),
        ProVideoItem(
            id = "folder_video_6_plasma_sphere",
            index = 6,
            title = "Volumetric Electric Plasma Sphere",
            subtitle = "Dark 3D orb wrapped in organic, fluid, undulating cyan-white electric plasma waves",
            badge = "Original Video #6",
            assetPath = "pro_videos/video_6_plasma_sphere.mp4",
            atlasAssetPath = "pro_videos/video_6_plasma_sphere.webp",
            accentHex = 0xFF00F0FFL,
            fallbackLottieDesignId = "pro_video_6_plasma_caustic_sphere"
        ),
        ProVideoItem(
            id = "folder_video_7_solar_cobalt_rings",
            index = 7,
            title = "Solar & Cobalt Concentric Rings",
            subtitle = "Exact uploaded video: blazing solar-orange & cobalt-blue concentric light rings on dark core",
            badge = "Uploaded Video #7",
            assetPath = "pro_videos/video_7_solar_cobalt_rings.mp4",
            atlasAssetPath = "pro_videos/video_7_solar_cobalt_rings.webp",
            accentHex = 0xFFFF9100L,
            fallbackLottieDesignId = "pro_video_7_solar_cobalt_rings"
        ),
        ProVideoItem(
            id = "folder_video_8_cyber_speedometer_hud",
            index = 8,
            title = "Cyber Speedometer & Tachometer HUD",
            subtitle = "Exact uploaded HUD dial: 0-240 speedometer with sweeping pink needle, red plasma ring & RPM wings",
            badge = "Animated Image #8",
            assetPath = "pro_videos/video_8_cyber_speedometer_hud.mp4",
            atlasAssetPath = "pro_videos/video_8_cyber_speedometer_hud.webp",
            accentHex = 0xFF00B0FFL,
            fallbackLottieDesignId = "pro_video_8_cyber_speedometer_hud"
        ),
        ProVideoItem(
            id = "folder_video_9_cyber_tachometer_overdrive",
            index = 9,
            title = "Cyber Tachometer Overdrive Dual-Spin",
            subtitle = "Exact uploaded HUD dial with high-speed counter-rotating bezel, turbo red ring & needle sweep",
            badge = "Animated Image #9",
            assetPath = "pro_videos/video_9_cyber_tachometer_overdrive.mp4",
            atlasAssetPath = "pro_videos/video_9_cyber_tachometer_overdrive.webp",
            accentHex = 0xFFFF3D00L,
            fallbackLottieDesignId = "pro_video_9_cyber_tachometer_overdrive"
        ),
        ProVideoItem(
            id = "folder_video_10_chromatic_starlight_rings",
            index = 10,
            title = "Chromatic Starlight Vortex Rings",
            subtitle = "Exact uploaded design: multicolor neon plasma rings with counter-rotating golden starlight core",
            badge = "Animated Image #10",
            assetPath = "pro_videos/video_10_chromatic_starlight_rings.mp4",
            atlasAssetPath = "pro_videos/video_10_chromatic_starlight_rings.webp",
            accentHex = 0xFFFF4081L,
            fallbackLottieDesignId = "pro_video_10_chromatic_starlight_rings"
        ),
        ProVideoItem(
            id = "folder_video_11_neon_magenta_cyan_portal",
            index = 11,
            title = "Neon Magenta & Cyan Cyber Portal",
            subtitle = "Exact uploaded design: glowing hot-pink outer tick ring & electric cyan inner portal ring",
            badge = "Animated Image #11",
            assetPath = "pro_videos/video_11_neon_magenta_cyan_portal.mp4",
            atlasAssetPath = "pro_videos/video_11_neon_magenta_cyan_portal.webp",
            accentHex = 0xFFFF00E5L,
            fallbackLottieDesignId = "pro_video_11_neon_magenta_cyan_portal"
        ),
        ProVideoItem(
            id = "folder_video_12_electric_blue_blade_vortex",
            index = 12,
            title = "Electric Sapphire Blade Vortex",
            subtitle = "Exact uploaded design: spinning sapphire-blue spiral energy blades with white-hot highlights",
            badge = "Animated Image #12",
            assetPath = "pro_videos/video_12_electric_blue_blade_vortex.mp4",
            atlasAssetPath = "pro_videos/video_12_electric_blue_blade_vortex.webp",
            accentHex = 0xFF2979FFL,
            fallbackLottieDesignId = "pro_video_12_electric_blue_blade_vortex"
        ),
        ProVideoItem(
            id = "folder_video_13_cyan_quantum_tech_dial",
            index = 13,
            title = "Cyan Quantum HUD Tech Dial",
            subtitle = "Exact uploaded design: 5 counter-rotating concentric cyan HUD gear & tick rings with glowing core",
            badge = "Animated Image #13",
            assetPath = "pro_videos/video_13_cyan_quantum_tech_dial.mp4",
            atlasAssetPath = "pro_videos/video_13_cyan_quantum_tech_dial.webp",
            accentHex = 0xFF00E5FFL,
            fallbackLottieDesignId = "pro_video_13_cyan_quantum_tech_dial"
        )
    )

    fun atlasPathFor(assetPath: String): String {
        val matched = items.firstOrNull { it.assetPath == assetPath || it.atlasAssetPath == assetPath }
        if (matched != null) return matched.atlasAssetPath
        return if (assetPath.endsWith(".mp4", ignoreCase = true)) {
            assetPath.substring(0, assetPath.length - 4) + ".webp"
        } else {
            assetPath
        }
    }

    fun assetExists(context: Context, assetPath: String): Boolean {
        val checkExist = { path: String ->
            runCatching {
                context.assets.openFd(path).use { it.length > 0L }
            }.getOrElse {
                runCatching {
                    context.assets.open(path).use { true }
                }.getOrDefault(false)
            }
        }
        return checkExist(assetPath) || checkExist(atlasPathFor(assetPath))
    }
}

/**
 * Thread-safe, pre-warmed cache of transparent-background (`ARGB_8888`) WebP video frame atlases
 * (`8x8` grid = 64 frames per video). Keying out the solid `#000000` background into a true alpha
 * channel (`alpha = 0`) eliminates the black background box/circle on the lock screen, AOD, and
 * preview cards, while eliminating `MediaCodec` / `MediaPlayer` decoder stalls and loop glitches.
 */
object ProVideoFrameCache {
    const val ATLAS_COLS = 8
    const val ATLAS_ROWS = 8
    const val TOTAL_FRAMES = ATLAS_COLS * ATLAS_ROWS
    const val BASE_FPS = 24f

    private val decodeExecutor = Executors.newFixedThreadPool(2)
    private val decodeLocks = HashMap<String, Any>()

    private val memoryCache: LruCache<String, Bitmap> = run {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        val cacheSizeKb = (maxKb / 2).coerceAtLeast(48 * 1024)
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return (value.allocationByteCount / 1024).coerceAtLeast(1)
            }
        }
    }

    private fun lockFor(key: String): Any = synchronized(decodeLocks) {
        decodeLocks.getOrPut(key) { Any() }
    }

    fun getCachedAtlas(assetPath: String): Bitmap? {
        val atlasPath = ProVideoFolder.atlasPathFor(assetPath)
        synchronized(memoryCache) {
            val cached = memoryCache.get(atlasPath)
            if (cached != null && !cached.isRecycled) return cached
        }
        return null
    }

    fun getOrDecodeAtlas(context: Context, assetPath: String, preferCompact: Boolean = false): Bitmap? {
        val atlasPath = ProVideoFolder.atlasPathFor(assetPath)
        getCachedAtlas(atlasPath)?.let { return it }

        synchronized(lockFor(atlasPath)) {
            getCachedAtlas(atlasPath)?.let { return it }
            val appCtx = context.applicationContext ?: context
            val decoded = decodeAtlasFromAssets(appCtx, atlasPath, sampleSize = if (preferCompact) 2 else 1)
                ?: decodeAtlasFromAssets(appCtx, atlasPath, sampleSize = 2)
            if (decoded != null) {
                synchronized(memoryCache) {
                    memoryCache.put(atlasPath, decoded)
                }
            }
            return decoded
        }
    }

    private fun decodeAtlasFromAssets(context: Context, atlasPath: String, sampleSize: Int): Bitmap? {
        return try {
            context.assets.open(atlasPath).use { input ->
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = sampleSize.coerceAtLeast(1)
                    inScaled = false
                }
                BitmapFactory.decodeStream(input, null, opts)
            }
        } catch (_: OutOfMemoryError) {
            synchronized(memoryCache) {
                memoryCache.evictAll()
            }
            if (sampleSize < 2) {
                runCatching {
                    context.assets.open(atlasPath).use { input ->
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inSampleSize = 2
                            inScaled = false
                        }
                        BitmapFactory.decodeStream(input, null, opts)
                    }
                }.getOrNull()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun prewarmAsync(context: Context, specificAssetPath: String? = null, onReady: ((Bitmap?) -> Unit)? = null) {
        val appCtx = context.applicationContext ?: context
        val targetPath = specificAssetPath ?: Prefs.getSelectedVideoAsset(appCtx) ?: ProVideoFolder.items.first().assetPath
        val already = getCachedAtlas(targetPath)
        if (already != null) {
            onReady?.invoke(already)
            return
        }
        decodeExecutor.execute {
            val bmp = getOrDecodeAtlas(appCtx, targetPath, preferCompact = false)
            if (onReady != null) {
                android.os.Handler(Looper.getMainLooper()).post {
                    onReady.invoke(bmp)
                }
            }
        }
    }
}

/**
 * Hardware-accelerated, zero-black-background video animation view that plays the real extracted
 * frames of the bundled Pro Videos (`assets/pro_videos/video_1..6`) with 100% transparent (`alpha = 0`)
 * background, 0ms instant start, zero loop stutter, and smooth `0.25x..3.0x` playback speed.
 */
class LoopingAssetVideoView(context: Context) : View(context), Choreographer.FrameCallback {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val srcRect = Rect()
    private val dstRect = RectF()

    private var loadedAssetPath: String? = null
    private var atlasBitmap: Bitmap? = null
    private var isPlaying = false
    private var desiredSpeed: Float = 1.0f
    private var currentFrameFloat: Float = 0f
    private var lastFrameTimeNanos: Long = 0L
    private var choreographerScheduled = false

    init {
        setWillNotDraw(false)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val d = minOf(view.width, view.height)
                val left = (view.width - d) / 2
                val top = (view.height - d) / 2
                outline.setOval(left, top, left + d, top + d)
            }
        }
        clipToOutline = true
    }

    fun setVideoAsset(assetPath: String, autoStart: Boolean, speed: Float = 1.0f) {
        desiredSpeed = speed.coerceIn(0.25f, 3.0f)
        val pathChanged = loadedAssetPath != assetPath
        loadedAssetPath = assetPath

        if (pathChanged || atlasBitmap == null || atlasBitmap?.isRecycled == true) {
            val cached = ProVideoFrameCache.getCachedAtlas(assetPath)
            if (cached != null) {
                atlasBitmap = cached
                invalidate()
            } else {
                ProVideoFrameCache.prewarmAsync(context, assetPath) { decoded ->
                    if (loadedAssetPath == assetPath && decoded != null) {
                        atlasBitmap = decoded
                        invalidate()
                        if (isPlaying) {
                            scheduleNextFrame()
                        }
                    }
                }
            }
        }

        if (autoStart) {
            startImmediate()
        } else {
            pauseAndReset()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        desiredSpeed = speed.coerceIn(0.25f, 3.0f)
    }

    fun startImmediate() {
        isPlaying = true
        val path = loadedAssetPath
        if ((atlasBitmap == null || atlasBitmap?.isRecycled == true) && path != null) {
            val cached = ProVideoFrameCache.getCachedAtlas(path)
            if (cached != null) {
                atlasBitmap = cached
            } else {
                ProVideoFrameCache.prewarmAsync(context, path) { decoded ->
                    if (loadedAssetPath == path && decoded != null) {
                        atlasBitmap = decoded
                        invalidate()
                    }
                }
            }
        }
        lastFrameTimeNanos = 0L
        scheduleNextFrame()
        invalidate()
    }

    fun pauseAndReset() {
        isPlaying = false
        currentFrameFloat = 0f
        lastFrameTimeNanos = 0L
        unscheduleFrameCallback()
        invalidate()
    }

    fun primeAtFrameZero() {
        currentFrameFloat = 0f
        lastFrameTimeNanos = 0L
        val path = loadedAssetPath
        if ((atlasBitmap == null || atlasBitmap?.isRecycled == true) && path != null) {
            val cached = ProVideoFrameCache.getCachedAtlas(path)
            if (cached != null) {
                atlasBitmap = cached
            } else {
                ProVideoFrameCache.prewarmAsync(context, path) { decoded ->
                    if (loadedAssetPath == path && decoded != null) {
                        atlasBitmap = decoded
                        invalidate()
                    }
                }
            }
        }
        invalidate()
    }

    override fun doFrame(frameTimeNanos: Long) {
        choreographerScheduled = false
        if (!isPlaying || !isAttachedToWindow || visibility != VISIBLE) {
            lastFrameTimeNanos = 0L
            return
        }

        if (lastFrameTimeNanos == 0L) {
            lastFrameTimeNanos = frameTimeNanos
        } else {
            val deltaSec = ((frameTimeNanos - lastFrameTimeNanos).coerceAtLeast(0L)) / 1_000_000_000f
            lastFrameTimeNanos = frameTimeNanos
            // Clamp delta to avoid large jumps after screen wake
            val clampedDelta = deltaSec.coerceAtMost(0.1f)
            val advance = clampedDelta * ProVideoFrameCache.BASE_FPS * desiredSpeed
            currentFrameFloat = (currentFrameFloat + advance) % ProVideoFrameCache.TOTAL_FRAMES
        }

        invalidate()
        scheduleNextFrame()
    }

    private fun scheduleNextFrame() {
        if (!choreographerScheduled && isPlaying && isAttachedToWindow && visibility == VISIBLE) {
            choreographerScheduled = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun unscheduleFrameCallback() {
        if (choreographerScheduled) {
            choreographerScheduled = false
            runCatching { Choreographer.getInstance().removeFrameCallback(this) }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlaying) {
            lastFrameTimeNanos = 0L
            scheduleNextFrame()
        }
    }

    override fun onDetachedFromWindow() {
        unscheduleFrameCallback()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && isPlaying) {
            lastFrameTimeNanos = 0L
            scheduleNextFrame()
        } else if (visibility != VISIBLE) {
            unscheduleFrameCallback()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = atlasBitmap ?: loadedAssetPath?.let { ProVideoFrameCache.getCachedAtlas(it) }?.also {
            atlasBitmap = it
        } ?: return

        if (bmp.isRecycled || width <= 0 || height <= 0) return

        val frameIdx = (currentFrameFloat.toInt() % ProVideoFrameCache.TOTAL_FRAMES)
            .coerceIn(0, ProVideoFrameCache.TOTAL_FRAMES - 1)
        val col = frameIdx % ProVideoFrameCache.ATLAS_COLS
        val row = frameIdx / ProVideoFrameCache.ATLAS_COLS

        val cellW = bmp.width / ProVideoFrameCache.ATLAS_COLS
        val cellH = bmp.height / ProVideoFrameCache.ATLAS_ROWS
        if (cellW <= 0 || cellH <= 0) return

        srcRect.set(
            col * cellW,
            row * cellH,
            (col + 1) * cellW,
            (row + 1) * cellH
        )

        val d = minOf(width, height).toFloat()
        val left = (width - d) / 2f
        val top = (height - d) / 2f
        dstRect.set(left, top, left + d, top + d)

        canvas.drawBitmap(bmp, srcRect, dstRect, paint)
    }

    fun releaseAll() {
        pauseAndReset()
        atlasBitmap = null
    }
}

/**
 * Unified overlay & preview container that hosts both the pre-warmed [LottieAnimationView] and the
 * transparent-background [LoopingAssetVideoView] so the accessibility service and preview screens
 * can render either a video from the Pro Videos folder or a Lottie animation seamlessly.
 */
class OverlayAnimHostView(context: Context) : FrameLayout(context) {

    val lottieView: LottieAnimationView = LottieAnimationView(context).apply {
        setRenderMode(RenderMode.HARDWARE)
        setCacheComposition(true)
        setIgnoreDisabledSystemAnimations(true)
        enableMergePathsForKitKatAndAbove(true)
        repeatCount = LottieDrawable.INFINITE
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)
    }

    val videoView: LoopingAssetVideoView = LoopingAssetVideoView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)
        visibility = View.GONE
    }

    var isVideoMode: Boolean = false
        private set

    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        addView(lottieView)
        addView(videoView)
    }

    fun bindActiveAnimation(
        context: Context,
        speed: Float = Prefs.load(context).speed,
        targetSizePx: Int = 256,
        autoPlay: Boolean = false
    ) {
        val videoAsset = Prefs.getSelectedVideoAsset(context)
        if (!videoAsset.isNullOrBlank() && ProVideoFolder.assetExists(context, videoAsset)) {
            isVideoMode = true
            lottieView.pauseAnimation()
            lottieView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            videoView.setVideoAsset(videoAsset, autoStart = autoPlay, speed = speed)
        } else {
            isVideoMode = false
            videoView.pauseAndReset()
            videoView.visibility = View.GONE
            lottieView.visibility = View.VISIBLE
            AnimLoader.load(context, lottieView, speed = speed, targetSizePx = targetSizePx)
            if (autoPlay) {
                lottieView.playAnimation()
            } else {
                lottieView.pauseAnimation()
                lottieView.frame = 0
            }
        }
    }

    fun setSpeed(speed: Float) {
        if (isVideoMode) {
            videoView.setPlaybackSpeed(speed)
        } else {
            lottieView.speed = speed
        }
    }

    fun playImmediate() {
        if (isVideoMode) {
            if (videoView.visibility != View.VISIBLE) videoView.visibility = View.VISIBLE
            videoView.startImmediate()
        } else {
            if (lottieView.visibility != View.VISIBLE) lottieView.visibility = View.VISIBLE
            if (!lottieView.isAnimating) {
                lottieView.playAnimation()
            }
        }
        invalidate()
    }

    fun pauseAndResetToFrameZero() {
        if (isVideoMode) {
            videoView.pauseAndReset()
        } else {
            lottieView.pauseAnimation()
            lottieView.frame = 0
        }
        invalidate()
    }

    fun primeForImmediateStart() {
        if (isVideoMode) {
            videoView.primeAtFrameZero()
        } else {
            LottiePrewarmCache.primeViewForImmediateStart(lottieView)
        }
    }
}

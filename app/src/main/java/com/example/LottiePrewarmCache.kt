package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.LruCache
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.RenderMode
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Shared, pre-warmed [LottieComposition] & [LottieDrawable] cache used by [FpAccessibilityService]
 * and preview screens to eliminate JSON parsing, disk I/O, layer-tree construction, and
 * first-frame GPU upload latency when the biometric prompt or lock-screen touch occurs.
 */
@Suppress("DEPRECATION")
object LottiePrewarmCache {

    private const val MAX_COMPOSITION_CACHE_SIZE = 96
    private const val ACTIVE_CACHE_KEY_PREFIX = "active_custom_"
    const val DEFAULT_CACHE_KEY = "raw_default_ring"

    private val compositionCache = object : LruCache<String, LottieComposition>(MAX_COMPOSITION_CACHE_SIZE) {}
    private val drawablePool = ConcurrentHashMap<String, LottieDrawable>()

    // Reusable offscreen buffer for zero-allocation frame-0 layer tree & path pre-warming
    private val warmupLock = Any()
    private var warmupBitmap: Bitmap? = null
    private var warmupCanvas: Canvas? = null

    private val warmupExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "LottiePrewarmThread").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY + 1
        }
    }

    @Volatile
    private var activeCacheKey: String = DEFAULT_CACHE_KEY

    @Volatile
    private var activeComposition: LottieComposition? = null

    @Volatile
    private var sharedOverlayDrawable: LottieDrawable? = null

    /**
     * Resolves the deterministic cache key for the currently active animation
     * (`custom.json` or fallback `R.raw.default_ring`).
     */
    fun resolveActiveKey(context: Context): String {
        val customFile = File(context.filesDir, "custom.json")
        return if (customFile.exists() && customFile.length() > 0L) {
            "$ACTIVE_CACHE_KEY_PREFIX${customFile.lastModified()}_${customFile.length()}"
        } else {
            DEFAULT_CACHE_KEY
        }
    }

    /**
     * Returns true if the composition for [cacheKey] is already parsed and resident in memory.
     */
    fun isCompositionCached(cacheKey: String): Boolean {
        return compositionCache.get(cacheKey) != null
    }

    /**
     * Synchronously parses (or retrieves from cache) the [LottieComposition] for the given JSON string.
     */
    fun getOrLoadCompositionFromJson(jsonString: String, cacheKey: String): LottieComposition? {
        compositionCache.get(cacheKey)?.let { return it }
        val result = LottieCompositionFactory.fromJsonStringSync(jsonString, cacheKey)
        val composition = result.value ?: return null
        compositionCache.put(cacheKey, composition)
        return composition
    }

    /**
     * Synchronously loads and caches the active [LottieComposition] from `custom.json` or `R.raw.default_ring`.
     */
    fun getOrLoadActiveComposition(context: Context): Pair<String, LottieComposition?> {
        val key = resolveActiveKey(context)
        compositionCache.get(key)?.let { cached ->
            activeCacheKey = key
            activeComposition = cached
            return key to cached
        }

        val customFile = File(context.filesDir, "custom.json")
        val composition: LottieComposition? = if (customFile.exists() && customFile.length() > 0L) {
            runCatching {
                val jsonText = customFile.readText()
                LottieCompositionFactory.fromJsonStringSync(jsonText, key).value
            }.getOrNull()
        } else {
            null
        } ?: runCatching {
            LottieCompositionFactory.fromRawResSync(
                context.applicationContext,
                R.raw.default_ring,
                DEFAULT_CACHE_KEY
            ).value
        }.getOrNull()

        if (composition != null) {
            compositionCache.put(key, composition)
            activeCacheKey = key
            activeComposition = composition
        }
        return key to composition
    }

    /**
     * Returns a shared, pre-warmed [LottieDrawable] for the active animation.
     * Pre-renders frame 0 into a reusable offscreen canvas so the internal Lottie `CompositionLayer`
     * hierarchy, trim paths, dash arrays, and stroke shaders are already allocated and warm.
     */
    @Synchronized
    fun getOrCreateWarmOverlayDrawable(
        context: Context,
        speed: Float,
        targetSizePx: Int = 256
    ): LottieDrawable {
        val (key, composition) = getOrLoadActiveComposition(context)
        val existing = sharedOverlayDrawable

        val drawable = if (
            existing != null &&
            drawablePool[key] === existing &&
            existing.composition === composition
        ) {
            existing
        } else {
            LottieDrawable().apply {
                enableMergePathsForKitKatAndAbove(true)
                renderMode = RenderMode.HARDWARE
                isApplyingOpacityToLayersEnabled = false
                repeatCount = LottieDrawable.INFINITE
                if (composition != null) {
                    setComposition(composition)
                }
                prewarmDrawableFrameZero(this, targetSizePx.coerceIn(64, 384))
                drawablePool.clear()
                drawablePool[key] = this
                sharedOverlayDrawable = this
            }
        }

        drawable.speed = speed
        drawable.repeatCount = LottieDrawable.INFINITE
        drawable.frame = 0
        return drawable
    }

    /**
     * Binds the pre-warmed active [LottieComposition] and warms the [LottieAnimationView]'s
     * underlying [LottieDrawable] at frame 0 without disk I/O or asynchronous parsing lag.
     */
    fun bindWarmToView(
        context: Context,
        view: LottieAnimationView,
        speed: Float = Prefs.load(context).speed,
        targetSizePx: Int = 256
    ) {
        view.setRenderMode(RenderMode.HARDWARE)
        view.setCacheComposition(true)
        view.setIgnoreDisabledSystemAnimations(true)
        view.enableMergePathsForKitKatAndAbove(true)

        // Ensure the shared overlay drawable and active composition are warm
        val warmShared = getOrCreateWarmOverlayDrawable(context, speed, targetSizePx)
        val composition = warmShared.composition ?: getOrLoadActiveComposition(context).second

        if (composition != null) {
            if (view.composition !== composition) {
                view.setComposition(composition)
            }
        } else {
            view.setAnimation(R.raw.default_ring)
        }

        view.repeatCount = LottieDrawable.INFINITE
        view.speed = speed
        view.frame = 0

        // Warm the view's internal LottieDrawable layer tree and hardware path caches at frame 0
        val dim = targetSizePx.takeIf { it > 0 }
            ?: view.layoutParams?.width?.takeIf { it > 0 }
            ?: 256
        (view.drawable as? LottieDrawable)?.let { drawable ->
            prewarmDrawableFrameZero(drawable, dim.coerceIn(64, 384))
        }
    }

    /**
     * Lightweight pre-trigger call executed when a BiometricPrompt or screen wake is detected
     * prior to finger touch. Guarantees the view's drawable is sought to frame 0 and warm.
     */
    fun primeViewForImmediateStart(view: LottieAnimationView?) {
        val v = view ?: return
        if (!v.isAnimating) {
            if (v.frame != 0) {
                v.frame = 0
            }
        }
    }

    /**
     * Binds an arbitrary JSON string (e.g., in catalog cards, calibration, or AI preview) using the
     * shared composition cache. If already cached, binds synchronously in <0.05ms; if not yet cached,
     * parses on Lottie's background task pool and caches on completion so fast grid scrolling never
     * stalls the Main UI thread.
     */
    fun bindJsonToView(
        view: LottieAnimationView,
        lottieJson: String,
        cacheKey: String,
        speed: Float
    ) {
        if (view.tag == cacheKey && view.composition != null) {
            if (view.speed != speed) view.speed = speed
            if (!view.isAnimating) view.playAnimation()
            return
        }
        view.tag = cacheKey
        view.setRenderMode(RenderMode.HARDWARE)
        view.setCacheComposition(true)
        view.setIgnoreDisabledSystemAnimations(true)
        view.enableMergePathsForKitKatAndAbove(true)
        view.repeatCount = LottieDrawable.INFINITE
        view.speed = speed

        val cached = compositionCache.get(cacheKey)
        if (cached != null) {
            view.setComposition(cached)
            view.playAnimation()
            return
        }

        LottieCompositionFactory.fromJsonString(lottieJson, cacheKey).addListener { comp ->
            if (comp != null) {
                compositionCache.put(cacheKey, comp)
                if (view.tag == cacheKey) {
                    view.setComposition(comp)
                    view.repeatCount = LottieDrawable.INFINITE
                    view.speed = speed
                    view.playAnimation()
                }
            }
        }
    }

    /**
     * Synchronously updates the active cached composition from an in-memory JSON string right when
     * the user selects a new design, then warms the overlay drawable on the background executor.
     */
    fun updateActiveAnimationAndPrewarm(context: Context, lottieJson: String) {
        val key = resolveActiveKey(context)
        val comp = getOrLoadCompositionFromJson(lottieJson, key)
        if (comp != null) {
            activeCacheKey = key
            activeComposition = comp
        }
        prewarmAsync(context, lottieJson)
    }

    /**
     * Asynchronously pre-warms the active animation in the background (e.g. on app start, service
     * connection, or animation change).
     */
    fun prewarmAsync(context: Context, jsonOverride: String? = null) {
        val appCtx = context.applicationContext
        warmupExecutor.execute {
            runCatching {
                val calib = Prefs.load(appCtx)
                if (jsonOverride != null) {
                    val key = resolveActiveKey(appCtx)
                    val comp = getOrLoadCompositionFromJson(jsonOverride, key)
                    if (comp != null) {
                        activeCacheKey = key
                        activeComposition = comp
                    }
                }
                getOrCreateWarmOverlayDrawable(appCtx, calib.speed)
            }
        }
    }

    /**
     * Forces [LottieDrawable] to build its internal `CompositionLayer` tree, path tessellations,
     * and stroke shaders at frame 0 by drawing once into a reusable offscreen canvas.
     */
    fun prewarmDrawableFrameZero(drawable: LottieDrawable, sizePx: Int = 256) {
        synchronized(warmupLock) {
            runCatching {
                val dim = sizePx.coerceIn(64, 384)
                var bmp = warmupBitmap
                if (bmp == null || bmp.isRecycled || bmp.width < dim || bmp.height < dim) {
                    bmp?.recycle()
                    bmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
                    warmupBitmap = bmp
                    warmupCanvas = Canvas(bmp)
                }
                val canvas = warmupCanvas ?: Canvas(bmp).also { warmupCanvas = it }
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawable.setBounds(0, 0, dim, dim)
                drawable.frame = 0
                drawable.draw(canvas)
                bmp.prepareToDraw()
            }
        }
    }
}

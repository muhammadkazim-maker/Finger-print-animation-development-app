package com.example

import android.content.Context
import android.util.LruCache
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.RenderMode
import java.io.File
import java.util.concurrent.Executors

/**
 * Lightweight pre-warm cache for active animations.
 * Prioritizes pinning the active Pro Folder HD WebP atlas (`ProVideoFrameCache`)
 * while maintaining warm Lottie fallback support with zero background CPU stalls.
 */
object LottiePrewarmCache {

    private const val ACTIVE_CACHE_KEY = "active_custom_fp_anim"
    private val memoryCache = object : LruCache<String, LottieComposition>(12) {}

    @Volatile private var warmOverlayDrawable: LottieDrawable? = null
    @Volatile private var warmOverlayKey: String? = null

    private val bgExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ProAnimPrewarm").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }

    fun resolveActiveKey(context: Context): String {
        val customFile = File(context.filesDir, "custom.json")
        return if (customFile.exists() && customFile.length() > 10L) {
            "${ACTIVE_CACHE_KEY}_${customFile.lastModified()}_${customFile.length()}"
        } else {
            "default_ring_asset"
        }
    }

    fun isCompositionCached(key: String): Boolean {
        return synchronized(memoryCache) { memoryCache.get(key) != null }
    }

    fun getComposition(key: String): LottieComposition? =
        synchronized(memoryCache) { memoryCache.get(key) }

    fun putComposition(key: String, composition: LottieComposition) {
        synchronized(memoryCache) { memoryCache.put(key, composition) }
    }

    fun updateActiveAnimationAndPrewarm(context: Context, lottieJson: String) {
        runCatching {
            val customFile = File(context.filesDir, "custom.json")
            customFile.writeText(lottieJson)
            val key = resolveActiveKey(context)
            val res = LottieCompositionFactory.fromJsonStringSync(lottieJson, key)
            res.value?.let { comp ->
                putComposition(key, comp)
                val d = warmOverlayDrawable ?: LottieDrawable().also { warmOverlayDrawable = it }
                d.composition = comp
                d.repeatCount = LottieDrawable.INFINITE
                warmOverlayKey = key
            }
        }
        val videoAsset = Prefs.getSelectedVideoAsset(context)
        ProVideoFrameCache.pinActiveOverlayAtlas(context, videoAsset)
    }

    fun getOrCreateWarmOverlayDrawable(
        context: Context,
        speed: Float = 1.0f,
        @Suppress("UNUSED_PARAMETER") targetSizePx: Int = 240
    ): LottieDrawable {
        val key = resolveActiveKey(context)
        val comp = getOrLoadSync(context, key)
        val existing = warmOverlayDrawable
        if (existing != null && warmOverlayKey == key && existing.composition === comp) {
            existing.speed = speed
            existing.repeatCount = LottieDrawable.INFINITE
            return existing
        }
        val drawable = existing ?: LottieDrawable().also { warmOverlayDrawable = it }
        if (comp != null) {
            drawable.composition = comp
        }
        drawable.speed = speed
        drawable.repeatCount = LottieDrawable.INFINITE
        warmOverlayKey = key
        return drawable
    }

    private fun getOrLoadSync(context: Context, key: String): LottieComposition? {
        getComposition(key)?.let { return it }
        val customFile = File(context.filesDir, "custom.json")
        val res = if (customFile.exists() && customFile.length() > 10L) {
            LottieCompositionFactory.fromJsonStringSync(customFile.readText(), key)
        } else {
            LottieCompositionFactory.fromAssetSync(context, "default_ring.json", key)
        }
        return res.value?.also { putComposition(key, it) }
    }

    @Suppress("DEPRECATION")
    fun bindWarmToView(
        context: Context,
        view: LottieAnimationView,
        speed: Float = 1.0f,
        @Suppress("UNUSED_PARAMETER") targetSizePx: Int = 240
    ) {
        view.setRenderMode(RenderMode.HARDWARE)
        view.setCacheComposition(true)
        view.setIgnoreDisabledSystemAnimations(true)
        val key = resolveActiveKey(context)
        val comp = getOrLoadSync(context, key)
        if (comp != null) {
            view.setComposition(comp)
        }
        view.repeatCount = LottieDrawable.INFINITE
        view.speed = speed
    }

    @Suppress("DEPRECATION")
    fun bindJsonToView(
        view: LottieAnimationView,
        lottieJson: String,
        cacheKey: String,
        speed: Float = 1.0f
    ) {
        view.setRenderMode(RenderMode.HARDWARE)
        view.setCacheComposition(true)
        view.setIgnoreDisabledSystemAnimations(true)
        val cached = getComposition(cacheKey)
        if (cached != null) {
            if (view.composition !== cached) {
                view.setComposition(cached)
            }
            view.repeatCount = LottieDrawable.INFINITE
            view.speed = speed
            if (!view.isAnimating) view.playAnimation()
            return
        }
        val res = LottieCompositionFactory.fromJsonStringSync(lottieJson, cacheKey)
        res.value?.let { comp ->
            putComposition(cacheKey, comp)
            view.setComposition(comp)
            view.repeatCount = LottieDrawable.INFINITE
            view.speed = speed
            if (!view.isAnimating) view.playAnimation()
        }
    }

    fun prewarmAsync(context: Context, jsonOverride: String? = null) {
        val appCtx = context.applicationContext ?: context
        bgExecutor.execute {
            try {
                val activeVideo = Prefs.getSelectedVideoAsset(appCtx)
                ProVideoFrameCache.pinActiveOverlayAtlas(appCtx, activeVideo)
                if (!jsonOverride.isNullOrBlank()) {
                    val key = "override_${jsonOverride.hashCode()}"
                    if (!isCompositionCached(key)) {
                        LottieCompositionFactory.fromJsonStringSync(jsonOverride, key).value?.let {
                            putComposition(key, it)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    fun primeViewForImmediateStart(view: LottieAnimationView) {
        try {
            view.frame = 0
            view.invalidate()
        } catch (_: Throwable) {}
    }
}

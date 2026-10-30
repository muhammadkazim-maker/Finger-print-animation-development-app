package com.example

import android.content.Context
import com.airbnb.lottie.LottieAnimationView
import java.io.File

object AnimLoader {
    fun load(
        ctx: Context,
        v: LottieAnimationView,
        speed: Float = Prefs.load(ctx).speed,
        targetSizePx: Int = 256
    ) {
        LottiePrewarmCache.bindWarmToView(
            context = ctx,
            view = v,
            speed = speed,
            targetSizePx = targetSizePx
        )
    }

    fun prewarm(ctx: Context, jsonOverride: String? = null) {
        LottiePrewarmCache.prewarmAsync(ctx, jsonOverride)
    }

    fun hasCustomAnimation(ctx: Context): Boolean = File(ctx.filesDir, "custom.json").exists()
}

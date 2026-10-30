package com.example

import android.content.Context

// cx, cy = fractions of display width/height; size = diameter / display width; speed = playback speed multiplier.
// Defaults (0.5, 0.82, 0.28, 1.0) are a starting guess for the itel S25.
data class Calib(
    val cx: Float,
    val cy: Float,
    val size: Float,
    val speed: Float = 1.0f
)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("fp", Context.MODE_PRIVATE)

    fun load(c: Context): Calib = sp(c).let {
        Calib(
            cx = it.getFloat("cx", 0.5f),
            cy = it.getFloat("cy", 0.82f),
            size = it.getFloat("size", 0.28f),
            speed = it.getFloat("speed", 1.0f)
        )
    }

    fun save(c: Context, v: Calib) {
        sp(c).edit()
            .putFloat("cx", v.cx.coerceIn(0.05f, 0.95f))
            .putFloat("cy", v.cy.coerceIn(0.05f, 0.95f))
            .putFloat("size", v.size.coerceIn(0.15f, 0.50f))
            .putFloat("speed", v.speed.coerceIn(0.25f, 3.0f))
            .putBoolean("calib_done", true)
            .apply()
    }

    fun saveCustomization(c: Context, size: Float, speed: Float) {
        sp(c).edit()
            .putFloat("size", size.coerceIn(0.15f, 0.50f))
            .putFloat("speed", speed.coerceIn(0.25f, 3.0f))
            .apply()
    }

    fun isBgDone(c: Context): Boolean = sp(c).getBoolean("bg_done", false)
    fun setBgDone(c: Context, done: Boolean) = sp(c).edit().putBoolean("bg_done", done).apply()

    fun isStockStepDone(c: Context): Boolean = sp(c).getBoolean("stock_done", false)
    fun setStockStepDone(c: Context, done: Boolean) = sp(c).edit().putBoolean("stock_done", done).apply()

    fun isCalibDone(c: Context): Boolean = sp(c).getBoolean("calib_done", false)
    fun setCalibDone(c: Context, done: Boolean) = sp(c).edit().putBoolean("calib_done", done).apply()

    fun isAnimStepDone(c: Context): Boolean = sp(c).getBoolean("anim_done", false)
    fun setAnimStepDone(c: Context, done: Boolean) = sp(c).edit().putBoolean("anim_done", done).apply()

    fun getSelectedAnimId(c: Context): String? = sp(c).getString("selected_anim_id", null)
    fun getSelectedAnimLabel(c: Context): String? = sp(c).getString("selected_anim_label", null)

    fun setSelectedAnim(c: Context, id: String?, label: String?) {
        sp(c).edit()
            .putString("selected_anim_id", id)
            .putString("selected_anim_label", label)
            .apply()
    }

    fun getSelectedVideoAsset(c: Context): String? =
        sp(c).getString("selected_video_asset", null)?.takeIf { it.isNotBlank() }

    fun setSelectedVideoAsset(c: Context, assetPath: String?) {
        sp(c).edit()
            .putString("selected_video_asset", assetPath)
            .apply()
    }
}

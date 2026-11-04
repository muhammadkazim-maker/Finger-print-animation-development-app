package com.example

import android.content.Context

// cx, cy = fractions of display width/height; size = diameter / display width; speed = playback speed multiplier;
// sensorSize = diameter of the inner physical fingerprint sensor circle / display width.
// Defaults (0.5, 0.82, 0.28, 1.0, 0.10) for the itel S25 optical fingerprint sensor.
data class Calib(
    val cx: Float,
    val cy: Float,
    val size: Float,
    val speed: Float = 1.0f,
    val sensorSize: Float = 0.10f
)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("fp", Context.MODE_PRIVATE)

    fun load(c: Context): Calib = sp(c).let {
        Calib(
            cx = it.getFloat("cx", 0.5f),
            cy = it.getFloat("cy", 0.82f),
            size = it.getFloat("size", 0.28f),
            speed = it.getFloat("speed", 1.0f),
            sensorSize = it.getFloat("sensor_size", 0.10f)
        )
    }

    fun getRealDisplayBounds(c: Context): android.graphics.Rect {
        val wm = c.getSystemService(android.view.WindowManager::class.java)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && wm != null) {
            wm.currentWindowMetrics.bounds
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(dm)
            android.graphics.Rect(0, 0, dm.widthPixels.coerceAtLeast(1080), dm.heightPixels.coerceAtLeast(2400))
        }
    }

    fun save(c: Context, v: Calib) {
        sp(c).edit()
            .putFloat("cx", v.cx.coerceIn(0.05f, 0.95f))
            .putFloat("cy", v.cy.coerceIn(0.05f, 0.95f))
            .putFloat("size", v.size.coerceIn(0.15f, 1.0f))
            .putFloat("speed", v.speed.coerceIn(0.25f, 8.0f))
            .putFloat("sensor_size", v.sensorSize.coerceIn(0.04f, 0.30f))
            .putBoolean("calib_done", true)
            .apply()
    }

    fun saveCustomization(c: Context, size: Float, speed: Float, sensorSize: Float = 0.10f) {
        sp(c).edit()
            .putFloat("size", size.coerceIn(0.15f, 1.0f))
            .putFloat("speed", speed.coerceIn(0.25f, 8.0f))
            .putFloat("sensor_size", sensorSize.coerceIn(0.04f, 0.30f))
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

    fun getSelectedAnimId(c: Context): String? =
        sp(c).getString("selected_anim_id", null)?.takeIf {
            it.startsWith("pro_") || it.startsWith("folder_")
        } ?: ProVideoFolder.items.first().id

    fun getSelectedAnimLabel(c: Context): String? {
        val savedId = sp(c).getString("selected_anim_id", null)
        val savedLabel = sp(c).getString("selected_anim_label", null)
        return if (!savedLabel.isNullOrBlank() &&
            (savedId?.startsWith("pro_") == true || savedId?.startsWith("folder_") == true)
        ) {
            savedLabel
        } else {
            ProVideoFolder.items.first().title
        }
    }

    fun setSelectedAnim(c: Context, id: String?, label: String?) {
        val resolvedId = id?.takeIf { it.isNotBlank() } ?: ProVideoFolder.items.first().id
        val resolvedLabel = label?.takeIf { it.isNotBlank() } ?: ProVideoFolder.items.first().title
        sp(c).edit()
            .putString("selected_anim_id", resolvedId)
            .putString("selected_anim_label", resolvedLabel)
            .apply()
    }

    fun getSelectedVideoAsset(c: Context): String =
        sp(c).getString("selected_video_asset", null)?.takeIf { it.isNotBlank() }
            ?: ProVideoFolder.items.first().assetPath

    fun setSelectedVideoAsset(c: Context, assetPath: String?) {
        val resolved = assetPath?.takeIf { it.isNotBlank() } ?: ProVideoFolder.items.first().assetPath
        sp(c).edit()
            .putString("selected_video_asset", resolved)
            .apply()
    }
}

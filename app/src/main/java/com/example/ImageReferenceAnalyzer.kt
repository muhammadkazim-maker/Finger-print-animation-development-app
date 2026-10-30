package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class ReferenceImageAttachment(
    val uri: Uri,
    val bitmap: Bitmap,
    val signature: VisualReferenceSignature
)

data class VisualReferenceSignature(
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val contourPoints: List<Pair<Float, Float>>,
    val shapeSummary: String
)

object ImageReferenceAnalyzer {

    private const val MAX_BITMAP_DIMENSION = 768

    suspend fun loadAndAnalyzeUri(context: Context, uri: Uri): ReferenceImageAttachment? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = decodeDownscaledBitmap(context, uri) ?: return@runCatching null
                val signature = analyzeBitmap(bitmap)
                ReferenceImageAttachment(
                    uri = uri,
                    bitmap = bitmap,
                    signature = signature
                )
            }.getOrNull()
        }

    fun decodeDownscaledBitmap(context: Context, uri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        var sampleSize = 1
        while (rawW / sampleSize > MAX_BITMAP_DIMENSION * 2 || rawH / sampleSize > MAX_BITMAP_DIMENSION * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val maxSide = max(decoded.width, decoded.height)
        if (maxSide <= MAX_BITMAP_DIMENSION) {
            return decoded
        }
        val scale = MAX_BITMAP_DIMENSION.toFloat() / maxSide.toFloat()
        val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
    }

    fun analyzeBitmap(bitmap: Bitmap): VisualReferenceSignature {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 4 || h <= 4) {
            return VisualReferenceSignature(
                primaryHex = 0xFF00E5FFL,
                secondaryHex = 0xFF7C4DFFL,
                accentHex = 0xFFFFD740L,
                contourPoints = emptyList(),
                shapeSummary = "balanced circular ring"
            )
        }

        // 1. Extract vibrant color buckets across a 12-hue histogram + high-luminance highlights
        val hueBuckets = IntArray(12)
        val hueR = LongArray(12)
        val hueG = LongArray(12)
        val hueB = LongArray(12)
        val hsv = FloatArray(3)

        val stepX = max(1, w / 48)
        val stepY = max(1, h / 48)

        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                if (alpha < 64) continue
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                Color.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                // Ignore pitch-black background pixels
                if (value < 0.16f) continue
                val weight = ((sat * 0.7f + value * 0.3f) * 10f).roundToInt().coerceAtLeast(1)
                val bucket = ((hsv[0] / 30f).toInt()).coerceIn(0, 11)
                hueBuckets[bucket] += weight
                hueR[bucket] += (r * weight).toLong()
                hueG[bucket] += (g * weight).toLong()
                hueB[bucket] += (b * weight).toLong()
            }
        }

        val rankedBuckets = (0 until 12)
            .filter { hueBuckets[it] > 0 }
            .sortedByDescending { hueBuckets[it] }

        fun bucketToHex(bucket: Int, fallback: Long): Long {
            val count = hueBuckets[bucket].toLong()
            if (count <= 0L) return fallback
            val r = (hueR[bucket] / count).coerceIn(0L, 255L)
            val g = (hueG[bucket] / count).coerceIn(0L, 255L)
            val b = (hueB[bucket] / count).coerceIn(0L, 255L)
            // Boost brightness slightly for crisp AMOLED visibility
            val maxChannel = max(r, max(g, b))
            val boost = if (maxChannel in 1..190) 225f / maxChannel.toFloat() else 1f
            val br = (r * boost).roundToInt().coerceIn(0, 255).toLong()
            val bg = (g * boost).roundToInt().coerceIn(0, 255).toLong()
            val bb = (b * boost).roundToInt().coerceIn(0, 255).toLong()
            return 0xFF000000L or (br shl 16) or (bg shl 8) or bb
        }

        val primaryHex = if (rankedBuckets.isNotEmpty()) {
            bucketToHex(rankedBuckets[0], 0xFF00E5FFL)
        } else {
            0xFF00E5FFL
        }

        val secondaryHex = if (rankedBuckets.size >= 2) {
            bucketToHex(rankedBuckets[1], 0xFF7C4DFFL)
        } else {
            shiftHueHex(primaryHex, 48f)
        }

        val accentHex = if (rankedBuckets.size >= 3) {
            bucketToHex(rankedBuckets[2], 0xFFFFD740L)
        } else {
            shiftHueHex(primaryHex, -48f)
        }

        // 2. Trace 16-point radial silhouette around image center
        val cx = w / 2f
        val cy = h / 2f
        val maxRadiusPx = min(w, h) * 0.46f
        val rayCount = 16
        val radiiNorm = FloatArray(rayCount)

        for (i in 0 until rayCount) {
            val angleRad = (i * 2.0 * Math.PI / rayCount) - (Math.PI / 2.0) // Start at top (12 o'clock)
            val dx = cos(angleRad).toFloat()
            val dy = sin(angleRad).toFloat()

            var furthestBrightRatio = 0.65f
            var peakScore = 0f

            val steps = 36
            for (s in 8..steps) {
                val ratio = s.toFloat() / steps.toFloat()
                val px = (cx + dx * maxRadiusPx * ratio).roundToInt().coerceIn(0, w - 1)
                val py = (cy + dy * maxRadiusPx * ratio).roundToInt().coerceIn(0, h - 1)
                val pixel = bitmap.getPixel(px, py)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                Color.RGBToHSV(r, g, b, hsv)
                val lum = hsv[2]
                val sat = hsv[1]
                val score = lum * (0.5f + 0.5f * sat)
                if (score > 0.25f && ratio >= furthestBrightRatio * 0.8f) {
                    furthestBrightRatio = ratio
                }
                if (score > peakScore) {
                    peakScore = score
                }
            }
            radiiNorm[i] = furthestBrightRatio.coerceIn(0.42f, 0.96f)
        }

        // Smooth radial silhouette slightly while preserving sharp peaks/wings/points
        val contourPoints = ArrayList<Pair<Float, Float>>(rayCount)
        for (i in 0 until rayCount) {
            val prev = radiiNorm[(i - 1 + rayCount) % rayCount]
            val curr = radiiNorm[i]
            val next = radiiNorm[(i + 1) % rayCount]
            val smoothed = (prev * 0.2f + curr * 0.6f + next * 0.2f)
            val lottieRadius = 38f + smoothed * 50f // Maps into [58..86] on 200x200 Lottie canvas
            val angleRad = (i * 2.0 * Math.PI / rayCount) - (Math.PI / 2.0)
            val lx = (cos(angleRad) * lottieRadius).toFloat()
            val ly = (sin(angleRad) * lottieRadius).toFloat()
            contourPoints.add(lx to ly)
        }

        // Summarize shape geometry for Gemini prompt augmentation
        val minR = radiiNorm.minOrNull() ?: 0.6f
        val maxR = radiiNorm.maxOrNull() ?: 0.8f
        val variance = maxR - minR
        val topDominant = radiiNorm[0] > radiiNorm[8] + 0.12f
        val wingSymmetry = (radiiNorm[2] + radiiNorm[14]) > (radiiNorm[0] + radiiNorm[8]) + 0.16f

        val shapeSummary = when {
            wingSymmetry -> "bilateral winged / butterfly / heart crest silhouette"
            topDominant -> "top-crested crown / flame / droplet silhouette"
            variance > 0.22f -> "multi-point star / polygon / crystal geometric frame"
            else -> "luminous concentric biometric ring"
        }

        return VisualReferenceSignature(
            primaryHex = primaryHex,
            secondaryHex = secondaryHex,
            accentHex = accentHex,
            contourPoints = contourPoints,
            shapeSummary = shapeSummary
        )
    }

    fun parseVerticesCsv(csv: String): List<Pair<Float, Float>> {
        if (csv.isBlank()) return emptyList()
        val rawPairs = csv.split(",", ";", " ")
            .map { it.trim() }
            .filter { it.contains(":") }
        val parsed = rawPairs.mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size != 2) return@mapNotNull null
            val x = parts[0].toFloatOrNull()?.coerceIn(-92f, 92f) ?: return@mapNotNull null
            val y = parts[1].toFloatOrNull()?.coerceIn(-92f, 92f) ?: return@mapNotNull null
            if (hypot(x.toDouble(), y.toDouble()) < 18.0) return@mapNotNull null
            x to y
        }
        return if (parsed.size >= 4) parsed.take(24) else emptyList()
    }

    fun formatHex(hex: Long): String {
        return String.format(Locale.US, "#%06X", hex and 0xFFFFFFL)
    }

    private fun shiftHueHex(hex: Long, deltaDeg: Float): Long {
        val r = ((hex shr 16) and 0xFF).toInt()
        val g = ((hex shr 8) and 0xFF).toInt()
        val b = (hex and 0xFF).toInt()
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        hsv[0] = ((hsv[0] + deltaDeg) % 360f + 360f) % 360f
        hsv[1] = max(0.65f, hsv[1])
        hsv[2] = max(0.88f, hsv[2])
        val rgb = Color.HSVToColor(hsv)
        return 0xFF000000L or (rgb.toLong() and 0xFFFFFFL)
    }
}

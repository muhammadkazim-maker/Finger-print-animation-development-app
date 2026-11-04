package com.example

import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class AnimationCategory(
    val number: Int,
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val accentHex: Long,
    val secondaryHex: Long,
    val designs: List<AnimationDesign>
)

data class AnimationDesign(
    val id: String,
    val index: Int,
    val name: String,
    val styleLabel: String,
    val description: String,
    val primaryColor: FloatArray,
    val secondaryColor: FloatArray,
    val lottieJson: String,
    val videoAssetPath: String? = null
)

/**
 * Per user requirement, all non-Pro animations have been removed so only the 129 HD
 * Pro Folder animations remain in the catalog.
 */
object AnimationCatalog {

    val proVideoDesigns: List<AnimationDesign> by lazy {
        ProVideoFolder.items.map { item ->
            val pColor = hexToFloatRgb(item.accentHex)
            val sColor = hexToFloatRgb(item.accentHex)
            AnimationDesign(
                id = item.id,
                index = item.index,
                name = item.title,
                styleLabel = item.badge,
                description = item.subtitle,
                primaryColor = pColor,
                secondaryColor = sColor,
                lottieJson = buildCompactFallbackLottieJson(
                    name = item.title,
                    primary = pColor,
                    secondary = sColor
                ),
                videoAssetPath = item.assetPath
            )
        }
    }

    val proVideoCategory: AnimationCategory by lazy {
        AnimationCategory(
            number = 1,
            id = "pro_video_folder",
            title = "Pro Video Animations Folder (${proVideoDesigns.size} HD Animations)",
            shortTitle = "Pro Video Folder",
            subtitle = "Ultra-Crisp HD 60fps Video Loops & Multi-Ring 3D HUD Animations",
            accentHex = 0xFF00E5FFL,
            secondaryHex = 0xFF7C4DFFL,
            designs = proVideoDesigns
        )
    }

    /**
     * Only the Pro Folder category exists; all other animation sheets have been deleted.
     */
    val categories: List<AnimationCategory> by lazy {
        listOf(proVideoCategory)
    }

    val allDesigns: List<AnimationDesign> by lazy {
        proVideoDesigns
    }

    val totalDesignCount: Int
        get() = proVideoDesigns.size

    fun findDesign(id: String?): AnimationDesign? {
        if (id.isNullOrBlank()) return null
        return proVideoDesigns.find { it.id == id }
    }

    fun parseHexColor(hexString: String?, fallback: Long = 0xFF00E5FFL): Long {
        if (hexString.isNullOrBlank()) return fallback
        val clean = hexString.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        return try {
            val parsed = clean.toLong(16)
            if (clean.length <= 6) {
                0xFF000000L or (parsed and 0xFFFFFFL)
            } else {
                parsed
            }
        } catch (_: Throwable) {
            fallback
        }
    }

    private fun hexToFloatRgb(hex: Long): FloatArray {
        val r = ((hex shr 16) and 0xFFL) / 255f
        val g = ((hex shr 8) and 0xFFL) / 255f
        val b = (hex and 0xFFL) / 255f
        return floatArrayOf(f(r), f(g), f(b))
    }

    private fun f(v: Float): Float = ((v * 1000f).roundToInt() / 1000f).coerceIn(0f, 1f)

    private fun cStr(c: FloatArray): String =
        String.format(Locale.US, "[%.3f,%.3f,%.3f,1]", c[0], c[1], c[2])

    private fun buildCompactFallbackLottieJson(
        name: String,
        primary: FloatArray,
        secondary: FloatArray
    ): String {
        val safeName = name.replace("\"", "").take(28)
        return """{"v":"5.7.4","fr":60,"ip":0,"op":120,"w":240,"h":240,"nm":"$safeName","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":4,"nm":"Outer","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":1,"k":[{"t":0,"s":[0],"e":[360]},{"t":120,"s":[360]}]},"p":{"a":0,"k":[120,120,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"ao":0,"shapes":[{"ty":"gr","it":[{"d":1,"ty":"el","s":{"a":0,"k":[188,188]},"p":{"a":0,"k":[0,0]},"nm":"E1"},{"ty":"st","c":{"a":0,"k":${cStr(primary)}},"o":{"a":0,"k":100},"w":{"a":0,"k":6},"lc":2,"lj":2,"nm":"S1"},{"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}],"nm":"G1"}],"ip":0,"op":120,"st":0,"bm":0},{"ddd":0,"ind":2,"ty":4,"nm":"Inner","sr":1,"ks":{"o":{"a":0,"k":90},"r":{"a":1,"k":[{"t":0,"s":[360],"e":[0]},{"t":120,"s":[0]}]},"p":{"a":0,"k":[120,120,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":1,"k":[{"t":0,"s":[92,92,100],"e":[106,106,100]},{"t":60,"s":[106,106,100],"e":[92,92,100]},{"t":120,"s":[92,92,100]}]}},"ao":0,"shapes":[{"ty":"gr","it":[{"d":1,"ty":"el","s":{"a":0,"k":[124,124]},"p":{"a":0,"k":[0,0]},"nm":"E2"},{"ty":"st","c":{"a":0,"k":${cStr(secondary)}},"o":{"a":0,"k":95},"w":{"a":0,"k":5},"lc":2,"lj":2,"nm":"S2"},{"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}],"nm":"G2"}],"ip":0,"op":120,"st":0,"bm":0}]}"""
    }

    fun buildAiCustomLottieJson(
        title: String,
        @Suppress("UNUSED_PARAMETER") primaryArchetypeName: String,
        @Suppress("UNUSED_PARAMETER") secondaryArchetypeName: String,
        primaryHex: Long,
        secondaryHex: Long,
        accentHex: Long = 0xFFFFD740L,
        strokeWidth: Int = 6,
        starPoints: Int = 8,
        @Suppress("UNUSED_PARAMETER") dashLen: Int = 14,
        @Suppress("UNUSED_PARAMETER") gapLen: Int = 8,
        clockwise: Boolean = true,
        customContourPoints: List<Pair<Float, Float>> = emptyList()
    ): String {
        val pCol = hexToFloatRgb(primaryHex)
        val sCol = hexToFloatRgb(secondaryHex)
        val aCol = hexToFloatRgb(accentHex)
        val safeTitle = title.replace("\"", "").take(32)
        val startRot = if (clockwise) 0 else 360
        val endRot = if (clockwise) 360 else 0
        val sw = strokeWidth.coerceIn(3, 10)

        val pts = if (customContourPoints.size >= 3) {
            customContourPoints.take(36)
        } else {
            val n = starPoints.coerceIn(4, 24)
            (0 until n).map { idx ->
                val ang = (2.0 * PI * idx) / n
                val rad = if (idx % 2 == 0) 76f else 54f
                Pair((cos(ang) * rad).toFloat(), (sin(ang) * rad).toFloat())
            }
        }
        val vArr = pts.joinToString(",") { (px, py) ->
            String.format(Locale.US, "[%.1f,%.1f]", px, py)
        }
        val zeroTangents = pts.joinToString(",") { "[0,0]" }

        return """{"v":"5.7.4","fr":60,"ip":0,"op":120,"w":240,"h":240,"nm":"$safeTitle","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":4,"nm":"CustomShape","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":1,"k":[{"t":0,"s":[$startRot],"e":[$endRot]},{"t":120,"s":[$endRot]}]},"p":{"a":0,"k":[120,120,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"ao":0,"shapes":[{"ty":"gr","it":[{"ty":"sh","ks":{"a":0,"k":{"i":[$zeroTangents],"o":[$zeroTangents],"v":[$vArr],"c":true}},"nm":"Path1"},{"ty":"st","c":{"a":0,"k":${cStr(pCol)}},"o":{"a":0,"k":100},"w":{"a":0,"k":$sw},"lc":2,"lj":2,"nm":"Stroke1"},{"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}],"nm":"Group1"}],"ip":0,"op":120,"st":0,"bm":0},{"ddd":0,"ind":2,"ty":4,"nm":"SecondaryRing","sr":1,"ks":{"o":{"a":0,"k":90},"r":{"a":1,"k":[{"t":0,"s":[$endRot],"e":[$startRot]},{"t":120,"s":[$startRot]}]},"p":{"a":0,"k":[120,120,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":1,"k":[{"t":0,"s":[92,92,100],"e":[106,106,100]},{"t":60,"s":[106,106,100],"e":[92,92,100]},{"t":120,"s":[92,92,100]}]}},"ao":0,"shapes":[{"ty":"gr","it":[{"d":1,"ty":"el","s":{"a":0,"k":[164,164]},"p":{"a":0,"k":[0,0]},"nm":"Ring2"},{"ty":"st","c":{"a":0,"k":${cStr(sCol)}},"o":{"a":0,"k":95},"w":{"a":0,"k":$sw},"lc":2,"lj":2,"nm":"Stroke2"},{"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}],"nm":"Group2"}],"ip":0,"op":120,"st":0,"bm":0},{"ddd":0,"ind":3,"ty":4,"nm":"AccentCore","sr":1,"ks":{"o":{"a":0,"k":95},"r":{"a":0,"k":0},"p":{"a":0,"k":[120,120,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"ao":0,"shapes":[{"ty":"gr","it":[{"d":1,"ty":"el","s":{"a":0,"k":[88,88]},"p":{"a":0,"k":[0,0]},"nm":"Core3"},{"ty":"st","c":{"a":0,"k":${cStr(aCol)}},"o":{"a":0,"k":90},"w":{"a":0,"k":4},"lc":2,"lj":2,"nm":"Stroke3"},{"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}],"nm":"Group3"}],"ip":0,"op":120,"st":0,"bm":0}]}"""
    }
}

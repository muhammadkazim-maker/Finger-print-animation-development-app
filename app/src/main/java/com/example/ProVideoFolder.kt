package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Looper
import android.util.LruCache
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.RenderMode
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class ProMotionProfile {
    ATLAS_VIDEO,
    SPEEDOMETER_HUD,
    HUD_RETICLE_CROSSHAIRS,
    SPIROGRAPH_BRAID_TORUS,
    ELECTRIC_PLASMA_LIGHTNING,
    CONTINUOUS_WAVE_TORUS,
    ORBITAL_STARFIELD_RING,
    POLYGON_APERTURE_IRIS,
    EQUALIZER_WAVEFORM_HUD,
    CYBER_PCB_CIRCUIT,
    STAR_WAVE_CONTOUR,
    BRAIDED_LATTICE_DOTS
}

data class ProVideoItem(
    val id: String,
    val index: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val assetPath: String,
    val atlasAssetPath: String,
    val accentHex: Long,
    val fallbackLottieDesignId: String,
    val isAtlasGrid: Boolean = false,
    val motionProfile: ProMotionProfile = ProMotionProfile.HUD_RETICLE_CROSSHAIRS
)

/**
 * Pre-decomposed concentric ring bitmaps of a real uploaded design (`innerCore + midRing + outerRing == original`).
 * Created only along natural empty radial gaps (or kept unified for continuous 1-body wave/star/lightning designs)
 * so not a single line, polygon edge, wave ribbon, or lightning bolt is ever torn or distorted.
 */
class ProDesignLayers(
    val fullBitmap: Bitmap,
    val innerCore: Bitmap,
    val midRing: Bitmap,
    val outerRing: Bitmap
) {
    val isRecycled: Boolean
        get() = fullBitmap.isRecycled || innerCore.isRecycled || midRing.isRecycled || outerRing.isRecycled

    val allocationKb: Int
        get() {
            var totalBytes = fullBitmap.allocationByteCount
            if (innerCore !== fullBitmap) totalBytes += innerCore.allocationByteCount
            if (midRing !== fullBitmap && midRing !== innerCore) totalBytes += midRing.allocationByteCount
            if (outerRing !== fullBitmap && outerRing !== innerCore && outerRing !== midRing) {
                totalBytes += outerRing.allocationByteCount
            }
            return (totalBytes / 1024).coerceAtLeast(1)
        }
}

object ProVideoFolder {
    const val FOLDER_PATH = "pro_videos"
    const val FOLDER_TITLE = "Pro Video & Real Designs Folder"

    private data class ProDesignSpec(
        val idSlug: String,
        val assetPath: String,
        val title: String,
        val subtitle: String,
        val badge: String,
        val accentHex: Long,
        val profile: ProMotionProfile
    )

    private val featuredRealDesignSpecs: List<ProDesignSpec> by lazy {
        listOf(
            ProDesignSpec(
                idSlug = "featured_001_star_radar",
                assetPath = "pro_videos/featured_001.webp",
                title = "Sapphire 12-Spoke Starlight Radar",
                subtitle = "Real uploaded design: 12-spoke sapphire radar grid, perimeter star flares & blazing cyan core",
                badge = "Real Design #28",
                accentHex = 0xFF00B0FFL,
                profile = ProMotionProfile.STAR_WAVE_CONTOUR
            ),
            ProDesignSpec(
                idSlug = "featured_002_cyan_segmented_hud",
                assetPath = "pro_videos/featured_002.webp",
                title = "Electric Cyan Segmented HUD Ring",
                subtitle = "Real uploaded design: counter-rotating cyan HUD blocks, 44-dot inner ring & 4 glowing nodes",
                badge = "Real Design #29",
                accentHex = 0xFF00E5FFL,
                profile = ProMotionProfile.HUD_RETICLE_CROSSHAIRS
            ),
            ProDesignSpec(
                idSlug = "featured_003_violet_concentric_hud",
                assetPath = "pro_videos/featured_003.webp",
                title = "Ultraviolet Concentric Vernier HUD",
                subtitle = "Real uploaded design: neon purple-violet concentric HUD rings, cardinal crosshairs & dot bezel",
                badge = "Real Design #30",
                accentHex = 0xFFB388FFL,
                profile = ProMotionProfile.HUD_RETICLE_CROSSHAIRS
            ),
            ProDesignSpec(
                idSlug = "featured_004_golden_armillary_gyro",
                assetPath = "pro_videos/featured_004.webp",
                title = "Golden 3D Armillary Gyroscope Sphere",
                subtitle = "Real uploaded design: 3D tilted golden orbital ribbons, 4 metallic cardinal spheres & solar core",
                badge = "Real Design #31",
                accentHex = 0xFFFFD740L,
                profile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProDesignSpec(
                idSlug = "featured_005_crimson_silk_torus",
                assetPath = "pro_videos/featured_005.webp",
                title = "Crimson-Gold 3D Silk Wave Torus",
                subtitle = "Real uploaded design: weaving translucent orange-red silk ribbon waves & floating ember sparks",
                badge = "Real Design #32",
                accentHex = 0xFFFF3D00L,
                profile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProDesignSpec(
                idSlug = "featured_006_cobalt_violet_vortex",
                assetPath = "pro_videos/featured_006.webp",
                title = "Cobalt & Violet Particle Vortex Ring",
                subtitle = "Real uploaded design: swirling electric blue & neon violet particle stream trails around dark core",
                badge = "Real Design #33",
                accentHex = 0xFF651FFFL,
                profile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProDesignSpec(
                idSlug = "featured_007_molten_gold_splash",
                assetPath = "pro_videos/featured_007.webp",
                title = "Molten Liquid Gold 3D Splash Ring",
                subtitle = "Real uploaded design: glossy 3D metallic molten gold wave crests & splashing golden droplets",
                badge = "Real Design #34",
                accentHex = 0xFFFFD740L,
                profile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProDesignSpec(
                idSlug = "featured_008_mechanical_iris",
                assetPath = "pro_videos/featured_008.webp",
                title = "Mechanical 12-Blade Aperture Iris",
                subtitle = "Real uploaded design: precision gunmetal & cyan 12-blade camera iris aperture with glowing core",
                badge = "Real Design #35",
                accentHex = 0xFF00E5FFL,
                profile = ProMotionProfile.POLYGON_APERTURE_IRIS
            ),
            ProDesignSpec(
                idSlug = "featured_009_violet_ribbon_portal",
                assetPath = "pro_videos/featured_009.webp",
                title = "Violet & Magenta Silk Ribbon Portal",
                subtitle = "Real uploaded design: flowing 3D violet, magenta & cyan silk light waves swirling around core",
                badge = "Real Design #36",
                accentHex = 0xFFE040FBL,
                profile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProDesignSpec(
                idSlug = "featured_010_emerald_particle_portal",
                assetPath = "pro_videos/featured_010.webp",
                title = "Bioluminescent Emerald Particle Portal",
                subtitle = "Real uploaded design: golden-ratio spiral of glowing emerald & mint quantum particles",
                badge = "Real Design #37",
                accentHex = 0xFF00E676L,
                profile = ProMotionProfile.ORBITAL_STARFIELD_RING
            ),
            ProDesignSpec(
                idSlug = "featured_011_icy_filament_vortex",
                assetPath = "pro_videos/featured_011.webp",
                title = "Cryo-Cyan Icy Filament Vortex",
                subtitle = "Real uploaded design: high-velocity ice-blue & white curved radial vortex filaments",
                badge = "Real Design #38",
                accentHex = 0xFF80D8FFL,
                profile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            )
        )
    }

    /**
     * Exact, as-is circular HUD designs extracted from `Circular_HUD_Animation_Designs_100_Compact (1).pdf`,
     * strictly deduplicated across the 10 archetypes and 6 distinct color diagonals so no animation ever
     * appears 2 or 3 times after animating.
     */
    private val pdfHudDesignSpecs: List<ProDesignSpec> by lazy {
        val archetypeNames = arrayOf(
            "Segmented Crosshair HUD Reticle",      // Arch 1
            "Guilloche Spirograph Braid Torus",     // Arch 2
            "High-Voltage Electric Plasma Ring",    // Arch 3
            "Sinusoidal Harmonic Wave Ribbon",      // Arch 4
            "Orbital Starlight Particle Cluster",   // Arch 5
            "Geometric Polygon Aperture Iris",      // Arch 6
            "Radial Equalizer & Sine Waveform",     // Arch 7
            "Cybernetic PCB Circuit Trace Matrix",  // Arch 8
            "Harmonic Star-Wave Contour Reticle",   // Arch 9
            "Braided Wave Lattice & Stipple Ring"   // Arch 10
        )
        val archetypeSubtitles = arrayOf(
            "Exact PDF HUD Design: counter-rotating inner reticle, segmented telemetry ring & outer satellite crosshairs",
            "Exact PDF HUD Design: intact guilloche spirograph braided torus with counter-rotating inner optical ring",
            "Exact PDF HUD Design: intact dual-layer jagged electric lightning arcs with high-voltage plasma surge",
            "Exact PDF HUD Design: intact multi-ribbon sinusoidal wave ring with glowing energy nodes & harmonic pulse",
            "Exact PDF HUD Design: orbital starfield particle cluster between ultra-crisp concentric guide arcs",
            "Exact PDF HUD Design: intact nested geometric polygon aperture iris with counter-rotating outer dot bezel",
            "Exact PDF HUD Design: rotating radial audio equalizer sunburst around live upright oscilloscope sine wave",
            "Exact PDF HUD Design: 3 counter-rotating concentric PCB circuit trace bands with glowing solder-pad nodes",
            "Exact PDF HUD Design: intact nested harmonic star-wave contours wrapping an inner geometric star core",
            "Exact PDF HUD Design: intact woven wave lattice surrounded by counter-rotating dual stipple dot rings"
        )
        val archetypeProfiles = arrayOf(
            ProMotionProfile.HUD_RETICLE_CROSSHAIRS,   // Arch 1
            ProMotionProfile.SPIROGRAPH_BRAID_TORUS,   // Arch 2
            ProMotionProfile.ELECTRIC_PLASMA_LIGHTNING,// Arch 3
            ProMotionProfile.CONTINUOUS_WAVE_TORUS,    // Arch 4
            ProMotionProfile.ORBITAL_STARFIELD_RING,   // Arch 5
            ProMotionProfile.POLYGON_APERTURE_IRIS,    // Arch 6
            ProMotionProfile.EQUALIZER_WAVEFORM_HUD,   // Arch 7
            ProMotionProfile.CYBER_PCB_CIRCUIT,        // Arch 8
            ProMotionProfile.STAR_WAVE_CONTOUR,        // Arch 9
            ProMotionProfile.BRAIDED_LATTICE_DOTS      // Arch 10
        )
        val keptDiags = mapOf(
            0 to Pair("Electric Cyan", 0xFF00E5FFL),
            1 to Pair("Cryo Ice-White", 0xFFE0F7FAL),
            2 to Pair("Crimson Coral", 0xFFFF5252L),
            3 to Pair("Ultraviolet Purple", 0xFFD500F9L),
            6 to Pair("Solar Gold", 0xFFFFB300L),
            9 to Pair("Bioluminescent Emerald", 0xFF00E676L)
        )

        val list = ArrayList<ProDesignSpec>(60)
        for (pdfPage in 1..100) {
            val archIdx = (pdfPage - 1) % 10
            val row = (pdfPage - 1) / 10
            val diag = ((archIdx - row) % 10 + 10) % 10
            val colorInfo = keptDiags[diag] ?: continue
            val seqNum = list.size + 1
            val seqStr = String.format(Locale.US, "%03d", seqNum)
            val pageStr = String.format(Locale.US, "%03d", pdfPage)
            list.add(
                ProDesignSpec(
                    idSlug = "hud_pdf_${seqStr}_p$pageStr",
                    assetPath = "pro_videos/hud_pdf_$seqStr.webp",
                    title = "${colorInfo.first} ${archetypeNames[archIdx]}",
                    subtitle = "${archetypeSubtitles[archIdx]} (PDF Page #$pageStr)",
                    badge = "PDF HUD #$pageStr",
                    accentHex = colorInfo.second,
                    profile = archetypeProfiles[archIdx]
                )
            )
        }
        list
    }

    val items: List<ProVideoItem> by lazy {
        val baseItems = listOf(
            ProVideoItem(
                id = "folder_video_1_chrono_reactor",
                index = 1,
                title = "Chrono Telemetry Reactor HUD",
                subtitle = "Crimson inner reactor teeth, white vernier scale, cobalt tachometer ticks & live counter",
                badge = "Original Video #1",
                assetPath = "pro_videos/video_1_chrono_reactor.mp4",
                atlasAssetPath = "pro_videos/video_1_chrono_reactor.webp",
                accentHex = 0xFF0066FFL,
                fallbackLottieDesignId = "pro_video_1_chrono_reactor",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
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
                fallbackLottieDesignId = "pro_video_2_singularity_supernova",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
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
                fallbackLottieDesignId = "pro_video_3_quantum_particle_sphere",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
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
                fallbackLottieDesignId = "pro_video_4_atomic_comet_orbits",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
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
                fallbackLottieDesignId = "pro_video_5_dichroic_liquid_orb",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
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
                fallbackLottieDesignId = "pro_video_6_plasma_caustic_sphere",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
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
                fallbackLottieDesignId = "pro_video_7_solar_cobalt_rings",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_8_cyber_speedometer_hud",
                index = 8,
                title = "Cyber Speedometer & Tachometer HUD",
                subtitle = "Exact uploaded HUD dial: 0-240 speedometer with sweeping pink needle, red plasma ring & RPM wings",
                badge = "Real Design #8",
                assetPath = "pro_videos/video_8_cyber_speedometer_hud.mp4",
                atlasAssetPath = "pro_videos/video_8_cyber_speedometer_hud.webp",
                accentHex = 0xFF00B0FFL,
                fallbackLottieDesignId = "pro_video_8_cyber_speedometer_hud",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.SPEEDOMETER_HUD
            ),
            ProVideoItem(
                id = "folder_video_10_chromatic_starlight_rings",
                index = 9,
                title = "Chromatic Starlight Vortex Rings",
                subtitle = "Exact uploaded design: multicolor neon plasma rings with golden starlight core",
                badge = "Real Design #9",
                assetPath = "pro_videos/video_10_chromatic_starlight_rings.mp4",
                atlasAssetPath = "pro_videos/video_10_chromatic_starlight_rings.webp",
                accentHex = 0xFFFF4081L,
                fallbackLottieDesignId = "pro_video_10_chromatic_starlight_rings",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProVideoItem(
                id = "folder_video_11_neon_magenta_cyan_portal",
                index = 10,
                title = "Neon Magenta & Cyan Cyber Portal",
                subtitle = "Exact uploaded design: glowing hot-pink outer tick ring & electric cyan inner portal ring",
                badge = "Real Design #10",
                assetPath = "pro_videos/video_11_neon_magenta_cyan_portal.mp4",
                atlasAssetPath = "pro_videos/video_11_neon_magenta_cyan_portal.webp",
                accentHex = 0xFFFF00E5L,
                fallbackLottieDesignId = "pro_video_11_neon_magenta_cyan_portal",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.HUD_RETICLE_CROSSHAIRS
            ),
            ProVideoItem(
                id = "folder_video_12_electric_blue_blade_vortex",
                index = 11,
                title = "Electric Sapphire Blade Vortex",
                subtitle = "Exact uploaded design: spinning sapphire-blue spiral energy blades with white-hot highlights",
                badge = "Real Design #11",
                assetPath = "pro_videos/video_12_electric_blue_blade_vortex.mp4",
                atlasAssetPath = "pro_videos/video_12_electric_blue_blade_vortex.webp",
                accentHex = 0xFF2979FFL,
                fallbackLottieDesignId = "pro_video_12_electric_blue_blade_vortex",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProVideoItem(
                id = "folder_video_13_cyan_quantum_tech_dial",
                index = 12,
                title = "Cyan Quantum HUD Tech Dial",
                subtitle = "Exact uploaded design: counter-rotating concentric cyan HUD gear & tick rings with glowing core",
                badge = "Real Design #12",
                assetPath = "pro_videos/video_13_cyan_quantum_tech_dial.mp4",
                atlasAssetPath = "pro_videos/video_13_cyan_quantum_tech_dial.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_13_cyan_quantum_tech_dial",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.HUD_RETICLE_CROSSHAIRS
            ),
            ProVideoItem(
                id = "folder_video_14_cyan_sunwave_tendrils",
                index = 13,
                title = "Cyan Sunwave Tendrils Ring",
                subtitle = "Exact uploaded video: organic cyan & teal solar-wave tendrils twisting around a dark core",
                badge = "Uploaded Video #13",
                assetPath = "pro_videos/video_14_cyan_sunwave_tendrils.mp4",
                atlasAssetPath = "pro_videos/video_14_cyan_sunwave_tendrils.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_14_cyan_sunwave_tendrils",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_15_ice_blue_particle_globe",
                index = 14,
                title = "Ice-Blue 3D Particle Vortex Globe",
                subtitle = "Exact uploaded video: glowing ice-blue luminous particle vortex sphere swirling in 3D",
                badge = "Uploaded Video #14",
                assetPath = "pro_videos/video_15_ice_blue_particle_globe.mp4",
                atlasAssetPath = "pro_videos/video_15_ice_blue_particle_globe.webp",
                accentHex = 0xFF40C4FFL,
                fallbackLottieDesignId = "pro_video_15_ice_blue_particle_globe",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_16_cobalt_violet_wave_orb",
                index = 15,
                title = "Cobalt & Violet 3D Wave Orb",
                subtitle = "Exact uploaded video: deep cobalt-blue & violet 3D undulating particle wave sphere",
                badge = "Uploaded Video #15",
                assetPath = "pro_videos/video_16_cobalt_violet_wave_orb.mp4",
                atlasAssetPath = "pro_videos/video_16_cobalt_violet_wave_orb.webp",
                accentHex = 0xFF536DFEL,
                fallbackLottieDesignId = "pro_video_16_cobalt_violet_wave_orb",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_17_neon_green_cyan_ribbon",
                index = 16,
                title = "Neon Green & Cyan Ribbon Wave",
                subtitle = "Exact uploaded video: morphing 3D neon emerald-green & electric-cyan harmonic ribbon ring",
                badge = "Uploaded Video #16",
                assetPath = "pro_videos/video_17_neon_green_cyan_ribbon.mp4",
                atlasAssetPath = "pro_videos/video_17_neon_green_cyan_ribbon.webp",
                accentHex = 0xFF00E676L,
                fallbackLottieDesignId = "pro_video_17_neon_green_cyan_ribbon",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_18_magenta_stardust_portal",
                index = 17,
                title = "Magenta Plasma & Stardust Portal",
                subtitle = "Exact uploaded video: blazing magenta plasma ring with shimmering blue stardust core",
                badge = "Uploaded Video #17",
                assetPath = "pro_videos/video_18_magenta_stardust_portal.mp4",
                atlasAssetPath = "pro_videos/video_18_magenta_stardust_portal.webp",
                accentHex = 0xFFFF4081L,
                fallbackLottieDesignId = "pro_video_18_magenta_stardust_portal",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_19_blue_plasma_energy_core",
                index = 18,
                title = "Blue Electric Plasma Energy Core",
                subtitle = "Exact uploaded video: high-voltage blue plasma energy ring with radiating lightning filaments",
                badge = "Uploaded Video #18",
                assetPath = "pro_videos/video_19_blue_plasma_energy_core.mp4",
                atlasAssetPath = "pro_videos/video_19_blue_plasma_energy_core.webp",
                accentHex = 0xFF2979FFL,
                fallbackLottieDesignId = "pro_video_19_blue_plasma_energy_core",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_20_cyan_purple_silk_orb",
                index = 19,
                title = "Cyan & Purple 3D Silk Mesh Orb",
                subtitle = "Exact uploaded video: rotating 3D electric cyan & violet silk particle mesh sphere",
                badge = "Uploaded Video #19",
                assetPath = "pro_videos/video_20_cyan_purple_silk_orb.mp4",
                atlasAssetPath = "pro_videos/video_20_cyan_purple_silk_orb.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_20_cyan_purple_silk_orb",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_21_polar_star_radar_grid",
                index = 20,
                title = "Polar Starlight Radar Grid",
                subtitle = "Exact uploaded design: sapphire polar radar grid with 8 pulsing perimeter star-nodes",
                badge = "Real Design #20",
                assetPath = "pro_videos/video_21_polar_star_radar_grid.mp4",
                atlasAssetPath = "pro_videos/video_21_polar_star_radar_grid.webp",
                accentHex = 0xFF448AFFL,
                fallbackLottieDesignId = "pro_video_21_polar_star_radar_grid",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.STAR_WAVE_CONTOUR
            ),
            ProVideoItem(
                id = "folder_video_22_cyan_dot_matrix_hud",
                index = 21,
                title = "Cyan Dot-Matrix Telemetry HUD",
                subtitle = "Exact uploaded design: counter-rotating cyan dot-matrix, equalizer bar & scanner arc rings",
                badge = "Real Design #21",
                assetPath = "pro_videos/video_22_cyan_dot_matrix_hud.mp4",
                atlasAssetPath = "pro_videos/video_22_cyan_dot_matrix_hud.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_22_cyan_dot_matrix_hud",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.HUD_RETICLE_CROSSHAIRS
            ),
            ProVideoItem(
                id = "folder_video_24_aqua_wave_mesh_sphere",
                index = 22,
                title = "Aqua 3D Wave Mesh Sphere",
                subtitle = "Exact uploaded design: 3D cyan particle stipple core & undulating wave crest shell",
                badge = "Real Design #22",
                assetPath = "pro_videos/video_24_aqua_wave_mesh_sphere.mp4",
                atlasAssetPath = "pro_videos/video_24_aqua_wave_mesh_sphere.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_24_aqua_wave_mesh_sphere",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProVideoItem(
                id = "folder_video_25_electric_lightning_globe",
                index = 23,
                title = "High-Voltage Electric Lightning Globe",
                subtitle = "Exact uploaded design: intact electric blue plasma lightning arcs with high-voltage surge",
                badge = "Real Design #23",
                assetPath = "pro_videos/video_25_electric_lightning_globe.mp4",
                atlasAssetPath = "pro_videos/video_25_electric_lightning_globe.webp",
                accentHex = 0xFF2979FFL,
                fallbackLottieDesignId = "pro_video_25_electric_lightning_globe",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.ELECTRIC_PLASMA_LIGHTNING
            ),
            ProVideoItem(
                id = "folder_video_26_fiber_optic_light_ring",
                index = 24,
                title = "White-Cyan Fiber-Optic Swirl Ring",
                subtitle = "Exact uploaded design: streaming white & cyan fiber-optic light trails on transparent core",
                badge = "Real Design #24",
                assetPath = "pro_videos/video_26_fiber_optic_light_ring.mp4",
                atlasAssetPath = "pro_videos/video_26_fiber_optic_light_ring.webp",
                accentHex = 0xFF80D8FFL,
                fallbackLottieDesignId = "pro_video_26_fiber_optic_light_ring",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProVideoItem(
                id = "folder_video_27_cyber_notched_hud_scanner",
                index = 25,
                title = "Cyber Notched Tactical HUD Scanner",
                subtitle = "Exact uploaded design: counter-rotating outer notched brackets, triangle indicators & inner ticks",
                badge = "Real Design #25",
                assetPath = "pro_videos/video_27_cyber_notched_hud_scanner.mp4",
                atlasAssetPath = "pro_videos/video_27_cyber_notched_hud_scanner.webp",
                accentHex = 0xFF00B0FFL,
                fallbackLottieDesignId = "pro_video_27_cyber_notched_hud_scanner",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.HUD_RETICLE_CROSSHAIRS
            ),
            ProVideoItem(
                id = "folder_video_28_hypnotic_spiral_torus",
                index = 26,
                title = "Hypnotic Optical Spiral Torus",
                subtitle = "Exact uploaded design: spinning blue, cyan & gold logarithmic spiral torus with optical wave motion",
                badge = "Real Design #26",
                assetPath = "pro_videos/video_28_hypnotic_spiral_torus.mp4",
                atlasAssetPath = "pro_videos/video_28_hypnotic_spiral_torus.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_28_hypnotic_spiral_torus",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CONTINUOUS_WAVE_TORUS
            ),
            ProVideoItem(
                id = "folder_video_29_neon_circuit_trace_ring",
                index = 27,
                title = "Neon Pink & Cyan Circuit Trace Ring",
                subtitle = "Exact uploaded design: 3 counter-rotating neon magenta-to-cyan circuit trace bands with pulsing nodes",
                badge = "Real Design #27",
                assetPath = "pro_videos/video_29_neon_circuit_trace_ring.mp4",
                atlasAssetPath = "pro_videos/video_29_neon_circuit_trace_ring.webp",
                accentHex = 0xFFFF4081L,
                fallbackLottieDesignId = "pro_video_29_neon_circuit_trace_ring",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CYBER_PCB_CIRCUIT
            )
        )

        val allExtraSpecs = featuredRealDesignSpecs + pdfHudDesignSpecs
        val extraItems = allExtraSpecs.mapIndexed { offset, spec ->
            val itemIndex = baseItems.size + offset + 1
            ProVideoItem(
                id = "folder_${spec.idSlug}",
                index = itemIndex,
                title = spec.title,
                subtitle = spec.subtitle,
                badge = spec.badge,
                assetPath = spec.assetPath,
                atlasAssetPath = spec.assetPath,
                accentHex = spec.accentHex,
                fallbackLottieDesignId = "pro_${spec.idSlug}",
                isAtlasGrid = false,
                motionProfile = spec.profile
            )
        }

        baseItems + extraItems
    }

    private val itemByPath: Map<String, ProVideoItem> by lazy {
        val map = HashMap<String, ProVideoItem>(items.size * 2)
        for (it in items) {
            map[it.assetPath] = it
            map[it.atlasAssetPath] = it
        }
        map
    }

    fun findItem(assetPath: String?): ProVideoItem? {
        if (assetPath.isNullOrBlank()) return null
        return itemByPath[assetPath] ?: itemByPath[atlasPathFor(assetPath)]
    }

    fun atlasPathFor(assetPath: String): String {
        val matched = itemByPath[assetPath]
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
        return checkExist(atlasPathFor(assetPath)) || checkExist(assetPath)
    }
}

/**
 * Thread-safe, pre-warmed cache for both:
 * 1. `8x8` multi-frame WebP video atlases (`video_1..7` and `video_14..20`), and
 * 2. High-resolution `512x512` single-frame transparent real designs (`video_8..13`, `video_21..29`,
 *    `featured_001..011`, and `hud_pdf_001..060`), decomposed only along natural empty radial gaps
 *    so continuous designs are never torn or altered.
 */
object ProVideoFrameCache {
    const val ATLAS_COLS = 8
    const val ATLAS_ROWS = 8
    const val TOTAL_FRAMES = ATLAS_COLS * ATLAS_ROWS
    const val BASE_FPS = 30f

    private val mainHandler = android.os.Handler(Looper.getMainLooper())

    private val decodeExecutor = Executors.newFixedThreadPool(3) { r ->
        Thread(r, "ProVideoAtlasDecoder").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }

    private val priorityOverlayExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ProVideoActivePrewarm").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY + 2
        }
    }

    private val decodeLocks = HashMap<String, Any>()
    private val inFlightCallbacks = HashMap<String, MutableList<(Bitmap?) -> Unit>>()

    @Volatile
    private var pinnedActiveAtlasPath: String? = null

    @Volatile
    private var pinnedActiveBitmap: Bitmap? = null

    @Volatile
    private var pinnedActiveLayers: ProDesignLayers? = null

    private val memoryCache: LruCache<String, Bitmap> = run {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        val cacheSizeKb = (maxKb / 5).coerceIn(16 * 1024, 48 * 1024)
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return (value.allocationByteCount / 1024).coerceAtLeast(1)
            }
        }
    }

    private val layersCache: LruCache<String, ProDesignLayers> = run {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        val cacheSizeKb = (maxKb / 6).coerceIn(12 * 1024, 32 * 1024)
        object : LruCache<String, ProDesignLayers>(cacheSizeKb) {
            override fun sizeOf(key: String, value: ProDesignLayers): Int {
                return value.allocationKb
            }
        }
    }

    private val compactMemoryCache: LruCache<String, Bitmap> = run {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        val cacheSizeKb = (maxKb / 8).coerceIn(8 * 1024, 24 * 1024)
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return (value.allocationByteCount / 1024).coerceAtLeast(1)
            }
        }
    }

    private fun lockFor(key: String): Any = synchronized(decodeLocks) {
        decodeLocks.getOrPut(key) { Any() }
    }

    fun getCachedAtlas(assetPath: String, preferCompact: Boolean = false): Bitmap? {
        val atlasPath = ProVideoFolder.atlasPathFor(assetPath)
        val pinned = pinnedActiveBitmap
        if (pinned != null && !pinned.isRecycled && pinnedActiveAtlasPath == atlasPath) {
            return pinned
        }
        synchronized(memoryCache) {
            val full = memoryCache.get(atlasPath)
            if (full != null && !full.isRecycled) return full
        }
        if (preferCompact) {
            synchronized(compactMemoryCache) {
                val compact = compactMemoryCache.get(atlasPath)
                if (compact != null && !compact.isRecycled) return compact
            }
        }
        return null
    }

    fun getCachedDesignLayers(assetPath: String, @Suppress("UNUSED_PARAMETER") preferCompact: Boolean = false): ProDesignLayers? {
        val atlasPath = ProVideoFolder.atlasPathFor(assetPath)
        val pinned = pinnedActiveLayers
        if (pinned != null && !pinned.isRecycled && pinnedActiveAtlasPath == atlasPath) {
            return pinned
        }
        synchronized(layersCache) {
            val full = layersCache.get(atlasPath)
            if (full != null && !full.isRecycled) return full
        }
        return null
    }

    fun getOrDecodeAtlas(context: Context, assetPath: String, preferCompact: Boolean = false): Bitmap? {
        val item = ProVideoFolder.findItem(assetPath)
        val atlasPath = item?.atlasAssetPath ?: ProVideoFolder.atlasPathFor(assetPath)
        val pinned = pinnedActiveBitmap
        if (pinned != null && !pinned.isRecycled && pinnedActiveAtlasPath == atlasPath) {
            return pinned
        }
        synchronized(memoryCache) {
            val cached = memoryCache.get(atlasPath)
            if (cached != null && !cached.isRecycled) return cached
        }
        if (preferCompact) {
            synchronized(compactMemoryCache) {
                val cachedCompact = compactMemoryCache.get(atlasPath)
                if (cachedCompact != null && !cachedCompact.isRecycled) return cachedCompact
            }
        }

        val lockKey = if (preferCompact) "$atlasPath#compact" else atlasPath
        synchronized(lockFor(lockKey)) {
            synchronized(memoryCache) {
                val cached = memoryCache.get(atlasPath)
                if (cached != null && !cached.isRecycled) return cached
            }
            if (preferCompact) {
                synchronized(compactMemoryCache) {
                    val cachedCompact = compactMemoryCache.get(atlasPath)
                    if (cachedCompact != null && !cachedCompact.isRecycled) return cachedCompact
                }
            }
            val appCtx = context.applicationContext ?: context
            val sampleSize = if (preferCompact) 2 else 1
            val decoded = decodeAtlasFromAssets(appCtx, atlasPath, sampleSize = sampleSize)
            if (decoded != null) {
                runCatching { decoded.prepareToDraw() }
                if (preferCompact) {
                    synchronized(compactMemoryCache) {
                        compactMemoryCache.put(atlasPath, decoded)
                    }
                } else {
                    synchronized(memoryCache) {
                        memoryCache.put(atlasPath, decoded)
                    }
                }
            }
            return decoded
        }
    }

    fun getOrBuildDesignLayers(
        context: Context,
        assetPath: String
    ): ProDesignLayers? {
        val item = ProVideoFolder.findItem(assetPath)
        val atlasPath = item?.atlasAssetPath ?: ProVideoFolder.atlasPathFor(assetPath)
        getCachedDesignLayers(atlasPath)?.let { return it }
        val bmp = getOrDecodeAtlas(context, atlasPath, preferCompact = false) ?: return null
        return getOrBuildDesignLayers(atlasPath, bmp, item?.motionProfile ?: ProMotionProfile.HUD_RETICLE_CROSSHAIRS)
    }

    private fun getOrBuildDesignLayers(
        atlasPath: String,
        sourceBitmap: Bitmap,
        profile: ProMotionProfile
    ): ProDesignLayers? {
        if (sourceBitmap.isRecycled) return null
        synchronized(layersCache) {
            val existing = layersCache.get(atlasPath)
            if (existing != null && !existing.isRecycled) return existing
        }
        return runCatching {
            val layers = sliceIntoConcentricRings(sourceBitmap, profile)
            runCatching {
                layers.innerCore.prepareToDraw()
                layers.midRing.prepareToDraw()
                layers.outerRing.prepareToDraw()
            }
            synchronized(layersCache) {
                layersCache.put(atlasPath, layers)
            }
            layers
        }.getOrNull()
    }

    /**
     * Decomposes a single-frame HD design bitmap strictly along its archetype's natural empty radial gaps
     * (`wInner + wMid + wOuter == 1.0` at every pixel), or keeps it 100% unified when the archetype is a
     * continuous 1-body wave, lightning ring, starfield, or star-wave contour so the design is never torn.
     */
    private fun sliceIntoConcentricRings(src: Bitmap, profile: ProMotionProfile): ProDesignLayers {
        when (profile) {
            // 1-Layer Intact Continuous Designs: zero slicing so waves, lightning, stars & particles are 100% untouched
            ProMotionProfile.ELECTRIC_PLASMA_LIGHTNING,
            ProMotionProfile.CONTINUOUS_WAVE_TORUS,
            ProMotionProfile.ORBITAL_STARFIELD_RING,
            ProMotionProfile.STAR_WAVE_CONTOUR,
            ProMotionProfile.ATLAS_VIDEO -> {
                return ProDesignLayers(
                    fullBitmap = src,
                    innerCore = src,
                    midRing = src,
                    outerRing = src
                )
            }

            // 2-Layer Natural-Gap Designs: split cleanly at the exact empty radial valley between inner & outer structures
            ProMotionProfile.SPIROGRAPH_BRAID_TORUS,
            ProMotionProfile.POLYGON_APERTURE_IRIS,
            ProMotionProfile.EQUALIZER_WAVEFORM_HUD,
            ProMotionProfile.BRAIDED_LATTICE_DOTS -> {
                val (gapInner, gapOuter) = when (profile) {
                    ProMotionProfile.SPIROGRAPH_BRAID_TORUS -> Pair(0.51f, 0.55f)
                    ProMotionProfile.POLYGON_APERTURE_IRIS -> Pair(0.60f, 0.64f)
                    ProMotionProfile.EQUALIZER_WAVEFORM_HUD -> Pair(0.38f, 0.42f)
                    else -> Pair(0.79f, 0.83f) // BRAIDED_LATTICE_DOTS
                }
                val w = src.width.coerceAtLeast(1)
                val h = src.height.coerceAtLeast(1)
                val cx = w * 0.5f
                val cy = h * 0.5f
                val maxR = min(w, h) * 0.5f
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }

                val innerBmp = src.copy(Bitmap.Config.ARGB_8888, true)
                Canvas(innerBmp).apply {
                    maskPaint.shader = RadialGradient(
                        cx, cy, maxR,
                        intArrayOf(-0x1, -0x1, 0x00FFFFFF, 0x00FFFFFF),
                        floatArrayOf(0f, gapInner, gapOuter, 1f),
                        Shader.TileMode.CLAMP
                    )
                    drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)
                }

                val outerBmp = src.copy(Bitmap.Config.ARGB_8888, true)
                Canvas(outerBmp).apply {
                    maskPaint.shader = RadialGradient(
                        cx, cy, maxR,
                        intArrayOf(0x00FFFFFF, 0x00FFFFFF, -0x1, -0x1),
                        floatArrayOf(0f, gapInner, gapOuter, 1f),
                        Shader.TileMode.CLAMP
                    )
                    drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)
                }

                return ProDesignLayers(
                    fullBitmap = src,
                    innerCore = innerBmp,
                    midRing = innerBmp,
                    outerRing = outerBmp
                )
            }

            // 3-Layer Natural-Gap HUD Designs: split at the two natural empty gaps between inner, middle & outer HUD rings
            ProMotionProfile.SPEEDOMETER_HUD,
            ProMotionProfile.HUD_RETICLE_CROSSHAIRS,
            ProMotionProfile.CYBER_PCB_CIRCUIT -> {
                val r1: Float
                val r2: Float
                val r3: Float
                val r4: Float
                when (profile) {
                    ProMotionProfile.SPEEDOMETER_HUD -> {
                        r1 = 0.50f; r2 = 0.56f; r3 = 0.67f; r4 = 0.73f
                    }
                    ProMotionProfile.CYBER_PCB_CIRCUIT -> {
                        r1 = 0.42f; r2 = 0.46f; r3 = 0.57f; r4 = 0.61f
                    }
                    else -> { // HUD_RETICLE_CROSSHAIRS
                        r1 = 0.47f; r2 = 0.51f; r3 = 0.75f; r4 = 0.80f
                    }
                }

                val w = src.width.coerceAtLeast(1)
                val h = src.height.coerceAtLeast(1)
                val cx = w * 0.5f
                val cy = h * 0.5f
                val maxR = min(w, h) * 0.5f
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }

                val innerBmp = src.copy(Bitmap.Config.ARGB_8888, true)
                Canvas(innerBmp).apply {
                    maskPaint.shader = RadialGradient(
                        cx, cy, maxR,
                        intArrayOf(-0x1, -0x1, 0x00FFFFFF, 0x00FFFFFF),
                        floatArrayOf(0f, r1, r2, 1f),
                        Shader.TileMode.CLAMP
                    )
                    drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)
                }

                val midBmp = src.copy(Bitmap.Config.ARGB_8888, true)
                Canvas(midBmp).apply {
                    maskPaint.shader = RadialGradient(
                        cx, cy, maxR,
                        intArrayOf(0x00FFFFFF, 0x00FFFFFF, -0x1, -0x1, 0x00FFFFFF, 0x00FFFFFF),
                        floatArrayOf(0f, r1, r2, r3, r4, 1f),
                        Shader.TileMode.CLAMP
                    )
                    drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)
                }

                val outerBmp = src.copy(Bitmap.Config.ARGB_8888, true)
                Canvas(outerBmp).apply {
                    maskPaint.shader = RadialGradient(
                        cx, cy, maxR,
                        intArrayOf(0x00FFFFFF, 0x00FFFFFF, -0x1, -0x1),
                        floatArrayOf(0f, r3, r4, 1f),
                        Shader.TileMode.CLAMP
                    )
                    drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)
                }

                return ProDesignLayers(
                    fullBitmap = src,
                    innerCore = innerBmp,
                    midRing = midBmp,
                    outerRing = outerBmp
                )
            }
        }
    }

    private fun decodeAtlasFromAssets(context: Context, atlasPath: String, sampleSize: Int): Bitmap? {
        return try {
            context.assets.open(atlasPath).use { input ->
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = sampleSize.coerceAtLeast(1)
                    inScaled = false
                    inMutable = false
                }
                BitmapFactory.decodeStream(input, null, opts)
            }
        } catch (_: OutOfMemoryError) {
            synchronized(memoryCache) {
                memoryCache.evictAll()
            }
            synchronized(compactMemoryCache) {
                compactMemoryCache.evictAll()
            }
            synchronized(layersCache) {
                layersCache.evictAll()
            }
            if (sampleSize < 2) {
                runCatching {
                    context.assets.open(atlasPath).use { input ->
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inSampleSize = 2
                            inScaled = false
                            inMutable = false
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

    /**
     * Pins the active lock-screen/unlocked-screen Pro Video / Pro Design in memory at full HD resolution
     * (`inSampleSize = 1`) so fingerprint sensor touch triggers in `<1ms`.
     */
    fun pinActiveOverlayAtlas(context: Context, assetPath: String?) {
        if (assetPath.isNullOrBlank()) {
            pinnedActiveAtlasPath = null
            pinnedActiveBitmap = null
            pinnedActiveLayers = null
            return
        }
        val item = ProVideoFolder.findItem(assetPath)
        val atlasPath = item?.atlasAssetPath ?: ProVideoFolder.atlasPathFor(assetPath)
        val existingPinned = pinnedActiveBitmap
        if (pinnedActiveAtlasPath == atlasPath && existingPinned != null && !existingPinned.isRecycled) {
            return
        }
        val appCtx = context.applicationContext ?: context
        priorityOverlayExecutor.execute {
            val bmp = getOrDecodeAtlas(appCtx, atlasPath, preferCompact = false)
            if (bmp != null && !bmp.isRecycled) {
                runCatching { bmp.prepareToDraw() }
                pinnedActiveAtlasPath = atlasPath
                pinnedActiveBitmap = bmp
                if (item != null && !item.isAtlasGrid) {
                    pinnedActiveLayers = getOrBuildDesignLayers(atlasPath, bmp, item.motionProfile)
                } else {
                    pinnedActiveLayers = null
                }
            }
        }
    }

    fun prewarm(context: Context, specificAssetPath: String? = null) {
        prewarmAsync(context, specificAssetPath = specificAssetPath, preferCompact = false)
    }

    fun prewarmAsync(
        context: Context,
        specificAssetPath: String? = null,
        preferCompact: Boolean = false,
        onReady: ((Bitmap?) -> Unit)? = null
    ) {
        val appCtx = context.applicationContext ?: context
        val isActivePrewarm = specificAssetPath == null
        val targetPath = specificAssetPath ?: Prefs.getSelectedVideoAsset(appCtx)
        val item = ProVideoFolder.findItem(targetPath)
        val atlasPath = item?.atlasAssetPath ?: ProVideoFolder.atlasPathFor(targetPath)

        val already = getCachedAtlas(atlasPath, preferCompact = preferCompact)
        if (already != null) {
            if (isActivePrewarm) {
                pinnedActiveAtlasPath = atlasPath
                pinnedActiveBitmap = already
                if (item != null && !item.isAtlasGrid && pinnedActiveLayers == null) {
                    decodeExecutor.execute {
                        pinnedActiveLayers = getOrBuildDesignLayers(atlasPath, already, item.motionProfile)
                    }
                }
            }
            onReady?.invoke(already)
            return
        }

        val inflightKey = if (preferCompact) "$atlasPath#compact" else atlasPath
        synchronized(inFlightCallbacks) {
            val existingList = inFlightCallbacks[inflightKey]
            if (existingList != null) {
                if (onReady != null) existingList.add(onReady)
                return
            }
            val newList = mutableListOf<(Bitmap?) -> Unit>()
            if (onReady != null) newList.add(onReady)
            inFlightCallbacks[inflightKey] = newList
        }

        val executor = if (isActivePrewarm || !preferCompact) priorityOverlayExecutor else decodeExecutor
        executor.execute {
            val bmp = getOrDecodeAtlas(appCtx, atlasPath, preferCompact = preferCompact)
            if (bmp != null && !preferCompact && (isActivePrewarm || Prefs.getSelectedVideoAsset(appCtx) == targetPath)) {
                runCatching { bmp.prepareToDraw() }
                pinnedActiveAtlasPath = atlasPath
                pinnedActiveBitmap = bmp
                if (item != null && !item.isAtlasGrid) {
                    pinnedActiveLayers = getOrBuildDesignLayers(atlasPath, bmp, item.motionProfile)
                }
            }
            val callbacks = synchronized(inFlightCallbacks) {
                inFlightCallbacks.remove(inflightKey)
            }
            if (!callbacks.isNullOrEmpty()) {
                mainHandler.post {
                    for (cb in callbacks) {
                        cb.invoke(bmp)
                    }
                }
            }
        }
    }

    fun prewarmGalleryThumbnailsAsync(context: Context, count: Int = 8) {
        val appCtx = context.applicationContext ?: context
        val subset = ProVideoFolder.items.take(count)
        for (item in subset) {
            if (getCachedAtlas(item.atlasAssetPath, preferCompact = true) == null) {
                prewarmAsync(appCtx, specificAssetPath = item.assetPath, preferCompact = true)
            }
        }
    }
}

/**
 * Hardware-accelerated, zero-black-background HD animation view that renders:
 * 1. The 14 uploaded `.mp4` Pro Videos (`8x8` 1120x1120 HD frame atlases), AND
 * 2. All 84 high-res `512x512` HD Real & PDF HUD Designs at native **60/120fps** using archetype-aware
 *    motion (keeping continuous waves, spirographs, lightning, stars & polygons 100% intact while
 *    counter-rotating multi-ring HUDs strictly along their natural empty radial gaps).
 */
class LoopingAssetVideoView(context: Context) : View(context), Choreographer.FrameCallback {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val srcRect = Rect()
    private val fullBmpSrcRect = Rect()
    private val dstRect = RectF()

    // Hardware clip paths aligned with natural empty radial gaps when layers are not yet cached
    private val hudOuterClipPath = android.graphics.Path()
    private val hudMidClipPath = android.graphics.Path()
    private val hudInnerClipPath = android.graphics.Path()

    private val pcbOuterClipPath = android.graphics.Path()
    private val pcbMidClipPath = android.graphics.Path()
    private val pcbInnerClipPath = android.graphics.Path()

    private val speedoOuterClipPath = android.graphics.Path()
    private val speedoMidClipPath = android.graphics.Path()
    private val speedoInnerClipPath = android.graphics.Path()

    private val spiroInnerClipPath = android.graphics.Path()
    private val spiroOuterClipPath = android.graphics.Path()

    private val polyInnerClipPath = android.graphics.Path()
    private val polyOuterClipPath = android.graphics.Path()

    private val eqInnerClipPath = android.graphics.Path()
    private val eqOuterClipPath = android.graphics.Path()

    private val latticeInnerClipPath = android.graphics.Path()
    private val latticeOuterClipPath = android.graphics.Path()

    var preferCompactDecode: Boolean = false

    private var loadedAssetPath: String? = null
    private var loadedItem: ProVideoItem? = null
    private var atlasBitmap: Bitmap? = null
    private var designLayers: ProDesignLayers? = null
    private var isPlaying = false
    private var desiredSpeed: Float = 1.0f
    private var currentFrameFloat: Float = 0f
    private var elapsedAnimSeconds: Float = 0f
    private var lastFrameTimeNanos: Long = 0L
    private var choreographerScheduled = false

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDstRect(w, h)
    }

    private fun buildTwoZoneClipPaths(
        cx: Float,
        cy: Float,
        r: Float,
        splitFraction: Float,
        innerPath: android.graphics.Path,
        outerPath: android.graphics.Path
    ) {
        innerPath.reset()
        innerPath.addCircle(cx, cy, r * splitFraction, android.graphics.Path.Direction.CW)
        outerPath.reset()
        outerPath.addCircle(cx, cy, r * 1.05f, android.graphics.Path.Direction.CW)
        outerPath.op(innerPath, android.graphics.Path.Op.DIFFERENCE)
    }

    private fun buildThreeZoneClipPaths(
        cx: Float,
        cy: Float,
        r: Float,
        split1: Float,
        split2: Float,
        innerPath: android.graphics.Path,
        midPath: android.graphics.Path,
        outerPath: android.graphics.Path
    ) {
        innerPath.reset()
        innerPath.addCircle(cx, cy, r * split1, android.graphics.Path.Direction.CW)

        midPath.reset()
        midPath.addCircle(cx, cy, r * split2, android.graphics.Path.Direction.CW)
        midPath.op(innerPath, android.graphics.Path.Op.DIFFERENCE)

        val midOuterUnion = android.graphics.Path()
        midOuterUnion.addCircle(cx, cy, r * split2, android.graphics.Path.Direction.CW)
        outerPath.reset()
        outerPath.addCircle(cx, cy, r * 1.05f, android.graphics.Path.Direction.CW)
        outerPath.op(midOuterUnion, android.graphics.Path.Op.DIFFERENCE)
    }

    private fun updateDstRect(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val d = minOf(w, h).toFloat()
        val left = (w - d) / 2f
        val top = (h - d) / 2f
        dstRect.set(left, top, left + d, top + d)
        val cx = dstRect.centerX()
        val cy = dstRect.centerY()
        val r = d * 0.5f

        buildThreeZoneClipPaths(cx, cy, r, 0.49f, 0.78f, hudInnerClipPath, hudMidClipPath, hudOuterClipPath)
        buildThreeZoneClipPaths(cx, cy, r, 0.44f, 0.59f, pcbInnerClipPath, pcbMidClipPath, pcbOuterClipPath)
        buildThreeZoneClipPaths(cx, cy, r, 0.53f, 0.70f, speedoInnerClipPath, speedoMidClipPath, speedoOuterClipPath)

        buildTwoZoneClipPaths(cx, cy, r, 0.53f, spiroInnerClipPath, spiroOuterClipPath)
        buildTwoZoneClipPaths(cx, cy, r, 0.62f, polyInnerClipPath, polyOuterClipPath)
        buildTwoZoneClipPaths(cx, cy, r, 0.40f, eqInnerClipPath, eqOuterClipPath)
        buildTwoZoneClipPaths(cx, cy, r, 0.81f, latticeInnerClipPath, latticeOuterClipPath)
    }

    fun setVideoAsset(
        assetPath: String,
        autoStart: Boolean,
        speed: Float = 1.0f,
        preferCompact: Boolean = preferCompactDecode
    ) {
        preferCompactDecode = preferCompact
        desiredSpeed = speed.coerceIn(0.25f, 8.0f)
        val pathChanged = loadedAssetPath != assetPath
        loadedAssetPath = assetPath
        loadedItem = ProVideoFolder.findItem(assetPath)

        if (pathChanged || atlasBitmap == null || atlasBitmap?.isRecycled == true) {
            designLayers = if (preferCompact) null else ProVideoFrameCache.getCachedDesignLayers(assetPath)
            val cached = ProVideoFrameCache.getCachedAtlas(assetPath, preferCompact = preferCompact)
            if (cached != null) {
                atlasBitmap = cached
                invalidate()
            } else {
                ProVideoFrameCache.prewarmAsync(
                    context = context,
                    specificAssetPath = assetPath,
                    preferCompact = preferCompact
                ) { decoded ->
                    if (loadedAssetPath == assetPath && decoded != null) {
                        atlasBitmap = decoded
                        if (!preferCompact) {
                            designLayers = ProVideoFrameCache.getCachedDesignLayers(assetPath)
                        }
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
        desiredSpeed = speed.coerceIn(0.25f, 8.0f)
    }

    fun startImmediate() {
        isPlaying = true
        val path = loadedAssetPath
        if ((atlasBitmap == null || atlasBitmap?.isRecycled == true) && path != null) {
            designLayers = if (preferCompactDecode) null else ProVideoFrameCache.getCachedDesignLayers(path)
            val cached = ProVideoFrameCache.getCachedAtlas(path, preferCompact = preferCompactDecode)
            if (cached != null) {
                atlasBitmap = cached
            } else {
                ProVideoFrameCache.prewarmAsync(
                    context = context,
                    specificAssetPath = path,
                    preferCompact = preferCompactDecode
                ) { decoded ->
                    if (loadedAssetPath == path && decoded != null) {
                        atlasBitmap = decoded
                        if (!preferCompactDecode) {
                            designLayers = ProVideoFrameCache.getCachedDesignLayers(path)
                        }
                        invalidate()
                        if (isPlaying) scheduleNextFrame()
                    }
                }
            }
        }
        if (lastFrameTimeNanos == 0L) {
            scheduleNextFrame()
            invalidate()
        } else {
            scheduleNextFrame()
        }
    }

    fun pauseAndReset() {
        val wasPlaying = isPlaying
        isPlaying = false
        currentFrameFloat = 0f
        elapsedAnimSeconds = 0f
        lastFrameTimeNanos = 0L
        unscheduleFrameCallback()
        if (wasPlaying) {
            invalidate()
        }
    }

    fun primeAtFrameZero() {
        currentFrameFloat = 0f
        elapsedAnimSeconds = 0f
        lastFrameTimeNanos = 0L
        val path = loadedAssetPath
        if ((atlasBitmap == null || atlasBitmap?.isRecycled == true) && path != null) {
            designLayers = if (preferCompactDecode) null else ProVideoFrameCache.getCachedDesignLayers(path)
            val cached = ProVideoFrameCache.getCachedAtlas(path, preferCompact = preferCompactDecode)
            if (cached != null) {
                atlasBitmap = cached
            } else {
                ProVideoFrameCache.prewarmAsync(
                    context = context,
                    specificAssetPath = path,
                    preferCompact = preferCompactDecode
                ) { decoded ->
                    if (loadedAssetPath == path && decoded != null) {
                        atlasBitmap = decoded
                        if (!preferCompactDecode) {
                            designLayers = ProVideoFrameCache.getCachedDesignLayers(path)
                        }
                        invalidate()
                    }
                }
            }
        }
        invalidate()
    }

    override fun doFrame(frameTimeNanos: Long) {
        choreographerScheduled = false
        if (!isPlaying || !isAttachedToWindow || visibility != VISIBLE || alpha <= 0.001f) {
            lastFrameTimeNanos = 0L
            return
        }

        if (lastFrameTimeNanos == 0L) {
            lastFrameTimeNanos = frameTimeNanos
        } else {
            val deltaSec = ((frameTimeNanos - lastFrameTimeNanos).coerceAtLeast(0L)) / 1_000_000_000f
            lastFrameTimeNanos = frameTimeNanos
            val clampedDelta = deltaSec.coerceAtMost(0.08f)
            val scaledDelta = clampedDelta * desiredSpeed
            elapsedAnimSeconds = (elapsedAnimSeconds + scaledDelta) % 3600f
            val advance = scaledDelta * ProVideoFrameCache.BASE_FPS
            currentFrameFloat = (currentFrameFloat + advance) % ProVideoFrameCache.TOTAL_FRAMES
        }

        invalidate()
        scheduleNextFrame()
    }

    private fun scheduleNextFrame() {
        if (!choreographerScheduled && isPlaying && isAttachedToWindow && visibility == VISIBLE && alpha > 0.001f) {
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

    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        if (alpha <= 0.001f) {
            unscheduleFrameCallback()
        } else if (isPlaying && isAttachedToWindow && visibility == VISIBLE) {
            lastFrameTimeNanos = 0L
            scheduleNextFrame()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlaying && visibility == VISIBLE && alpha > 0.001f) {
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
        if (visibility == VISIBLE && isPlaying && alpha > 0.001f) {
            lastFrameTimeNanos = 0L
            scheduleNextFrame()
        } else if (visibility != VISIBLE) {
            unscheduleFrameCallback()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (alpha <= 0.001f) return
        val path = loadedAssetPath ?: return
        val bmp = atlasBitmap ?: ProVideoFrameCache.getCachedAtlas(path, preferCompact = preferCompactDecode)?.also {
            atlasBitmap = it
        } ?: return

        if (bmp.isRecycled || width <= 0 || height <= 0) return

        if (dstRect.isEmpty) {
            updateDstRect(width, height)
        }

        val item = loadedItem ?: ProVideoFolder.findItem(path).also { loadedItem = it }
        val isGridAtlas = item?.isAtlasGrid ?: (bmp.width >= 480 && item == null)

        if (isGridAtlas) {
            drawMultiFrameVideoAtlas(canvas, bmp)
        } else {
            drawLiveRealDesign(canvas, bmp, item)
        }
    }

    private fun drawMultiFrameVideoAtlas(canvas: Canvas, bmp: Bitmap) {
        val cellW = bmp.width / ProVideoFrameCache.ATLAS_COLS
        val cellH = bmp.height / ProVideoFrameCache.ATLAS_ROWS
        if (cellW <= 0 || cellH <= 0) return

        val frameIdx = (currentFrameFloat.toInt() % ProVideoFrameCache.TOTAL_FRAMES)
            .coerceIn(0, ProVideoFrameCache.TOTAL_FRAMES - 1)
        val col = frameIdx % ProVideoFrameCache.ATLAS_COLS
        val row = frameIdx / ProVideoFrameCache.ATLAS_COLS
        srcRect.set(col * cellW, row * cellH, (col + 1) * cellW, (row + 1) * cellH)

        basePaint.alpha = 255
        canvas.drawBitmap(bmp, srcRect, dstRect, basePaint)
    }

    private fun drawIntactWholeDesign(
        canvas: Canvas,
        bmp: Bitmap,
        cx: Float,
        cy: Float,
        angleDeg: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        canvas.save()
        if (angleDeg != 0f) {
            canvas.rotate(angleDeg, cx, cy)
        }
        if (scaleX != 1f || scaleY != 1f) {
            canvas.scale(scaleX, scaleY, cx, cy)
        }
        fullBmpSrcRect.set(0, 0, bmp.width, bmp.height)
        basePaint.alpha = 255
        canvas.drawBitmap(bmp, fullBmpSrcRect, dstRect, basePaint)
        canvas.restore()
    }

    private fun drawRingLayer(
        canvas: Canvas,
        ringBmp: Bitmap,
        cx: Float,
        cy: Float,
        angleDeg: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        canvas.save()
        if (angleDeg != 0f) {
            canvas.rotate(angleDeg, cx, cy)
        }
        if (scaleX != 1f || scaleY != 1f) {
            canvas.scale(scaleX, scaleY, cx, cy)
        }
        fullBmpSrcRect.set(0, 0, ringBmp.width, ringBmp.height)
        basePaint.alpha = 255
        canvas.drawBitmap(ringBmp, fullBmpSrcRect, dstRect, basePaint)
        canvas.restore()
    }

    private fun drawLiveRealDesign(canvas: Canvas, bmp: Bitmap, item: ProVideoItem?) {
        val cx = dstRect.centerX()
        val cy = dstRect.centerY()
        val t = elapsedAnimSeconds

        // Fast single-draw path for static gallery thumbnails or when pre-sliced layers are not yet ready:
        // avoids 3x full-quad overdraw and keeps scrolling at a locked 60/120fps.
        if (!isPlaying || preferCompactDecode) {
            drawIntactWholeDesign(canvas, bmp, cx, cy, 0f, 1f, 1f)
            return
        }

        val path = loadedAssetPath
        var layers = designLayers
        if ((layers == null || layers.isRecycled) && path != null) {
            layers = ProVideoFrameCache.getCachedDesignLayers(path)
            designLayers = layers
        }

        val useLayers = layers?.takeIf { !it.isRecycled }
        val radius = dstRect.width() * 0.5f
        val idxSeed = (item?.index ?: 1) * 0.37f
        val profile = item?.motionProfile ?: ProMotionProfile.HUD_RETICLE_CROSSHAIRS
        val dir = if ((item?.index ?: 0) % 2 == 0) 1f else -1f

        // If multi-ring layers haven't finished pre-slicing on the background thread yet,
        // smoothly rotate the whole intact bitmap in a single pass instead of drawing 3 unclipped copies.
        if (useLayers == null) {
            val mainAng = (t * 48f * dir) % 360f
            val breath = 1.0f + 0.025f * sin(t * 3.2f + idxSeed)
            drawIntactWholeDesign(canvas, bmp, cx, cy, mainAng, breath, breath)
            return
        }

        when (profile) {
            ProMotionProfile.SPEEDOMETER_HUD -> {
                val wingSway = sin(t * 2.3f) * 8.0f
                val wingScale = 1.0f + 0.02f * cos(t * 2.8f)
                drawRingLayer(canvas, useLayers.outerRing, cx, cy, wingSway, wingScale, wingScale)

                val plasmaAngle = (t * 88f) % 360f
                val plasmaPulse = 1.0f + 0.025f * sin(t * 4.5f)
                drawRingLayer(canvas, useLayers.midRing, cx, cy, plasmaAngle, plasmaPulse, plasmaPulse)

                val dialTilt = sin(t * 1.9f) * 2.0f
                val dialBreath = 1.0f + 0.012f * sin(t * 3.6f)
                drawRingLayer(canvas, useLayers.innerCore, cx, cy, dialTilt, dialBreath, dialBreath)

                val revWave = 0.5f + 0.5f * sin(t * 2.5f + sin(t * 1.3f) * 0.6f)
                val needleDeg = 145f + revWave * 245f
                val needleRad = Math.toRadians(needleDeg.toDouble())
                val nLen = radius * 0.42f
                val nx = cx + cos(needleRad).toFloat() * nLen
                val ny = cy + sin(needleRad).toFloat() * nLen
                needlePaint.color = Color.argb(235, 255, 60, 140)
                needlePaint.strokeWidth = (radius * 0.024f).coerceAtLeast(2f)
                canvas.drawLine(cx, cy, nx, ny, needlePaint)
            }

            ProMotionProfile.ELECTRIC_PLASMA_LIGHTNING -> {
                val mainAng = (t * 58f * dir + sin(t * 3.8f + idxSeed) * 6f) % 360f
                val pulseScale = 1.0f + 0.032f * sin(t * 5.4f + idxSeed)
                drawIntactWholeDesign(canvas, bmp, cx, cy, mainAng, pulseScale, pulseScale)
            }

            ProMotionProfile.CONTINUOUS_WAVE_TORUS -> {
                val mainAng = (t * 48f * dir) % 360f
                val sx = 1.0f + 0.028f * sin(t * 2.8f + idxSeed)
                val sy = 1.0f + 0.028f * cos(t * 2.8f + idxSeed)
                drawIntactWholeDesign(canvas, bmp, cx, cy, mainAng, sx, sy)
            }

            ProMotionProfile.ORBITAL_STARFIELD_RING -> {
                val mainAng = (t * 38f * dir) % 360f
                val breath = 1.0f + 0.024f * sin(t * 3.1f + idxSeed)
                drawIntactWholeDesign(canvas, bmp, cx, cy, mainAng, breath, breath)
            }

            ProMotionProfile.STAR_WAVE_CONTOUR -> {
                val mainAng = (t * 42f * dir + sin(t * 1.8f + idxSeed) * 8f) % 360f
                val starBreath = 1.0f + 0.030f * sin(t * 3.4f + idxSeed)
                drawIntactWholeDesign(canvas, bmp, cx, cy, mainAng, starBreath, starBreath)
            }

            ProMotionProfile.SPIROGRAPH_BRAID_TORUS -> {
                val outerAng = (t * 44f * dir) % 360f
                val innerAng = (t * -72f * dir) % 360f
                val outerScale = 1.0f + 0.022f * sin(t * 2.9f + idxSeed)
                val innerScale = 1.0f + 0.035f * cos(t * 3.7f + idxSeed)
                drawRingLayer(canvas, useLayers.outerRing, cx, cy, outerAng, outerScale, outerScale)
                drawRingLayer(canvas, useLayers.innerCore, cx, cy, innerAng, innerScale, innerScale)
            }

            ProMotionProfile.POLYGON_APERTURE_IRIS -> {
                val outerDotsAng = (t * -34f * dir) % 360f
                val irisAng = (t * 52f * dir + sin(t * 2.0f + idxSeed) * 10f) % 360f
                val outerScale = 1.0f + 0.018f * cos(t * 2.4f + idxSeed)
                val irisScale = 1.0f + 0.034f * sin(t * 3.5f + idxSeed)
                drawRingLayer(canvas, useLayers.outerRing, cx, cy, outerDotsAng, outerScale, outerScale)
                drawRingLayer(canvas, useLayers.innerCore, cx, cy, irisAng, irisScale, irisScale)
            }

            ProMotionProfile.EQUALIZER_WAVEFORM_HUD -> {
                val eqSunburstAng = (t * 50f * dir) % 360f
                val eqBeatScale = 1.0f + 0.032f * sin(t * 5.6f + idxSeed)
                val waveTilt = sin(t * 2.2f + idxSeed) * 3.5f
                val waveSx = 1.0f + 0.015f * cos(t * 4.2f + idxSeed)
                val waveSy = 1.0f + 0.062f * sin(t * 6.8f + idxSeed)
                drawRingLayer(canvas, useLayers.outerRing, cx, cy, eqSunburstAng, eqBeatScale, eqBeatScale)
                drawRingLayer(canvas, useLayers.innerCore, cx, cy, waveTilt, waveSx, waveSy)
            }

            ProMotionProfile.BRAIDED_LATTICE_DOTS -> {
                val outerDotsAng = (t * -38f * dir) % 360f
                val latticeAng = (t * 54f * dir) % 360f
                val outerScale = 1.0f + 0.020f * cos(t * 2.6f + idxSeed)
                val latticeScale = 1.0f + 0.028f * sin(t * 3.3f + idxSeed)
                drawRingLayer(canvas, useLayers.outerRing, cx, cy, outerDotsAng, outerScale, outerScale)
                drawRingLayer(canvas, useLayers.innerCore, cx, cy, latticeAng, latticeScale, latticeScale)
            }

            ProMotionProfile.CYBER_PCB_CIRCUIT -> {
                val outerAng = (t * 32f * dir) % 360f
                val midAng = (t * -56f * dir) % 360f
                val innerAng = (t * 78f * dir) % 360f
                val outerScale = 1.0f + 0.018f * sin(t * 2.3f + idxSeed)
                val midScale = 1.0f + 0.026f * cos(t * 3.1f + idxSeed)
                val innerScale = 1.0f + 0.034f * sin(t * 4.0f + idxSeed)
                drawRingLayer(canvas, useLayers.outerRing, cx, cy, outerAng, outerScale, outerScale)
                drawRingLayer(canvas, useLayers.midRing, cx, cy, midAng, midScale, midScale)
                drawRingLayer(canvas, useLayers.innerCore, cx, cy, innerAng, innerScale, innerScale)
            }

            ProMotionProfile.HUD_RETICLE_CROSSHAIRS,
            ProMotionProfile.ATLAS_VIDEO -> {
                val outerAng = (t * 28f * dir + sin(t * 1.5f + idxSeed) * 6f) % 360f
                val midAng = (t * -54f * dir) % 360f
                val innerAng = (t * 72f * dir) % 360f
                val outerScale = 1.0f + 0.020f * sin(t * 2.2f + idxSeed)
                val midScale = 1.0f + 0.028f * cos(t * 2.9f + idxSeed)
                val innerScale = 1.0f + 0.036f * sin(t * 3.7f + idxSeed)
                drawRingLayer(canvas, useLayers.outerRing, cx, cy, outerAng, outerScale, outerScale)
                drawRingLayer(canvas, useLayers.midRing, cx, cy, midAng, midScale, midScale)
                drawRingLayer(canvas, useLayers.innerCore, cx, cy, innerAng, innerScale, innerScale)
            }
        }
    }

    fun releaseAll() {
        pauseAndReset()
        atlasBitmap = null
        designLayers = null
    }
}

/**
 * Unified overlay & preview container that hosts both the pre-warmed [LottieAnimationView] and the
 * transparent-background [LoopingAssetVideoView] so the accessibility service and preview screens
 * can render either a video/design from the Pro Videos folder or a Lottie animation seamlessly.
 */
@Suppress("DEPRECATION")
class OverlayAnimHostView(context: Context) : FrameLayout(context) {

    val lottieView: LottieAnimationView = LottieAnimationView(
        android.view.ContextThemeWrapper(context, R.style.Theme_MyApplication)
    ).apply {
        setRenderMode(RenderMode.HARDWARE)
        setCacheComposition(true)
        setIgnoreDisabledSystemAnimations(true)
        enableMergePathsForKitKatAndAbove(true)
        repeatCount = LottieDrawable.INFINITE
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)
    }

    val videoView: LoopingAssetVideoView = LoopingAssetVideoView(context).apply {
        preferCompactDecode = false
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)
        visibility = View.GONE
    }

    var isVideoMode: Boolean = false
        private set

    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(lottieView)
        addView(videoView)
    }

    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        videoView.alpha = alpha
        if (alpha <= 0.001f && !isVideoMode && lottieView.isAnimating) {
            lottieView.pauseAnimation()
        }
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
            ProVideoFrameCache.pinActiveOverlayAtlas(context, videoAsset)
            lottieView.pauseAnimation()
            lottieView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            videoView.setVideoAsset(videoAsset, autoStart = autoPlay, speed = speed, preferCompact = false)
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
        if (alpha <= 0.001f) {
            alpha = 1f
        }
        if (isVideoMode) {
            if (videoView.visibility != View.VISIBLE) videoView.visibility = View.VISIBLE
            videoView.alpha = 1f
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

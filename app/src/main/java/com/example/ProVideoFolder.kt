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
    STAR_RADAR_GRID,
    CYBER_SEGMENTED_HUD,
    ARMILLARY_3D_GYRO,
    PLASMA_VORTEX_PORTAL
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
    val motionProfile: ProMotionProfile = ProMotionProfile.CYBER_SEGMENTED_HUD
)

/**
 * Pre-decomposed concentric ring bitmaps of a real uploaded design (`innerCore + midRing + outerRing == original`).
 * Created once on the background decode thread using smooth [RadialGradient] + [PorterDuff.Mode.DST_IN] feathering
 * and uploaded to the GPU via [Bitmap.prepareToDraw], enabling 120fps multi-ring counter-rotation and additive
 * HDR bloom in [LoopingAssetVideoView.onDraw] with zero per-frame allocations or clip-path overhead.
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
        get() = ((fullBitmap.allocationByteCount +
            innerCore.allocationByteCount +
            midRing.allocationByteCount +
            outerRing.allocationByteCount) / 1024).coerceAtLeast(1)
}

object ProVideoFolder {
    const val FOLDER_PATH = "pro_videos"
    const val FOLDER_TITLE = "Pro Video & Real Designs Folder"

    private data class ProDesignSpec(
        val designNum: Int,
        val slug: String,
        val title: String,
        val subtitle: String,
        val badge: String,
        val accentHex: Long,
        val profile: ProMotionProfile
    )

    private val generatedProDesignSpecs: List<ProDesignSpec> by lazy {
        val themeNames = arrayOf(
            "Quantum Cyber HUD",
            "Plasma Vortex Ring",
            "Biometric Star Radar",
            "3D Armillary Gyro",
            "Ultraviolet Neon Portal",
            "Emerald Matrix Scanner",
            "Solar Corona Reactor",
            "Cryo Filament Helix"
        )
        val themeSubtitles = arrayOf(
            "Counter-rotating cyan HUD vernier bezel, segmented data arcs & biometric ridge core",
            "Swirling neon magenta-cyan plasma filaments with counter-spinning optical vortex core",
            "Precision polar radar grid with pulsing perimeter star-nodes & live biometric scanner",
            "3D gimballed golden-cyan orbital rings tumbling around a luminous biometric sensor core",
            "Electric violet & hot-magenta concentric portal rings with harmonic optical shockwaves",
            "Bioluminescent emerald dot-matrix telemetry bezel with high-speed counter-rotating ticks",
            "Blazing solar-gold & cobalt reactor teeth with multi-ring optical laser interference",
            "Crystalline ice-blue refractive light filaments & counter-spinning cryo biometric core"
        )
        val themeHexes = longArrayOf(
            0xFF00E5FFL,
            0xFFFF4081L,
            0xFF2979FFL,
            0xFFFFD740L,
            0xFFB388FFL,
            0xFF00E676L,
            0xFFFF9100L,
            0xFF18FFFFL
        )
        val themeProfiles = arrayOf(
            ProMotionProfile.CYBER_SEGMENTED_HUD,
            ProMotionProfile.PLASMA_VORTEX_PORTAL,
            ProMotionProfile.STAR_RADAR_GRID,
            ProMotionProfile.ARMILLARY_3D_GYRO,
            ProMotionProfile.CYBER_SEGMENTED_HUD,
            ProMotionProfile.CYBER_SEGMENTED_HUD,
            ProMotionProfile.ARMILLARY_3D_GYRO,
            ProMotionProfile.PLASMA_VORTEX_PORTAL
        )

        (1..100).map { dNum ->
            when (dNum) {
                1 -> ProDesignSpec(
                    designNum = 1,
                    slug = "design_001_star_radar",
                    title = "Sapphire 12-Spoke Starlight Radar",
                    subtitle = "Exact uploaded design_001: 12-spoke sapphire radar grid, perimeter star flares & blazing cyan core",
                    badge = "Uploaded Design #001",
                    accentHex = 0xFF00B0FFL,
                    profile = ProMotionProfile.STAR_RADAR_GRID
                )
                2 -> ProDesignSpec(
                    designNum = 2,
                    slug = "design_002_cyan_segmented_hud",
                    title = "Electric Cyan Segmented HUD Ring",
                    subtitle = "Exact uploaded design_002: counter-rotating cyan HUD blocks, 36-dot inner ring & 4 glowing nodes",
                    badge = "Uploaded Design #002",
                    accentHex = 0xFF00E5FFL,
                    profile = ProMotionProfile.CYBER_SEGMENTED_HUD
                )
                3 -> ProDesignSpec(
                    designNum = 3,
                    slug = "design_003_violet_concentric_hud",
                    title = "Ultraviolet Concentric Vernier HUD",
                    subtitle = "Exact uploaded design_003: neon purple-violet concentric HUD rings, cardinal crosshairs & dot bezel",
                    badge = "Uploaded Design #003",
                    accentHex = 0xFFB388FFL,
                    profile = ProMotionProfile.CYBER_SEGMENTED_HUD
                )
                4 -> ProDesignSpec(
                    designNum = 4,
                    slug = "design_004_golden_armillary_gyro",
                    title = "Golden 3D Armillary Gyroscope Sphere",
                    subtitle = "Exact uploaded design_004: 3D tilted golden orbital ribbons, 4 metallic cardinal spheres & solar core",
                    badge = "Uploaded Design #004",
                    accentHex = 0xFFFFD740L,
                    profile = ProMotionProfile.ARMILLARY_3D_GYRO
                )
                97 -> ProDesignSpec(
                    designNum = 97,
                    slug = "design_097_mechanical_iris",
                    title = "Mechanical 12-Blade Aperture Iris",
                    subtitle = "Exact uploaded design_097: precision gunmetal & cyan 12-blade camera iris aperture with glowing core",
                    badge = "Uploaded Design #097",
                    accentHex = 0xFF00E5FFL,
                    profile = ProMotionProfile.CYBER_SEGMENTED_HUD
                )
                98 -> ProDesignSpec(
                    designNum = 98,
                    slug = "design_098_violet_ribbon_portal",
                    title = "Violet & Magenta Silk Ribbon Portal",
                    subtitle = "Exact uploaded design_098: flowing 3D violet, magenta & cyan silk light waves swirling around core",
                    badge = "Uploaded Design #098",
                    accentHex = 0xFFE040FBL,
                    profile = ProMotionProfile.PLASMA_VORTEX_PORTAL
                )
                99 -> ProDesignSpec(
                    designNum = 99,
                    slug = "design_099_emerald_particle_portal",
                    title = "Bioluminescent Emerald Particle Portal",
                    subtitle = "Exact uploaded design_099: golden-ratio spiral of glowing emerald & mint quantum particles",
                    badge = "Uploaded Design #099",
                    accentHex = 0xFF00E676L,
                    profile = ProMotionProfile.PLASMA_VORTEX_PORTAL
                )
                100 -> ProDesignSpec(
                    designNum = 100,
                    slug = "design_100_icy_filament_vortex",
                    title = "Cryo-Cyan Icy Filament Vortex",
                    subtitle = "Exact uploaded design_100: high-velocity ice-blue & white curved radial vortex filaments",
                    badge = "Uploaded Design #100",
                    accentHex = 0xFF80D8FFL,
                    profile = ProMotionProfile.PLASMA_VORTEX_PORTAL
                )
                else -> {
                    val tIdx = (dNum - 5) % themeNames.size
                    val numStr = String.format(Locale.US, "%03d", dNum)
                    ProDesignSpec(
                        designNum = dNum,
                        slug = "design_$numStr",
                        title = "Pro ${themeNames[tIdx]} #$numStr",
                        subtitle = themeSubtitles[tIdx],
                        badge = "Uploaded Design #$numStr",
                        accentHex = themeHexes[tIdx],
                        profile = themeProfiles[tIdx]
                    )
                }
            }
        }
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
                id = "folder_video_9_cyber_tachometer_overdrive",
                index = 9,
                title = "Cyber Tachometer Overdrive Dual-Spin",
                subtitle = "Exact uploaded HUD dial with high-speed counter-rotating bezel, turbo red ring & needle sweep",
                badge = "Real Design #9",
                assetPath = "pro_videos/video_9_cyber_tachometer_overdrive.mp4",
                atlasAssetPath = "pro_videos/video_9_cyber_tachometer_overdrive.webp",
                accentHex = 0xFFFF3D00L,
                fallbackLottieDesignId = "pro_video_9_cyber_tachometer_overdrive",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.SPEEDOMETER_HUD
            ),
            ProVideoItem(
                id = "folder_video_10_chromatic_starlight_rings",
                index = 10,
                title = "Chromatic Starlight Vortex Rings",
                subtitle = "Exact uploaded design: multicolor neon plasma rings with counter-rotating golden starlight core",
                badge = "Real Design #10",
                assetPath = "pro_videos/video_10_chromatic_starlight_rings.mp4",
                atlasAssetPath = "pro_videos/video_10_chromatic_starlight_rings.webp",
                accentHex = 0xFFFF4081L,
                fallbackLottieDesignId = "pro_video_10_chromatic_starlight_rings",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.ARMILLARY_3D_GYRO
            ),
            ProVideoItem(
                id = "folder_video_11_neon_magenta_cyan_portal",
                index = 11,
                title = "Neon Magenta & Cyan Cyber Portal",
                subtitle = "Exact uploaded design: glowing hot-pink outer tick ring & electric cyan inner portal ring",
                badge = "Real Design #11",
                assetPath = "pro_videos/video_11_neon_magenta_cyan_portal.mp4",
                atlasAssetPath = "pro_videos/video_11_neon_magenta_cyan_portal.webp",
                accentHex = 0xFFFF00E5L,
                fallbackLottieDesignId = "pro_video_11_neon_magenta_cyan_portal",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CYBER_SEGMENTED_HUD
            ),
            ProVideoItem(
                id = "folder_video_12_electric_blue_blade_vortex",
                index = 12,
                title = "Electric Sapphire Blade Vortex",
                subtitle = "Exact uploaded design: spinning sapphire-blue spiral energy blades with white-hot highlights",
                badge = "Real Design #12",
                assetPath = "pro_videos/video_12_electric_blue_blade_vortex.mp4",
                atlasAssetPath = "pro_videos/video_12_electric_blue_blade_vortex.webp",
                accentHex = 0xFF2979FFL,
                fallbackLottieDesignId = "pro_video_12_electric_blue_blade_vortex",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.PLASMA_VORTEX_PORTAL
            ),
            ProVideoItem(
                id = "folder_video_13_cyan_quantum_tech_dial",
                index = 13,
                title = "Cyan Quantum HUD Tech Dial",
                subtitle = "Exact uploaded design: 5 counter-rotating concentric cyan HUD gear & tick rings with glowing core",
                badge = "Real Design #13",
                assetPath = "pro_videos/video_13_cyan_quantum_tech_dial.mp4",
                atlasAssetPath = "pro_videos/video_13_cyan_quantum_tech_dial.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_13_cyan_quantum_tech_dial",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CYBER_SEGMENTED_HUD
            ),
            ProVideoItem(
                id = "folder_video_14_cyan_sunwave_tendrils",
                index = 14,
                title = "Cyan Sunwave Tendrils Ring",
                subtitle = "Exact uploaded video: organic cyan & teal solar-wave tendrils twisting around a dark core",
                badge = "Uploaded Video #14",
                assetPath = "pro_videos/video_14_cyan_sunwave_tendrils.mp4",
                atlasAssetPath = "pro_videos/video_14_cyan_sunwave_tendrils.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_14_cyan_sunwave_tendrils",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_15_ice_blue_particle_globe",
                index = 15,
                title = "Ice-Blue 3D Particle Vortex Globe",
                subtitle = "Exact uploaded video: glowing ice-blue luminous particle vortex sphere swirling in 3D",
                badge = "Uploaded Video #15",
                assetPath = "pro_videos/video_15_ice_blue_particle_globe.mp4",
                atlasAssetPath = "pro_videos/video_15_ice_blue_particle_globe.webp",
                accentHex = 0xFF40C4FFL,
                fallbackLottieDesignId = "pro_video_15_ice_blue_particle_globe",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_16_cobalt_violet_wave_orb",
                index = 16,
                title = "Cobalt & Violet 3D Wave Orb",
                subtitle = "Exact uploaded video: deep cobalt-blue & violet 3D undulating particle wave sphere",
                badge = "Uploaded Video #16",
                assetPath = "pro_videos/video_16_cobalt_violet_wave_orb.mp4",
                atlasAssetPath = "pro_videos/video_16_cobalt_violet_wave_orb.webp",
                accentHex = 0xFF536DFEL,
                fallbackLottieDesignId = "pro_video_16_cobalt_violet_wave_orb",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_17_neon_green_cyan_ribbon",
                index = 17,
                title = "Neon Green & Cyan Ribbon Wave",
                subtitle = "Exact uploaded video: morphing 3D neon emerald-green & electric-cyan harmonic ribbon ring",
                badge = "Uploaded Video #17",
                assetPath = "pro_videos/video_17_neon_green_cyan_ribbon.mp4",
                atlasAssetPath = "pro_videos/video_17_neon_green_cyan_ribbon.webp",
                accentHex = 0xFF00E676L,
                fallbackLottieDesignId = "pro_video_17_neon_green_cyan_ribbon",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_18_magenta_stardust_portal",
                index = 18,
                title = "Magenta Plasma & Stardust Portal",
                subtitle = "Exact uploaded video: blazing magenta plasma ring with shimmering blue stardust core",
                badge = "Uploaded Video #18",
                assetPath = "pro_videos/video_18_magenta_stardust_portal.mp4",
                atlasAssetPath = "pro_videos/video_18_magenta_stardust_portal.webp",
                accentHex = 0xFFFF4081L,
                fallbackLottieDesignId = "pro_video_18_magenta_stardust_portal",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_19_blue_plasma_energy_core",
                index = 19,
                title = "Blue Electric Plasma Energy Core",
                subtitle = "Exact uploaded video: high-voltage blue plasma energy ring with radiating lightning filaments",
                badge = "Uploaded Video #19",
                assetPath = "pro_videos/video_19_blue_plasma_energy_core.mp4",
                atlasAssetPath = "pro_videos/video_19_blue_plasma_energy_core.webp",
                accentHex = 0xFF2979FFL,
                fallbackLottieDesignId = "pro_video_19_blue_plasma_energy_core",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_20_cyan_purple_silk_orb",
                index = 20,
                title = "Cyan & Purple 3D Silk Mesh Orb",
                subtitle = "Exact uploaded video: rotating 3D electric cyan & violet silk particle mesh sphere",
                badge = "Uploaded Video #20",
                assetPath = "pro_videos/video_20_cyan_purple_silk_orb.mp4",
                atlasAssetPath = "pro_videos/video_20_cyan_purple_silk_orb.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_20_cyan_purple_silk_orb",
                isAtlasGrid = true,
                motionProfile = ProMotionProfile.ATLAS_VIDEO
            ),
            ProVideoItem(
                id = "folder_video_21_polar_star_radar_grid",
                index = 21,
                title = "Polar Starlight Radar Grid",
                subtitle = "Exact uploaded design: counter-rotating sapphire polar radar grid with 8 pulsing perimeter star-nodes",
                badge = "Real Design #21",
                assetPath = "pro_videos/video_21_polar_star_radar_grid.mp4",
                atlasAssetPath = "pro_videos/video_21_polar_star_radar_grid.webp",
                accentHex = 0xFF448AFFL,
                fallbackLottieDesignId = "pro_video_21_polar_star_radar_grid",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.STAR_RADAR_GRID
            ),
            ProVideoItem(
                id = "folder_video_22_cyan_dot_matrix_hud",
                index = 22,
                title = "Cyan Dot-Matrix Telemetry HUD",
                subtitle = "Exact uploaded design: 3 counter-rotating cyan dot-matrix, equalizer bar & scanner arc rings",
                badge = "Real Design #22",
                assetPath = "pro_videos/video_22_cyan_dot_matrix_hud.mp4",
                atlasAssetPath = "pro_videos/video_22_cyan_dot_matrix_hud.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_22_cyan_dot_matrix_hud",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CYBER_SEGMENTED_HUD
            ),
            ProVideoItem(
                id = "folder_video_23_cyan_matrix_overdrive_hud",
                index = 23,
                title = "Cyan Dot-Matrix Overdrive Dual-Spin",
                subtitle = "Exact uploaded design: high-speed dual-spin overdrive HUD with counter-rotating dot-matrix bezel",
                badge = "Real Design #23",
                assetPath = "pro_videos/video_23_cyan_matrix_overdrive_hud.mp4",
                atlasAssetPath = "pro_videos/video_23_cyan_matrix_overdrive_hud.webp",
                accentHex = 0xFF18FFFFL,
                fallbackLottieDesignId = "pro_video_23_cyan_matrix_overdrive_hud",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CYBER_SEGMENTED_HUD
            ),
            ProVideoItem(
                id = "folder_video_24_aqua_wave_mesh_sphere",
                index = 24,
                title = "Aqua 3D Wave Mesh Sphere",
                subtitle = "Exact uploaded design: counter-rotating 3D cyan particle stipple core & undulating wave crest shell",
                badge = "Real Design #24",
                assetPath = "pro_videos/video_24_aqua_wave_mesh_sphere.mp4",
                atlasAssetPath = "pro_videos/video_24_aqua_wave_mesh_sphere.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_24_aqua_wave_mesh_sphere",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.PLASMA_VORTEX_PORTAL
            ),
            ProVideoItem(
                id = "folder_video_25_electric_lightning_globe",
                index = 25,
                title = "High-Voltage Electric Lightning Globe",
                subtitle = "Exact uploaded design: counter-rotating electric blue plasma lightning arcs with high-voltage surge",
                badge = "Real Design #25",
                assetPath = "pro_videos/video_25_electric_lightning_globe.mp4",
                atlasAssetPath = "pro_videos/video_25_electric_lightning_globe.webp",
                accentHex = 0xFF2979FFL,
                fallbackLottieDesignId = "pro_video_25_electric_lightning_globe",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.PLASMA_VORTEX_PORTAL
            ),
            ProVideoItem(
                id = "folder_video_26_fiber_optic_light_ring",
                index = 26,
                title = "White-Cyan Fiber-Optic Swirl Ring",
                subtitle = "Exact uploaded design: multi-speed streaming white & cyan fiber-optic light trails on transparent core",
                badge = "Real Design #26",
                assetPath = "pro_videos/video_26_fiber_optic_light_ring.mp4",
                atlasAssetPath = "pro_videos/video_26_fiber_optic_light_ring.webp",
                accentHex = 0xFF80D8FFL,
                fallbackLottieDesignId = "pro_video_26_fiber_optic_light_ring",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.PLASMA_VORTEX_PORTAL
            ),
            ProVideoItem(
                id = "folder_video_27_cyber_notched_hud_scanner",
                index = 27,
                title = "Cyber Notched Tactical HUD Scanner",
                subtitle = "Exact uploaded design: counter-rotating outer notched brackets, triangle indicators & inner equalizer ticks",
                badge = "Real Design #27",
                assetPath = "pro_videos/video_27_cyber_notched_hud_scanner.mp4",
                atlasAssetPath = "pro_videos/video_27_cyber_notched_hud_scanner.webp",
                accentHex = 0xFF00B0FFL,
                fallbackLottieDesignId = "pro_video_27_cyber_notched_hud_scanner",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.CYBER_SEGMENTED_HUD
            ),
            ProVideoItem(
                id = "folder_video_28_hypnotic_spiral_torus",
                index = 28,
                title = "Hypnotic Optical Spiral Torus",
                subtitle = "Exact uploaded design: spinning blue, cyan & gold logarithmic spiral torus with real optical wave motion",
                badge = "Real Design #28",
                assetPath = "pro_videos/video_28_hypnotic_spiral_torus.mp4",
                atlasAssetPath = "pro_videos/video_28_hypnotic_spiral_torus.webp",
                accentHex = 0xFF00E5FFL,
                fallbackLottieDesignId = "pro_video_28_hypnotic_spiral_torus",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.PLASMA_VORTEX_PORTAL
            ),
            ProVideoItem(
                id = "folder_video_29_neon_circuit_trace_ring",
                index = 29,
                title = "Neon Pink & Cyan Circuit Trace Ring",
                subtitle = "Exact uploaded design: 3 counter-rotating neon magenta-to-cyan circuit trace bands with pulsing nodes",
                badge = "Real Design #29",
                assetPath = "pro_videos/video_29_neon_circuit_trace_ring.mp4",
                atlasAssetPath = "pro_videos/video_29_neon_circuit_trace_ring.webp",
                accentHex = 0xFFFF4081L,
                fallbackLottieDesignId = "pro_video_29_neon_circuit_trace_ring",
                isAtlasGrid = false,
                motionProfile = ProMotionProfile.ARMILLARY_3D_GYRO
            )
        )

        val extraItems = generatedProDesignSpecs.map { spec ->
            val itemIndex = 29 + spec.designNum
            val fileNum = String.format(Locale.US, "%03d", spec.designNum)
            val webpPath = "pro_videos/design_$fileNum.webp"
            ProVideoItem(
                id = "folder_${spec.slug}",
                index = itemIndex,
                title = spec.title,
                subtitle = spec.subtitle,
                badge = spec.badge,
                assetPath = webpPath,
                atlasAssetPath = webpPath,
                accentHex = spec.accentHex,
                fallbackLottieDesignId = "pro_${spec.slug}",
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
 * 2. High-resolution `480x480` single-frame transparent real designs (`video_8..13`, `video_21..29`,
 *    and `design_001..design_100`), automatically sliced into 3 seamless concentric ring bitmaps
 *    ([ProDesignLayers]) for 120fps multi-ring counter-rotation + additive HDR bloom.
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
        val cacheSizeKb = (maxKb / 3).coerceAtLeast(40 * 1024)
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return (value.allocationByteCount / 1024).coerceAtLeast(1)
            }
        }
    }

    private val layersCache: LruCache<String, ProDesignLayers> = run {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        val cacheSizeKb = (maxKb / 4).coerceAtLeast(32 * 1024)
        object : LruCache<String, ProDesignLayers>(cacheSizeKb) {
            override fun sizeOf(key: String, value: ProDesignLayers): Int {
                return value.allocationKb
            }
        }
    }

    private fun cacheKeyFor(atlasPath: String, compact: Boolean): String {
        return if (compact) "$atlasPath#compact" else atlasPath
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
            if (preferCompact) {
                val compact = memoryCache.get(cacheKeyFor(atlasPath, compact = true))
                if (compact != null && !compact.isRecycled) return compact
            }
            val full = memoryCache.get(cacheKeyFor(atlasPath, compact = false))
            if (full != null && !full.isRecycled) return full
            if (!preferCompact) {
                val compactFallback = memoryCache.get(cacheKeyFor(atlasPath, compact = true))
                if (compactFallback != null && !compactFallback.isRecycled) return compactFallback
            }
        }
        return null
    }

    fun getCachedDesignLayers(assetPath: String, preferCompact: Boolean = false): ProDesignLayers? {
        val atlasPath = ProVideoFolder.atlasPathFor(assetPath)
        val pinned = pinnedActiveLayers
        if (pinned != null && !pinned.isRecycled && pinnedActiveAtlasPath == atlasPath) {
            return pinned
        }
        synchronized(layersCache) {
            if (preferCompact) {
                val compact = layersCache.get(cacheKeyFor(atlasPath, compact = true))
                if (compact != null && !compact.isRecycled) return compact
            }
            val full = layersCache.get(cacheKeyFor(atlasPath, compact = false))
            if (full != null && !full.isRecycled) return full
            if (!preferCompact) {
                val compactFallback = layersCache.get(cacheKeyFor(atlasPath, compact = true))
                if (compactFallback != null && !compactFallback.isRecycled) return compactFallback
            }
        }
        return null
    }

    fun getOrDecodeAtlas(context: Context, assetPath: String, preferCompact: Boolean = false): Bitmap? {
        val item = ProVideoFolder.findItem(assetPath)
        val atlasPath = item?.atlasAssetPath ?: ProVideoFolder.atlasPathFor(assetPath)
        val pinned = pinnedActiveBitmap
        if (!preferCompact && pinned != null && !pinned.isRecycled && pinnedActiveAtlasPath == atlasPath) {
            return pinned
        }
        val key = cacheKeyFor(atlasPath, compact = preferCompact)
        synchronized(memoryCache) {
            val cached = memoryCache.get(key)
            if (cached != null && !cached.isRecycled) return cached
            if (preferCompact) {
                val full = memoryCache.get(cacheKeyFor(atlasPath, compact = false))
                if (full != null && !full.isRecycled) return full
            }
        }

        synchronized(lockFor(key)) {
            synchronized(memoryCache) {
                val cached = memoryCache.get(key)
                if (cached != null && !cached.isRecycled) return cached
            }
            val appCtx = context.applicationContext ?: context
            val targetSample = if (preferCompact) 2 else 1
            val decoded = decodeAtlasFromAssets(appCtx, atlasPath, sampleSize = targetSample)
                ?: decodeAtlasFromAssets(appCtx, atlasPath, sampleSize = 2)
            if (decoded != null) {
                runCatching { decoded.prepareToDraw() }
                synchronized(memoryCache) {
                    memoryCache.put(key, decoded)
                }
                if (item != null && !item.isAtlasGrid) {
                    getOrBuildDesignLayers(atlasPath, decoded, item.motionProfile, preferCompact)
                }
            }
            return decoded
        }
    }

    private fun getOrBuildDesignLayers(
        atlasPath: String,
        sourceBitmap: Bitmap,
        profile: ProMotionProfile,
        compact: Boolean
    ): ProDesignLayers? {
        if (sourceBitmap.isRecycled) return null
        val key = cacheKeyFor(atlasPath, compact = compact)
        synchronized(layersCache) {
            val existing = layersCache.get(key)
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
                layersCache.put(key, layers)
            }
            layers
        }.getOrNull()
    }

    /**
     * Slices a single-frame `480x480` (or `240x240`) real design bitmap into 3 concentric ring bitmaps
     * (`innerCore`, `midRing`, `outerRing`) using linear complementary radial alpha gradients
     * (`wInner + wMid + wOuter == 1.0` at every pixel) so the real design is 100% preserved while
     * allowing each ring to rotate, counter-rotate, breathe, and bloom independently at 120fps.
     */
    private fun sliceIntoConcentricRings(src: Bitmap, profile: ProMotionProfile): ProDesignLayers {
        val w = src.width.coerceAtLeast(1)
        val h = src.height.coerceAtLeast(1)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxR = min(w, h) * 0.5f

        val r1: Float
        val r2: Float
        val r3: Float
        val r4: Float
        if (profile == ProMotionProfile.SPEEDOMETER_HUD) {
            // Keep the 0..240 speedometer dial and 870/965 digital numbers inside innerCore,
            // isolate the blazing red/orange plasma ring into midRing, and RPM wings into outerRing.
            r1 = 0.50f
            r2 = 0.56f
            r3 = 0.67f
            r4 = 0.73f
        } else {
            r1 = 0.33f
            r2 = 0.43f
            r3 = 0.64f
            r4 = 0.74f
        }

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // 1. Inner Core (1.0 from 0..r1, ramps to 0.0 at r2)
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

        // 2. Mid Ring (0.0 from 0..r1, ramps to 1.0 at r2..r3, ramps to 0.0 at r4)
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

        // 3. Outer Ring (0.0 from 0..r3, ramps to 1.0 at r4..1.0)
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
     * Pins the active lock-screen Pro Video / Pro Design in memory at full resolution (`inSampleSize = 1`)
     * along with its pre-sliced concentric ring bitmaps so fingerprint sensor touch triggers in `<1ms`.
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
                    pinnedActiveLayers = getOrBuildDesignLayers(atlasPath, bmp, item.motionProfile, compact = false)
                } else {
                    pinnedActiveLayers = null
                }
            }
        }
    }

    fun prewarmAsync(
        context: Context,
        specificAssetPath: String? = null,
        preferCompact: Boolean = false,
        onReady: ((Bitmap?) -> Unit)? = null
    ) {
        val appCtx = context.applicationContext ?: context
        val isActivePrewarm = specificAssetPath == null
        val targetPath = specificAssetPath ?: Prefs.getSelectedVideoAsset(appCtx) ?: ProVideoFolder.items.first().assetPath
        val item = ProVideoFolder.findItem(targetPath)
        val atlasPath = item?.atlasAssetPath ?: ProVideoFolder.atlasPathFor(targetPath)

        val already = getCachedAtlas(atlasPath, preferCompact = preferCompact)
        if (already != null && (preferCompact || pinnedActiveAtlasPath == atlasPath || !isActivePrewarm)) {
            if (item != null && !item.isAtlasGrid && getCachedDesignLayers(atlasPath, preferCompact = preferCompact) == null) {
                decodeExecutor.execute {
                    getOrBuildDesignLayers(atlasPath, already, item.motionProfile, compact = preferCompact)
                }
            }
            if (isActivePrewarm && !preferCompact) {
                pinnedActiveAtlasPath = atlasPath
                pinnedActiveBitmap = already
            }
            onReady?.invoke(already)
            return
        }

        val inflightKey = cacheKeyFor(atlasPath, compact = preferCompact)
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
                    pinnedActiveLayers = getCachedDesignLayers(atlasPath, preferCompact = false)
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

    /**
     * Pre-warms the first visible batch of compact thumbnails in the background so opening
     * the Pro Video Folder feels instantaneous with zero frame drops.
     */
    fun prewarmGalleryThumbnailsAsync(context: Context, count: Int = 10) {
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
 * Hardware-accelerated, zero-black-background animation view that renders:
 * 1. The 14 uploaded `.mp4` Pro Videos (`8x8` frame atlases) with sub-frame cross-fade interpolation
 *    and an additive `PorterDuff.Mode.ADD` HDR optical bloom pass, AND
 * 2. All 115 high-res `480x480` uploaded Pro Designs at native **60/120fps** using the
 *    **Real-Design Multi-Ring HDR Live Animation Engine** (preserving 100% of the real uploaded artwork
 *    while animating `outerRing`, `midRing`, and `innerCore` with differential counter-rotation,
 *    3D gimbal perspective breathing, additive HDR self-bloom, live laser speedometer needle sweep,
 *    and expanding optical shockwave rings).
 */
class LoopingAssetVideoView(context: Context) : View(context), Choreographer.FrameCallback {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val additiveBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val additiveFxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    private val srcRect = Rect()
    private val nextSrcRect = Rect()
    private val fullBmpSrcRect = Rect()
    private val dstRect = RectF()

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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDstRect(w, h)
    }

    private fun updateDstRect(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val d = minOf(w, h).toFloat()
        val left = (w - d) / 2f
        val top = (h - d) / 2f
        dstRect.set(left, top, left + d, top + d)
    }

    fun setVideoAsset(
        assetPath: String,
        autoStart: Boolean,
        speed: Float = 1.0f,
        preferCompact: Boolean = preferCompactDecode
    ) {
        preferCompactDecode = preferCompact
        desiredSpeed = speed.coerceIn(0.25f, 3.0f)
        val pathChanged = loadedAssetPath != assetPath
        loadedAssetPath = assetPath
        loadedItem = ProVideoFolder.findItem(assetPath)

        if (pathChanged || atlasBitmap == null || atlasBitmap?.isRecycled == true) {
            designLayers = ProVideoFrameCache.getCachedDesignLayers(assetPath, preferCompact = preferCompact)
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
                        designLayers = ProVideoFrameCache.getCachedDesignLayers(assetPath, preferCompact = preferCompact)
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
            designLayers = ProVideoFrameCache.getCachedDesignLayers(path, preferCompact = preferCompactDecode)
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
                        designLayers = ProVideoFrameCache.getCachedDesignLayers(path, preferCompact = preferCompactDecode)
                        invalidate()
                        if (isPlaying) scheduleNextFrame()
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
        elapsedAnimSeconds = 0f
        lastFrameTimeNanos = 0L
        unscheduleFrameCallback()
        invalidate()
    }

    fun primeAtFrameZero() {
        currentFrameFloat = 0f
        elapsedAnimSeconds = 0f
        lastFrameTimeNanos = 0L
        val path = loadedAssetPath
        if ((atlasBitmap == null || atlasBitmap?.isRecycled == true) && path != null) {
            designLayers = ProVideoFrameCache.getCachedDesignLayers(path, preferCompact = preferCompactDecode)
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
                        designLayers = ProVideoFrameCache.getCachedDesignLayers(path, preferCompact = preferCompactDecode)
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
        val isGridAtlas = item?.isAtlasGrid ?: (bmp.width >= 960 && bmp.height >= 960)

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
        val nextIdx = (frameIdx + 1) % ProVideoFrameCache.TOTAL_FRAMES
        val frac = (currentFrameFloat - currentFrameFloat.toInt()).coerceIn(0f, 1f)

        val col = frameIdx % ProVideoFrameCache.ATLAS_COLS
        val row = frameIdx / ProVideoFrameCache.ATLAS_COLS
        srcRect.set(col * cellW, row * cellH, (col + 1) * cellW, (row + 1) * cellH)

        // 1. Primary crisp video frame
        basePaint.alpha = 255
        canvas.drawBitmap(bmp, srcRect, dstRect, basePaint)

        // 2. Sub-frame cross-fade interpolation for 120Hz silky-smooth motion
        if (frac > 0.05f) {
            val nCol = nextIdx % ProVideoFrameCache.ATLAS_COLS
            val nRow = nextIdx / ProVideoFrameCache.ATLAS_COLS
            nextSrcRect.set(nCol * cellW, nRow * cellH, (nCol + 1) * cellW, (nRow + 1) * cellH)
            blendPaint.alpha = (frac * 155f).toInt().coerceIn(0, 155)
            canvas.drawBitmap(bmp, nextSrcRect, dstRect, blendPaint)
        }

        // 3. Additive HDR optical bloom pass so real video frames radiate authentic OLED laser glow
        val pulse = 0.5f + 0.5f * sin(elapsedAnimSeconds * 4.2f)
        val bloomScale = 1.025f + 0.02f * pulse
        additiveBitmapPaint.alpha = (58f + 36f * pulse).toInt().coerceIn(40, 105)
        canvas.save()
        canvas.scale(bloomScale, bloomScale, dstRect.centerX(), dstRect.centerY())
        canvas.drawBitmap(bmp, srcRect, dstRect, additiveBitmapPaint)
        canvas.restore()
    }

    private fun drawLiveRealDesign(canvas: Canvas, bmp: Bitmap, item: ProVideoItem?) {
        val path = loadedAssetPath
        var layers = designLayers
        if ((layers == null || layers.isRecycled) && path != null) {
            layers = ProVideoFrameCache.getCachedDesignLayers(path, preferCompact = preferCompactDecode)
            designLayers = layers
        }

        val cx = dstRect.centerX()
        val cy = dstRect.centerY()
        val radius = dstRect.width() * 0.5f
        val t = elapsedAnimSeconds
        val idxSeed = (item?.index ?: 1) * 0.37f
        val profile = item?.motionProfile ?: ProMotionProfile.CYBER_SEGMENTED_HUD
        val accentColor = ((item?.accentHex ?: 0xFF00E5FFL).toInt()) or -0x1000000

        if (layers != null && !layers.isRecycled) {
            val lBmp = layers.fullBitmap
            fullBmpSrcRect.set(0, 0, lBmp.width, lBmp.height)

            when (profile) {
                ProMotionProfile.SPEEDOMETER_HUD -> {
                    val isOverdrive = (item?.index == 9)
                    val wingSway = sin(t * (if (isOverdrive) 3.4f else 2.2f)) * (if (isOverdrive) 12f else 7.5f)
                    val wingScale = 1.0f + 0.02f * cos(t * 2.8f)

                    // 1. Outer RPM tachometer wings (oscillate & breathe)
                    canvas.save()
                    canvas.rotate(wingSway, cx, cy)
                    canvas.scale(wingScale, wingScale, cx, cy)
                    basePaint.alpha = 255
                    canvas.drawBitmap(layers.outerRing, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    // 2. Middle red/orange turbo plasma ring (high-speed continuous spin + HDR bloom)
                    val plasmaAngle = (t * (if (isOverdrive) -125f else 85f)) % 360f
                    val plasmaPulse = 1.0f + 0.03f * sin(t * 4.5f)
                    canvas.save()
                    canvas.rotate(plasmaAngle, cx, cy)
                    canvas.scale(plasmaPulse, plasmaPulse, cx, cy)
                    canvas.drawBitmap(layers.midRing, fullBmpSrcRect, dstRect, basePaint)
                    additiveBitmapPaint.alpha = 115
                    canvas.scale(1.03f, 1.03f, cx, cy)
                    canvas.drawBitmap(layers.midRing, fullBmpSrcRect, dstRect, additiveBitmapPaint)
                    canvas.restore()

                    // 3. Inner 0-240 speedometer dial & 870/965 readout (kept upright & readable!)
                    val dialTilt = sin(t * 1.9f) * 2.2f
                    val dialBreath = 1.0f + 0.015f * sin(t * 3.6f)
                    canvas.save()
                    canvas.rotate(dialTilt, cx, cy)
                    canvas.scale(dialBreath, dialBreath, cx, cy)
                    canvas.drawBitmap(layers.innerCore, fullBmpSrcRect, dstRect, basePaint)
                    additiveBitmapPaint.alpha = 70
                    canvas.drawBitmap(layers.innerCore, fullBmpSrcRect, dstRect, additiveBitmapPaint)
                    canvas.restore()

                    // 4. Live sweeping laser speedometer needle across the 0..240 dial
                    val revWave = 0.5f + 0.5f * sin(t * (if (isOverdrive) 3.8f else 2.4f) + sin(t * 1.3f) * 0.6f)
                    val needleDeg = 145f + revWave * 245f
                    val needleRad = Math.toRadians(needleDeg.toDouble())
                    val nLen = radius * 0.42f
                    val nx = cx + cos(needleRad).toFloat() * nLen
                    val ny = cy + sin(needleRad).toFloat() * nLen
                    needlePaint.color = Color.argb(135, 255, 20, 100)
                    needlePaint.strokeWidth = (radius * 0.045f).coerceAtLeast(3f)
                    canvas.drawLine(cx, cy, nx, ny, needlePaint)
                    needlePaint.color = Color.argb(235, 255, 210, 235)
                    needlePaint.strokeWidth = (radius * 0.018f).coerceAtLeast(1.5f)
                    canvas.drawLine(cx, cy, nx, ny, needlePaint)
                }

                ProMotionProfile.ARMILLARY_3D_GYRO -> {
                    val dir = if ((item?.index ?: 0) % 2 == 0) 1f else -1f
                    val outerAng = (t * 34f * dir) % 360f
                    val midAng = (t * -62f * dir + sin(t * 1.6f + idxSeed) * 14f) % 360f
                    val innerAng = (t * 88f * dir) % 360f

                    val outerSx = 1.0f + 0.032f * cos(t * 2.1f + idxSeed)
                    val outerSy = 1.0f - 0.032f * cos(t * 2.1f + idxSeed)
                    val midSx = 1.0f - 0.048f * sin(t * 2.7f + idxSeed)
                    val midSy = 1.0f + 0.048f * sin(t * 2.7f + idxSeed)
                    val innerScale = 1.0f + 0.045f * sin(t * 3.8f + idxSeed)

                    basePaint.alpha = 255
                    // Outer ring
                    canvas.save()
                    canvas.rotate(outerAng, cx, cy)
                    canvas.scale(outerSx, outerSy, cx, cy)
                    canvas.drawBitmap(layers.outerRing, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    // Mid ring (3D gimbal counter-orbit)
                    canvas.save()
                    canvas.rotate(midAng, cx, cy)
                    canvas.scale(midSx, midSy, cx, cy)
                    canvas.drawBitmap(layers.midRing, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    // Inner core
                    canvas.save()
                    canvas.rotate(innerAng, cx, cy)
                    canvas.scale(innerScale, innerScale, cx, cy)
                    canvas.drawBitmap(layers.innerCore, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    // Additive HDR counter-interference bloom of the real design
                    val bloomPulse = 0.5f + 0.5f * sin(t * 3.5f + idxSeed)
                    additiveBitmapPaint.alpha = (75f + 55f * bloomPulse).toInt().coerceIn(55, 135)
                    canvas.save()
                    canvas.rotate(-midAng * 0.65f, cx, cy)
                    val bs = 1.02f + 0.03f * bloomPulse
                    canvas.scale(bs, bs, cx, cy)
                    canvas.drawBitmap(lBmp, fullBmpSrcRect, dstRect, additiveBitmapPaint)
                    canvas.restore()
                }

                ProMotionProfile.PLASMA_VORTEX_PORTAL -> {
                    val dir = if ((item?.index ?: 0) % 2 == 0) 1f else -1f
                    val outerAng = (t * 44f * dir) % 360f
                    val midAng = (t * -76f * dir) % 360f
                    val innerAng = (t * 112f * dir) % 360f

                    val outerScale = 1.0f + 0.03f * sin(t * 2.4f + idxSeed)
                    val midScale = 1.0f + 0.045f * cos(t * 3.1f + idxSeed)
                    val innerScale = 1.0f + 0.055f * sin(t * 4.2f + idxSeed)

                    basePaint.alpha = 255
                    canvas.save()
                    canvas.rotate(outerAng, cx, cy)
                    canvas.scale(outerScale, outerScale, cx, cy)
                    canvas.drawBitmap(layers.outerRing, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    canvas.save()
                    canvas.rotate(midAng, cx, cy)
                    canvas.scale(midScale, midScale, cx, cy)
                    canvas.drawBitmap(layers.midRing, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    canvas.save()
                    canvas.rotate(innerAng, cx, cy)
                    canvas.scale(innerScale, innerScale, cx, cy)
                    canvas.drawBitmap(layers.innerCore, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    // Dual-wave additive HDR photonic shimmer using the real design's filaments
                    val pulse = 0.5f + 0.5f * sin(t * 4.0f + idxSeed)
                    additiveBitmapPaint.alpha = (85f + 60f * pulse).toInt().coerceIn(65, 150)
                    canvas.save()
                    canvas.rotate((t * -52f * dir) % 360f, cx, cy)
                    val bs = 1.03f + 0.035f * pulse
                    canvas.scale(bs, bs, cx, cy)
                    canvas.drawBitmap(lBmp, fullBmpSrcRect, dstRect, additiveBitmapPaint)
                    canvas.restore()
                }

                ProMotionProfile.STAR_RADAR_GRID,
                ProMotionProfile.CYBER_SEGMENTED_HUD,
                ProMotionProfile.ATLAS_VIDEO -> {
                    val dir = if ((item?.index ?: 0) % 2 == 0) 1f else -1f
                    val outerAng = (t * 30f * dir + sin(t * 1.4f + idxSeed) * 8f) % 360f
                    val midAng = (t * -58f * dir) % 360f
                    val innerAng = (t * 74f * dir) % 360f

                    val outerScale = 1.0f + 0.022f * sin(t * 2.2f + idxSeed)
                    val midScale = 1.0f + 0.04f * cos(t * 2.9f + idxSeed)
                    val innerScale = 1.0f + 0.048f * sin(t * 3.7f + idxSeed)

                    basePaint.alpha = 255
                    canvas.save()
                    canvas.rotate(outerAng, cx, cy)
                    canvas.scale(outerScale, outerScale, cx, cy)
                    canvas.drawBitmap(layers.outerRing, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    canvas.save()
                    canvas.rotate(midAng, cx, cy)
                    canvas.scale(midScale, midScale, cx, cy)
                    canvas.drawBitmap(layers.midRing, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    canvas.save()
                    canvas.rotate(innerAng, cx, cy)
                    canvas.scale(innerScale, innerScale, cx, cy)
                    canvas.drawBitmap(layers.innerCore, fullBmpSrcRect, dstRect, basePaint)
                    canvas.restore()

                    // Additive HDR self-bloom pass of the real design
                    val pulse = 0.5f + 0.5f * sin(t * 3.6f + idxSeed)
                    additiveBitmapPaint.alpha = (72f + 52f * pulse).toInt().coerceIn(55, 130)
                    canvas.save()
                    canvas.rotate(outerAng, cx, cy)
                    val bs = 1.02f + 0.025f * pulse
                    canvas.scale(bs, bs, cx, cy)
                    canvas.drawBitmap(lBmp, fullBmpSrcRect, dstRect, additiveBitmapPaint)
                    canvas.restore()
                }
            }
        } else {
            // Instant fallback while concentric layers are building on the background thread
            fullBmpSrcRect.set(0, 0, bmp.width, bmp.height)
            val baseAng = if (profile == ProMotionProfile.SPEEDOMETER_HUD) {
                sin(t * 2.2f) * 4f
            } else {
                (t * 42f) % 360f
            }
            val scale = 1.0f + 0.03f * sin(t * 3.0f)
            basePaint.alpha = 255
            canvas.save()
            canvas.rotate(baseAng, cx, cy)
            canvas.scale(scale, scale, cx, cy)
            canvas.drawBitmap(bmp, fullBmpSrcRect, dstRect, basePaint)
            additiveBitmapPaint.alpha = 90
            canvas.scale(1.03f, 1.03f, cx, cy)
            canvas.drawBitmap(bmp, fullBmpSrcRect, dstRect, additiveBitmapPaint)
            canvas.restore()
        }

        // 5. Subtle expanding optical biometric shockwave ripple synced to the animation phase
        val wavePhase = (t * 0.65f + idxSeed) % 1.0f
        val waveRadius = radius * (0.18f + wavePhase * 0.76f)
        val waveAlpha = ((1f - wavePhase) * (1f - wavePhase) * 95f).toInt().coerceIn(0, 95)
        if (waveAlpha > 4) {
            val r = (accentColor shr 16) and 0xFF
            val g = (accentColor shr 8) and 0xFF
            val b = accentColor and 0xFF
            additiveFxPaint.color = Color.argb(waveAlpha, r, g, b)
            additiveFxPaint.strokeWidth = (radius * 0.022f * (1f - wavePhase * 0.5f)).coerceAtLeast(1.5f)
            canvas.drawCircle(cx, cy, waveRadius, additiveFxPaint)
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

    val lottieView: LottieAnimationView = LottieAnimationView(context).apply {
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

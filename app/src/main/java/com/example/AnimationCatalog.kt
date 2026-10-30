package com.example

import java.util.Locale

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

class AnimationDesign(
    val id: String,
    val index: Int,
    val name: String,
    val description: String,
    val styleLabel: String,
    val primaryColor: FloatArray,
    val secondaryColor: FloatArray,
    private val jsonBuilder: () -> String
) {
    val lottieJson: String by lazy(LazyThreadSafetyMode.PUBLICATION) { jsonBuilder() }
}

object AnimationCatalog {

    internal enum class VisualArchetype(val styleTag: String, val summary: String) {
        CLASSIC_RING("Optic Halo Ring", "Multi-pass HDR concentric biometric ring with specular core"),
        SCAN_LASER("Precision Laser", "High-frequency optical laser sweep with vernier bezel"),
        PARTICLE_BURST("Quantum Stipple", "High-density luminous micro-particle burst & 3D stipple rings"),
        WAVE_RIPPLE("Caustic Ripple", "Refractive hydro-sonic caustic ripples with chromatic rim"),
        FIRE_CORONA("Solar Corona", "High-density multi-pass plasma & solar corona filaments"),
        ICE_CRYSTAL("Cryo Refraction", "Razor-sharp 12-point crystalline frost prism with specular glints"),
        FIRE_AND_ICE("Thermal Dual", "Opposing plasma-orange and cryo-cyan high-velocity comet arcs"),
        GALAXY_ORBIT("Relativistic Orbit", "3D tilted orbital pathways with tapering comet tails"),
        HEXAGON_CUBE("Isometric Lattice", "Micro-engraved hexagonal & isometric 3D wireframe lattice"),
        TRIANGLE_PYRAMID("Prism Delta", "Refractive triangular prism & harmonic energy frame"),
        SQUARE_HUD("Tactical Telemetry", "Precision vernier telemetry brackets & micro-tick matrix"),
        DIAMOND_PRISM("Dichroic Prism", "Multifaceted dichroic crystal prism with subsurface caustics"),
        BUTTERFLY_WINGS("Bioluminescent Wings", "Luminous filament wings with micro-particle aura"),
        HEART_PULSE("Cardio Telemetry", "Sculpted optical heart contour with precision harmonic wave"),
        ROYAL_CROWN("Imperial Chrono", "Precision luxury chronograph bezel with golden crest"),
        CYBER_SHIELD("Aegis Defense HUD", "Multi-layered tactical defense shield with perimeter laser"),
        MATRIX_CIRCUIT("Neural Data Bus", "High-density data stream & radial micro-circuit traces"),
        NATURE_BLOOM("Organic Caustics", "Fluid bioluminescent harmonic waves & botanical geometry"),
        WATER_DROP("Liquid Optics", "Refractive liquid glass droplet with subsurface caustic rings"),
        INFINITY_DNA("Quantum Helix", "High-velocity intertwined infinity & double-strand light trails"),
        SOUND_EQUALIZER("Acoustic Spectrum", "High-resolution harmonic spectrum & vernier gauge"),
        PROGRESS_COMPASS("Chrono Tachometer", "Precision vernier tachometer sweep & cardinal bezel"),
        CHRONO_REACTOR_HUD("Chrono Reactor HUD", "Red crenellated inner reactor, white micro-tick vernier ring & cobalt tachometer sweep"),
        SINGULARITY_SUPERNOVA("Singularity Supernova", "Incandescent white event-horizon vortex & cyan-emerald radial filament needle burst"),
        QUANTUM_PARTICLE_SPHERE("3D Quantum Sphere", "Bioluminescent 3D stipple particle sphere with crescent rim density & aqua plasma core"),
        ATOMIC_COMET_ORBITS("Atomic Comet Orbits", "3D tilted elliptical electron orbits with blazing white-hot sapphire comet tails"),
        DICHROIC_LIQUID_ORB("Dichroic Glass Orb", "3D glossy dark sphere with subsurface cyan-magenta-violet caustics & prism rim"),
        PLASMA_CAUSTIC_SPHERE("Electric Plasma Orb", "Dark 3D sphere wrapped in undulating cyan-white electric plasma caustic waves")
    }

    /**
     * Featured Pro VFX animations faithfully recreating the 6 uploaded reference `.mp4` videos
     * with multi-pass HDR bloom, micro-tick vernier scales, 3D particle stipples, comet trails,
     * and subsurface liquid caustics.
     */
    val proVideoDesigns: List<AnimationDesign> by lazy {
        listOf(
            createProVideoDesign(
                id = "pro_video_1_chrono_reactor",
                index = 1,
                name = "Chrono Telemetry Reactor HUD",
                styleLabel = "Pro Video #1 • HUD",
                description = "Crimson inner crenellated reactor teeth, white micro-tick vernier scale & cobalt-blue outer tachometer sweep",
                archetype = VisualArchetype.CHRONO_REACTOR_HUD,
                primaryHex = 0xFF0066FFL,
                secondaryHex = 0xFFFF1744L
            ),
            createProVideoDesign(
                id = "pro_video_2_singularity_supernova",
                index = 2,
                name = "Singularity Vortex & Supernova",
                styleLabel = "Pro Video #2 • Vortex",
                description = "Incandescent white event-horizon ring pulling violet spiral streaks inward before a cyan-emerald radial filament burst",
                archetype = VisualArchetype.SINGULARITY_SUPERNOVA,
                primaryHex = 0xFF00F5D4L,
                secondaryHex = 0xFF9D4EDDL
            ),
            createProVideoDesign(
                id = "pro_video_3_quantum_particle_sphere",
                index = 3,
                name = "Bioluminescent 3D Particle Sphere",
                styleLabel = "Pro Video #3 • 3D Sphere",
                description = "3D rotating cyan stipple particle sphere with crescent rim density and volumetric deep-aqua inner plasma globes",
                archetype = VisualArchetype.QUANTUM_PARTICLE_SPHERE,
                primaryHex = 0xFF00E5FFL,
                secondaryHex = 0xFF0055FFL
            ),
            createProVideoDesign(
                id = "pro_video_4_atomic_comet_orbits",
                index = 4,
                name = "Atomic Electron Comet Orbits",
                styleLabel = "Pro Video #4 • Orbits",
                description = "3D-tilted elliptical orbital pathways with blazing white-hot sapphire comet heads and tapering light tails",
                archetype = VisualArchetype.ATOMIC_COMET_ORBITS,
                primaryHex = 0xFF2979FFL,
                secondaryHex = 0xFF80D8FFL
            ),
            createProVideoDesign(
                id = "pro_video_5_dichroic_liquid_orb",
                index = 5,
                name = "Dichroic Liquid Glass Orb",
                styleLabel = "Pro Video #5 • Iridescent",
                description = "3D glossy dark sphere with smooth subsurface cyan, magenta, and violet caustics and chromatic prism rim highlight",
                archetype = VisualArchetype.DICHROIC_LIQUID_ORB,
                primaryHex = 0xFF00E5FFL,
                secondaryHex = 0xFFE040FBL
            ),
            createProVideoDesign(
                id = "pro_video_6_plasma_caustic_sphere",
                index = 6,
                name = "Volumetric Electric Plasma Sphere",
                styleLabel = "Pro Video #6 • Plasma",
                description = "Dark 3D orb wrapped in organic, fluid, undulating cyan-white electric plasma caustic waves",
                archetype = VisualArchetype.PLASMA_CAUSTIC_SPHERE,
                primaryHex = 0xFF00F0FFL,
                secondaryHex = 0xFF0077FFL
            ),
            createProVideoDesign(
                id = "pro_video_7_solar_cobalt_rings",
                index = 7,
                name = "Solar & Cobalt Concentric Rings",
                styleLabel = "Pro Video #7 • Dual Rings",
                description = "Blazing solar-orange & cobalt-blue concentric light rings rotating around a dark core",
                archetype = VisualArchetype.FIRE_AND_ICE,
                primaryHex = 0xFFFF9100L,
                secondaryHex = 0xFF0077FFL
            ),
            createProVideoDesign(
                id = "pro_video_8_cyber_speedometer_hud",
                index = 8,
                name = "Cyber Speedometer & Tachometer HUD",
                styleLabel = "Pro Video #8 • Speed HUD",
                description = "0-240 speedometer dial with sweeping pink needle, red plasma ring & RPM wings",
                archetype = VisualArchetype.CHRONO_REACTOR_HUD,
                primaryHex = 0xFF00B0FFL,
                secondaryHex = 0xFFFF1744L
            ),
            createProVideoDesign(
                id = "pro_video_9_cyber_tachometer_overdrive",
                index = 9,
                name = "Cyber Tachometer Overdrive Dual-Spin",
                styleLabel = "Pro Video #9 • Overdrive",
                description = "High-speed counter-rotating speedometer bezel, turbo red ring & needle sweep",
                archetype = VisualArchetype.CHRONO_REACTOR_HUD,
                primaryHex = 0xFFFF3D00L,
                secondaryHex = 0xFF00E5FFL
            ),
            createProVideoDesign(
                id = "pro_video_10_chromatic_starlight_rings",
                index = 10,
                name = "Chromatic Starlight Vortex Rings",
                styleLabel = "Pro Video #10 • Starlight",
                description = "Multicolor neon plasma rings with counter-rotating golden starlight core",
                archetype = VisualArchetype.SINGULARITY_SUPERNOVA,
                primaryHex = 0xFFFF4081L,
                secondaryHex = 0xFF00E5FFL
            ),
            createProVideoDesign(
                id = "pro_video_11_neon_magenta_cyan_portal",
                index = 11,
                name = "Neon Magenta & Cyan Cyber Portal",
                styleLabel = "Pro Video #11 • Cyber Portal",
                description = "Glowing hot-pink outer tick ring & electric cyan inner portal ring",
                archetype = VisualArchetype.CLASSIC_RING,
                primaryHex = 0xFFFF00E5L,
                secondaryHex = 0xFF00E5FFL
            ),
            createProVideoDesign(
                id = "pro_video_12_electric_blue_blade_vortex",
                index = 12,
                name = "Electric Sapphire Blade Vortex",
                styleLabel = "Pro Video #12 • Blade Vortex",
                description = "Spinning sapphire-blue spiral energy blades with white-hot highlights",
                archetype = VisualArchetype.GALAXY_ORBIT,
                primaryHex = 0xFF2979FFL,
                secondaryHex = 0xFF80D8FFL
            ),
            createProVideoDesign(
                id = "pro_video_13_cyan_quantum_tech_dial",
                index = 13,
                name = "Cyan Quantum HUD Tech Dial",
                styleLabel = "Pro Video #13 • Tech Dial",
                description = "5 counter-rotating concentric cyan HUD gear & tick rings with glowing core",
                archetype = VisualArchetype.CHRONO_REACTOR_HUD,
                primaryHex = 0xFF00E5FFL,
                secondaryHex = 0xFF0091EAL
            )
        )
    }

    val categories: List<AnimationCategory> by lazy {
        AnimationCatalogData.specs.map { spec ->
            buildCategory(spec)
        }
    }

    val totalDesignCount: Int by lazy {
        categories.sumOf { it.designs.size }
    }

    private fun createProVideoDesign(
        id: String,
        index: Int,
        name: String,
        styleLabel: String,
        description: String,
        archetype: VisualArchetype,
        primaryHex: Long,
        secondaryHex: Long
    ): AnimationDesign {
        val c1 = hexToRgbFloats(primaryHex)
        val c2 = hexToRgbFloats(secondaryHex)
        return AnimationDesign(
            id = id,
            index = index,
            name = name,
            description = description,
            styleLabel = styleLabel,
            primaryColor = c1,
            secondaryColor = c2,
            jsonBuilder = {
                generateLottieJson(
                    name = name,
                    archetype = archetype,
                    catNumber = 1,
                    designIndex = index,
                    c1 = c1,
                    c2 = c2
                )
            }
        )
    }

    private fun hexToRgbFloats(hex: Long): FloatArray {
        val r = ((hex shr 16) and 0xFF) / 255f
        val g = ((hex shr 8) and 0xFF) / 255f
        val b = (hex and 0xFF) / 255f
        return floatArrayOf(r, g, b)
    }

    private fun shiftColor(rgb: FloatArray, delta: Float): FloatArray {
        return floatArrayOf(
            (rgb[0] + delta).coerceIn(0.08f, 1.0f),
            (rgb[1] - delta * 0.4f).coerceIn(0.08f, 1.0f),
            (rgb[2] + delta * 0.6f).coerceIn(0.08f, 1.0f)
        )
    }

    private fun resolveArchetype(name: String): VisualArchetype {
        val n = name.lowercase(Locale.US)
        return when {
            n.contains("chrono") || n.contains("tachometer") || n.contains("telemetry") || n.contains("hud") || n.contains("radar") || n.contains("compass") || n.contains("progress") -> VisualArchetype.CHRONO_REACTOR_HUD
            n.contains("singularity") || n.contains("supernova") || n.contains("starburst") || n.contains("star burst") || n.contains("energy burst") || n.contains("explosion") || n.contains("vortex") -> VisualArchetype.SINGULARITY_SUPERNOVA
            n.contains("sphere") || n.contains("orb") && (n.contains("particle") || n.contains("quantum") || n.contains("dot")) || n.contains("particle") || n.contains("dots") || n.contains("dust") || n.contains("sand") -> VisualArchetype.QUANTUM_PARTICLE_SPHERE
            n.contains("comet") || n.contains("atomic") || n.contains("electron") || n.contains("orbit") || n.contains("planet") || n.contains("galaxy") || n.contains("spiral") || n.contains("celestial") -> VisualArchetype.ATOMIC_COMET_ORBITS
            n.contains("dichroic") || n.contains("iridescent") || n.contains("glass") || n.contains("bubble") || n.contains("crystal ball") || n.contains("orb") || n.contains("aurora") || n.contains("holographic") || n.contains("hologram") || n.contains("prism") || n.contains("rainbow") -> VisualArchetype.DICHROIC_LIQUID_ORB
            n.contains("plasma") || n.contains("caustic") || n.contains("electric") || n.contains("liquid") || n.contains("smoke") || n.contains("wave") || n.contains("ripple") -> VisualArchetype.PLASMA_CAUSTIC_SPHERE
            n.contains("butterfly") -> VisualArchetype.BUTTERFLY_WINGS
            n.contains("heart") -> VisualArchetype.HEART_PULSE
            n.contains("crown") || n.contains("royal") -> VisualArchetype.ROYAL_CROWN
            n.contains("shield") || n.contains("security") || n.contains("protection") -> VisualArchetype.CYBER_SHIELD
            n.contains("infinity") || n.contains("dna") || n.contains("ribbon") || n.contains("yin yang") -> VisualArchetype.INFINITY_DNA
            n.contains("fire & ice") || n.contains("split") || n.contains("dual") || n.contains("day & night") || n.contains("energy frame") -> VisualArchetype.FIRE_AND_ICE
            n.contains("fire") || n.contains("flame") || n.contains("lava") || n.contains("magma") || n.contains("solar") || n.contains("sunburst") || n.contains("dragon") -> VisualArchetype.FIRE_CORONA
            n.contains("ice") || n.contains("snow") || n.contains("frost") -> VisualArchetype.ICE_CRYSTAL
            n.contains("triangle") || n.contains("pyramid") || n.contains("mountain") -> VisualArchetype.TRIANGLE_PYRAMID
            n.contains("hexagon") || n.contains("polygon") || n.contains("cube") || n.contains("honeycomb") || n.contains("polyhedron") -> VisualArchetype.HEXAGON_CUBE
            n.contains("diamond") || n.contains("crystal") || n.contains("shatter") -> VisualArchetype.DIAMOND_PRISM
            n.contains("leaf") || n.contains("leaves") || n.contains("nature") || n.contains("vines") || n.contains("forest") || n.contains("flower") || n.contains("bloom") || n.contains("lotus") || n.contains("sakura") || n.contains("rose") || n.contains("floral") || n.contains("feather") || n.contains("mandala") -> VisualArchetype.NATURE_BLOOM
            n.contains("drop") || n.contains("splash") || n.contains("underwater") || n.contains("ocean") || n.contains("drip") -> VisualArchetype.WATER_DROP
            n.contains("matrix") || n.contains("circuit") || n.contains("binary") || n.contains("code") || n.contains("data") || n.contains("digital rain") || n.contains("network") || n.contains("wireframe") || n.contains("chip") -> VisualArchetype.MATRIX_CIRCUIT
            n.contains("sound") || n.contains("equalizer") || n.contains("waveform") || n.contains("sonic") -> VisualArchetype.SOUND_EQUALIZER
            n.contains("scan") || n.contains("laser") || n.contains("beam") || n.contains("loading bar") || n.contains("vertical lines") -> VisualArchetype.SCAN_LASER
            n.contains("square") || n.contains("grid") || n.contains("digital frame") || n.contains("blocks") || n.contains("future interface") -> VisualArchetype.SQUARE_HUD
            else -> VisualArchetype.CLASSIC_RING
        }
    }

    private fun resolveColors(
        name: String,
        catPrimary: FloatArray,
        catSecondary: FloatArray,
        idx: Int
    ): Pair<FloatArray, FloatArray> {
        val n = name.lowercase(Locale.US)
        val delta = ((idx % 5) - 2) * 0.04f
        val pair: Pair<Long, Long> = when {
            n.contains("fire & ice") || n.contains("split") || n.contains("blue & orange") || n.contains("dual") || n.contains("energy frame") ->
                0xFFFF5500L to 0xFF00E5FFL
            n.contains("blue flame") ->
                0xFF00B0FFL to 0xFF651FFFL
            n.contains("fire") || n.contains("flame") || n.contains("lava") || n.contains("magma") || n.contains("solar") || n.contains("orange") || n.contains("sunburst") ->
                0xFFFF5722L to 0xFFFFAB00L
            n.contains("gold") || n.contains("crown") || n.contains("royal") || n.contains("sand") ->
                0xFFFFD740L to 0xFFFF8F00L
            n.contains("ice") || n.contains("snow") || n.contains("frost") || n.contains("crystal") || n.contains("water") || n.contains("ocean") || n.contains("underwater") || n.contains("blue") ->
                0xFF00E5FFL to 0xFF2979FFL
            n.contains("leaf") || n.contains("leaves") || n.contains("nature") || n.contains("vines") || n.contains("forest") || n.contains("green") || n.contains("matrix") || n.contains("code") ->
                0xFF00E676L to 0xFF00BFA5L
            n.contains("heart") || n.contains("pink") || n.contains("sakura") || n.contains("rose") || n.contains("flower") || n.contains("lotus") || n.contains("bloom") ->
                0xFFFF2A85L to 0xFFD500F9L
            n.contains("butterfly") || n.contains("purple") || n.contains("violet") || n.contains("galaxy") || n.contains("nebula") || n.contains("vortex") ->
                0xFF7C4DFFL to 0xFF00E5FFL
            n.contains("rainbow") || n.contains("color") || n.contains("prism") || n.contains("aurora") || n.contains("paint") || n.contains("sunset") || n.contains("dichroic") ->
                0xFF00E5FFL to 0xFFE040FBL
            n.contains("silver") || n.contains("white") || n.contains("steel") || n.contains("metal") || n.contains("chrome") || n.contains("minimal") || n.contains("dark") ->
                0xFFECEFF1L to 0xFF90A4AEL
            n.contains("teal") || n.contains("aqua") ->
                0xFF1DE9B6L to 0xFF00B0FFL
            n.contains("dark energy") || n.contains("glitch") || n.contains("reactor") ->
                0xFF0066FFL to 0xFFFF1744L
            else -> {
                val c1 = if (idx % 2 == 0) shiftColor(catPrimary, delta) else shiftColor(catSecondary, delta)
                val c2 = if (idx % 2 == 0) shiftColor(catSecondary, -delta) else shiftColor(catPrimary, -delta)
                return c1 to c2
            }
        }
        return shiftColor(hexToRgbFloats(pair.first), delta) to shiftColor(hexToRgbFloats(pair.second), -delta)
    }

    private fun buildCategory(spec: RawCategorySpec): AnimationCategory {
        val basePrimary = hexToRgbFloats(spec.accentHex)
        val baseSecondary = hexToRgbFloats(spec.secondaryHex)
        val shortTitle = spec.title.substringAfter(". ", spec.title)

        val designs = spec.items.mapIndexed { idx, item ->
            val archetype = resolveArchetype(item.name)
            val (c1, c2) = resolveColors(item.name, basePrimary, baseSecondary, idx)
            val designId = "${spec.id}_${item.number}"
            val jsonSafeTitle = item.name.replace("\"", "'")

            AnimationDesign(
                id = designId,
                index = item.number,
                name = item.name,
                description = "${archetype.styleTag} • ${archetype.summary}",
                styleLabel = archetype.styleTag,
                primaryColor = c1,
                secondaryColor = c2,
                jsonBuilder = {
                    generateLottieJson(
                        name = jsonSafeTitle,
                        archetype = archetype,
                        catNumber = spec.number,
                        designIndex = item.number,
                        c1 = c1,
                        c2 = c2
                    )
                }
            )
        }

        return AnimationCategory(
            number = spec.number,
            id = spec.id,
            title = spec.title,
            shortTitle = shortTitle,
            subtitle = spec.subtitle,
            accentHex = spec.accentHex,
            secondaryHex = spec.secondaryHex,
            designs = designs
        )
    }

    private fun fmt(v: Float): String = String.format(Locale.US, "%.2f", v)

    /**
     * Produces a 3-pass photorealistic HDR glow stroke stack (incandescent white-hot core +
     * saturated chromatic mid-glow + wide atmospheric bloom halo) so every vector path looks
     * like real optical/shader VFX rather than flat cartoon lines.
     */
    private fun hdrStrokeStack(
        color: String,
        baseWidth: Int,
        dash: Int = 0,
        gap: Int = 0
    ): String {
        val coreW = (baseWidth * 0.35f).coerceIn(1.0f, 2.2f)
        val midW = baseWidth.toFloat().coerceIn(2.2f, 8.0f)
        val bloomW = (baseWidth * 2.5f).coerceIn(6.0f, 18.0f)
        val dashBlock = if (dash > 0 && gap > 0) {
            """, "d": [{ "n": "d", "nm": "dash", "v": { "a": 0, "k": $dash } }, { "n": "g", "nm": "gap", "v": { "a": 0, "k": $gap } }]"""
        } else {
            ""
        }
        return """
            { "ty": "st", "c": { "a": 0, "k": [0.95, 0.99, 1, 1] }, "o": { "a": 0, "k": 94 }, "w": { "a": 0, "k": ${fmt(coreW)} }, "lc": 2, "lj": 2$dashBlock, "nm": "SpecularFilament" },
            { "ty": "st", "c": { "a": 0, "k": $color }, "o": { "a": 0, "k": 78 }, "w": { "a": 0, "k": ${fmt(midW)} }, "lc": 2, "lj": 2$dashBlock, "nm": "ChromaticGlow" },
            { "ty": "st", "c": { "a": 0, "k": $color }, "o": { "a": 0, "k": 20 }, "w": { "a": 0, "k": ${fmt(bloomW)} }, "lc": 2, "lj": 2$dashBlock, "nm": "AtmosphericBloom" }
        """.trimIndent()
    }

    private fun generateLottieJson(
        name: String,
        archetype: VisualArchetype,
        catNumber: Int,
        designIndex: Int,
        c1: FloatArray,
        c2: FloatArray
    ): String {
        val c1Str = "[${fmt(c1[0])}, ${fmt(c1[1])}, ${fmt(c1[2])}, 1]"
        val c2Str = "[${fmt(c2[0])}, ${fmt(c2[1])}, ${fmt(c2[2])}, 1]"
        val whiteStr = "[0.94, 0.98, 1.00, 1]"
        val dir = if ((catNumber + designIndex) % 2 == 0) 1 else -1
        val spinEnd = 360 * dir
        val counterSpinEnd = -spinEnd
        val outerStroke = 4 + ((catNumber + designIndex) % 3)
        val innerStroke = 3 + ((catNumber + designIndex) % 2)

        // Precision optical vernier aperture core (replaces the old cartoon 3-oval fingerprint core)
        val coreLayer = photorealOpticCoreLayer(ind = 99, color1 = c1Str, color2 = c2Str)

        val effectLayers = when (archetype) {
            VisualArchetype.CHRONO_REACTOR_HUD -> """
                ${rotatingDashedEllipseLayer(ind = 1, diameter = 182, strokeWidth = 6, color = c1Str, dash = 3, gap = 6, endRot = spinEnd / 2)},
                ${trimmedArcLayer(ind = 2, diameter = 166, strokeWidth = 3, color = whiteStr, startPct = 8, endPct = 42, endRot = spinEnd)},
                ${trimmedArcLayer(ind = 3, diameter = 166, strokeWidth = 3, color = whiteStr, startPct = 58, endPct = 92, endRot = spinEnd)},
                ${rotatingDashedEllipseLayer(ind = 4, diameter = 148, strokeWidth = 4, color = whiteStr, dash = 1, gap = 4, endRot = counterSpinEnd / 3)},
                ${rotatingDashedEllipseLayer(ind = 5, diameter = 114, strokeWidth = 7, color = c2Str, dash = 5, gap = 5, endRot = spinEnd)}
            """.trimIndent()

            VisualArchetype.SINGULARITY_SUPERNOVA -> """
                ${starLayer(ind = 1, points = 28, outerRadius = 94, innerRadius = 38, outerRound = 0, innerRound = 0, strokeWidth = 2, color = c1Str, endRot = spinEnd / 3)},
                ${starLayer(ind = 2, points = 20, outerRadius = 84, innerRadius = 46, outerRound = 12, innerRound = -20, strokeWidth = 3, color = c2Str, endRot = counterSpinEnd / 2)},
                ${breathingEllipseLayer(ind = 3, diameter = 124, strokeWidth = 5, color = whiteStr)},
                ${expandingParticleRingLayer(ind = 4, diameter = 184, strokeWidth = 4, color = c1Str)}
            """.trimIndent()

            VisualArchetype.QUANTUM_PARTICLE_SPHERE -> """
                ${rotatingDashedEllipseLayer(ind = 1, diameter = 178, strokeWidth = 4, color = c1Str, dash = 1, gap = 5, endRot = spinEnd / 2)},
                ${tiltedOrbitEllipseLayer(ind = 2, width = 176, height = 128, tiltDeg = 28, strokeWidth = 4, color = c1Str)},
                ${tiltedOrbitEllipseLayer(ind = 3, width = 176, height = 128, tiltDeg = -28, strokeWidth = 3, color = c2Str)},
                ${rotatingDashedEllipseLayer(ind = 4, diameter = 142, strokeWidth = 4, color = c2Str, dash = 1, gap = 6, endRot = counterSpinEnd / 2)},
                ${trimmedArcLayer(ind = 5, diameter = 178, strokeWidth = 5, color = whiteStr, startPct = 55, endPct = 92, endRot = spinEnd / 4)}
            """.trimIndent()

            VisualArchetype.ATOMIC_COMET_ORBITS -> """
                ${tiltedOrbitEllipseLayer(ind = 1, width = 186, height = 68, tiltDeg = 36, strokeWidth = 5, color = c1Str)},
                ${tiltedOrbitEllipseLayer(ind = 2, width = 186, height = 68, tiltDeg = -36, strokeWidth = 5, color = c2Str)},
                ${tiltedOrbitEllipseLayer(ind = 3, width = 176, height = 58, tiltDeg = 82, strokeWidth = 3, color = whiteStr)},
                ${rotatingDashedEllipseLayer(ind = 4, diameter = 96, strokeWidth = 4, color = c1Str, dash = 1, gap = 5, endRot = spinEnd)}
            """.trimIndent()

            VisualArchetype.DICHROIC_LIQUID_ORB -> """
                ${breathingEllipseLayer(ind = 1, diameter = 174, strokeWidth = 5, color = c1Str)},
                ${trimmedArcLayer(ind = 2, diameter = 172, strokeWidth = 6, color = c2Str, startPct = 12, endPct = 64, endRot = spinEnd)},
                ${starLayer(ind = 3, points = 6, outerRadius = 78, innerRadius = 58, outerRound = 85, innerRound = 65, strokeWidth = 5, color = c2Str, endRot = counterSpinEnd / 2)},
                ${starLayer(ind = 4, points = 5, outerRadius = 72, innerRadius = 52, outerRound = 85, innerRound = 65, strokeWidth = 4, color = c1Str, endRot = spinEnd / 2)}
            """.trimIndent()

            VisualArchetype.PLASMA_CAUSTIC_SPHERE -> """
                ${starLayer(ind = 1, points = 8, outerRadius = 86, innerRadius = 68, outerRound = 75, innerRound = 55, strokeWidth = 4, color = c1Str, endRot = spinEnd / 2)},
                ${starLayer(ind = 2, points = 10, outerRadius = 82, innerRadius = 62, outerRound = 70, innerRound = 50, strokeWidth = 3, color = c2Str, endRot = counterSpinEnd / 2)},
                ${trimmedArcLayer(ind = 3, diameter = 174, strokeWidth = 4, color = whiteStr, startPct = 5, endPct = 52, endRot = spinEnd)},
                ${breathingEllipseLayer(ind = 4, diameter = 176, strokeWidth = 3, color = c1Str)}
            """.trimIndent()

            VisualArchetype.CLASSIC_RING -> """
                ${breathingEllipseLayer(ind = 1, diameter = 170, strokeWidth = outerStroke, color = c1Str)},
                ${trimmedArcLayer(ind = 2, diameter = 154, strokeWidth = innerStroke + 1, color = c2Str, startPct = 6, endPct = 68, endRot = spinEnd)},
                ${rotatingDashedEllipseLayer(ind = 3, diameter = 138, strokeWidth = 2, color = whiteStr, dash = 1, gap = 5, endRot = counterSpinEnd / 2)},
                ${expandingEllipseLayer(ind = 4, diameter = 182, strokeWidth = 3, color = c1Str, startScale = 68, endScale = 104)}
            """.trimIndent()

            VisualArchetype.SCAN_LASER -> """
                ${scanLineSweepLayer(ind = 1, color = c1Str, vertical = (designIndex % 5 == 0))},
                ${roundedRectHudLayer(ind = 2, size = 166, cornerRadius = 82, strokeWidth = outerStroke, color = c2Str, dash = 28, gap = 14, endRot = spinEnd / 2)},
                ${rotatingDashedEllipseLayer(ind = 3, diameter = 146, strokeWidth = 2, color = whiteStr, dash = 1, gap = 4, endRot = counterSpinEnd / 3)}
            """.trimIndent()

            VisualArchetype.PARTICLE_BURST -> """
                ${rotatingDashedEllipseLayer(ind = 1, diameter = 178, strokeWidth = 4, color = c1Str, dash = 1, gap = 6, endRot = spinEnd)},
                ${rotatingDashedEllipseLayer(ind = 2, diameter = 154, strokeWidth = 4, color = c2Str, dash = 1, gap = 5, endRot = counterSpinEnd)},
                ${expandingParticleRingLayer(ind = 3, diameter = 172, strokeWidth = 4, color = c1Str)}
            """.trimIndent()

            VisualArchetype.WAVE_RIPPLE -> """
                ${expandingEllipseLayer(ind = 1, diameter = 182, strokeWidth = outerStroke, color = c1Str, startScale = 54, endScale = 104)},
                ${delayedExpandingEllipseLayer(ind = 2, diameter = 164, strokeWidth = innerStroke + 1, color = c2Str)},
                ${trimmedArcLayer(ind = 3, diameter = 144, strokeWidth = 4, color = c1Str, startPct = 10, endPct = 82, endRot = spinEnd)}
            """.trimIndent()

            VisualArchetype.FIRE_CORONA -> """
                ${starLayer(ind = 1, points = 22, outerRadius = 90, innerRadius = 68, outerRound = 35, innerRound = -25, strokeWidth = outerStroke, color = c1Str, endRot = spinEnd)},
                ${starLayer(ind = 2, points = 16, outerRadius = 82, innerRadius = 62, outerRound = 20, innerRound = -15, strokeWidth = innerStroke + 1, color = c2Str, endRot = counterSpinEnd)},
                ${expandingEllipseLayer(ind = 3, diameter = 168, strokeWidth = 3, color = c1Str, startScale = 72, endScale = 102)}
            """.trimIndent()

            VisualArchetype.ICE_CRYSTAL -> """
                ${starLayer(ind = 1, points = 12, outerRadius = 90, innerRadius = 54, outerRound = 0, innerRound = 0, strokeWidth = outerStroke, color = c1Str, endRot = spinEnd / 2)},
                ${starLayer(ind = 2, points = 12, outerRadius = 80, innerRadius = 62, outerRound = 0, innerRound = 0, strokeWidth = innerStroke, color = c2Str, endRot = counterSpinEnd / 2)},
                ${polystarLayer(ind = 3, points = 6, outerRadius = 68, strokeWidth = 3, color = whiteStr, endRot = spinEnd / 3, pulse = true)}
            """.trimIndent()

            VisualArchetype.FIRE_AND_ICE -> """
                ${trimmedArcLayer(ind = 1, diameter = 172, strokeWidth = outerStroke + 2, color = c1Str, startPct = 2, endPct = 48, endRot = spinEnd)},
                ${trimmedArcLayer(ind = 2, diameter = 172, strokeWidth = outerStroke + 2, color = c2Str, startPct = 52, endPct = 98, endRot = spinEnd)},
                ${rotatingDashedEllipseLayer(ind = 3, diameter = 148, strokeWidth = 3, color = whiteStr, dash = 2, gap = 6, endRot = counterSpinEnd)}
            """.trimIndent()

            VisualArchetype.GALAXY_ORBIT -> """
                ${tiltedOrbitEllipseLayer(ind = 1, width = 184, height = 70, tiltDeg = 34, strokeWidth = outerStroke, color = c1Str)},
                ${tiltedOrbitEllipseLayer(ind = 2, width = 184, height = 70, tiltDeg = -34, strokeWidth = innerStroke + 1, color = c2Str)},
                ${rotatingDashedEllipseLayer(ind = 3, diameter = 154, strokeWidth = 3, color = c1Str, dash = 2, gap = 7, endRot = spinEnd)}
            """.trimIndent()

            VisualArchetype.HEXAGON_CUBE -> """
                ${polystarLayer(ind = 1, points = 6, outerRadius = 88, strokeWidth = outerStroke, color = c1Str, endRot = spinEnd / 2, pulse = true)},
                ${polystarLayer(ind = 2, points = 6, outerRadius = 72, strokeWidth = innerStroke, color = c2Str, endRot = counterSpinEnd / 2, pulse = false)},
                ${isometricCubeLinesLayer(ind = 3, color = c2Str)}
            """.trimIndent()

            VisualArchetype.TRIANGLE_PYRAMID -> """
                ${polystarLayer(ind = 1, points = 3, outerRadius = 92, strokeWidth = outerStroke, color = c1Str, endRot = spinEnd / 3, pulse = true)},
                ${polystarLayer(ind = 2, points = 3, outerRadius = 76, strokeWidth = innerStroke, color = c2Str, endRot = counterSpinEnd / 3, pulse = false)},
                ${rotatingDashedEllipseLayer(ind = 3, diameter = 144, strokeWidth = 2, color = whiteStr, dash = 1, gap = 5, endRot = spinEnd / 2)}
            """.trimIndent()

            VisualArchetype.SQUARE_HUD -> """
                ${roundedRectHudLayer(ind = 1, size = 158, cornerRadius = 20, strokeWidth = outerStroke, color = c1Str, dash = 36, gap = 12, endRot = 0)},
                ${roundedRectHudLayer(ind = 2, size = 136, cornerRadius = 14, strokeWidth = innerStroke, color = c2Str, dash = 18, gap = 10, endRot = spinEnd / 4)},
                ${scanLineSweepLayer(ind = 3, color = c1Str, vertical = false)}
            """.trimIndent()

            VisualArchetype.DIAMOND_PRISM -> """
                ${polystarLayer(ind = 1, points = 4, outerRadius = 90, strokeWidth = outerStroke, color = c1Str, endRot = 0, pulse = true)},
                ${starLayer(ind = 2, points = 8, outerRadius = 84, innerRadius = 58, outerRound = 0, innerRound = 0, strokeWidth = innerStroke, color = c2Str, endRot = spinEnd / 4)},
                ${polystarLayer(ind = 3, points = 8, outerRadius = 68, strokeWidth = 3, color = c1Str, endRot = counterSpinEnd / 4, pulse = false)}
            """.trimIndent()

            VisualArchetype.BUTTERFLY_WINGS -> """
                ${butterflyWingsLayer(ind = 1, strokeWidth = outerStroke, color = c1Str, innerWing = false)},
                ${butterflyWingsLayer(ind = 2, strokeWidth = innerStroke, color = c2Str, innerWing = true)},
                ${expandingParticleRingLayer(ind = 3, diameter = 174, strokeWidth = 3, color = c1Str)}
            """.trimIndent()

            VisualArchetype.HEART_PULSE -> """
                ${heartBezierLayer(ind = 1, scale = 100, strokeWidth = outerStroke, color = c1Str)},
                ${heartBezierLayer(ind = 2, scale = 84, strokeWidth = innerStroke, color = c2Str)},
                ${ecgWaveformLayer(ind = 3, color = c2Str)}
            """.trimIndent()

            VisualArchetype.ROYAL_CROWN -> """
                ${royalCrownBezierLayer(ind = 1, color = c1Str)},
                ${rotatingDashedEllipseLayer(ind = 2, diameter = 160, strokeWidth = outerStroke, color = c1Str, dash = 3, gap = 6, endRot = spinEnd)},
                ${breathingEllipseLayer(ind = 3, diameter = 140, strokeWidth = innerStroke, color = c2Str)}
            """.trimIndent()

            VisualArchetype.CYBER_SHIELD -> """
                ${cyberShieldBezierLayer(ind = 1, scale = 100, strokeWidth = outerStroke, color = c1Str)},
                ${cyberShieldBezierLayer(ind = 2, scale = 84, strokeWidth = innerStroke, color = c2Str)},
                ${scanLineSweepLayer(ind = 3, color = c2Str, vertical = false)}
            """.trimIndent()

            VisualArchetype.MATRIX_CIRCUIT -> """
                ${matrixRainColumnsLayer(ind = 1, color = c1Str)},
                ${rotatingDashedEllipseLayer(ind = 2, diameter = 168, strokeWidth = outerStroke, color = c2Str, dash = 2, gap = 6, endRot = spinEnd)},
                ${polystarLayer(ind = 3, points = 8, outerRadius = 74, strokeWidth = 3, color = c1Str, endRot = 0, pulse = true)}
            """.trimIndent()

            VisualArchetype.NATURE_BLOOM -> """
                ${starLayer(ind = 1, points = 10, outerRadius = 88, innerRadius = 56, outerRound = 80, innerRound = -30, strokeWidth = outerStroke, color = c1Str, endRot = spinEnd / 3)},
                ${starLayer(ind = 2, points = 10, outerRadius = 74, innerRadius = 48, outerRound = 70, innerRound = -25, strokeWidth = innerStroke, color = c2Str, endRot = counterSpinEnd / 3)},
                ${breathingEllipseLayer(ind = 3, diameter = 132, strokeWidth = 3, color = c1Str)}
            """.trimIndent()

            VisualArchetype.WATER_DROP -> """
                ${waterDropletBezierLayer(ind = 1, strokeWidth = outerStroke, color = c1Str)},
                ${tiltedOrbitEllipseLayer(ind = 2, width = 166, height = 56, tiltDeg = 0, strokeWidth = innerStroke + 1, color = c2Str)},
                ${expandingEllipseLayer(ind = 3, diameter = 172, strokeWidth = 3, color = c1Str, startScale = 58, endScale = 102)}
            """.trimIndent()

            VisualArchetype.INFINITY_DNA -> """
                ${infinityHelixBezierLayer(ind = 1, strokeWidth = outerStroke, color = c1Str, vertical = name.lowercase(Locale.US).contains("dna"))},
                ${infinityHelixBezierLayer(ind = 2, strokeWidth = innerStroke, color = c2Str, vertical = !name.lowercase(Locale.US).contains("dna"))},
                ${breathingEllipseLayer(ind = 3, diameter = 166, strokeWidth = 3, color = c1Str)}
            """.trimIndent()

            VisualArchetype.SOUND_EQUALIZER -> """
                ${ecgWaveformLayer(ind = 1, color = c1Str)},
                ${rotatingDashedEllipseLayer(ind = 2, diameter = 172, strokeWidth = outerStroke, color = c2Str, dash = 2, gap = 5, endRot = spinEnd / 2)},
                ${expandingEllipseLayer(ind = 3, diameter = 154, strokeWidth = 3, color = c1Str, startScale = 72, endScale = 102)}
            """.trimIndent()

            VisualArchetype.PROGRESS_COMPASS -> """
                ${animatedProgressRingLayer(ind = 1, diameter = 172, strokeWidth = outerStroke + 1, color = c1Str)},
                ${rotatingDashedEllipseLayer(ind = 2, diameter = 152, strokeWidth = innerStroke + 1, color = c2Str, dash = 2, gap = 6, endRot = counterSpinEnd / 2)},
                ${polystarLayer(ind = 3, points = 4, outerRadius = 86, strokeWidth = 3, color = c1Str, endRot = spinEnd / 4, pulse = true)}
            """.trimIndent()
        }

        return """
        {
          "v": "5.7.4",
          "fr": 60,
          "ip": 0,
          "op": 60,
          "w": 200,
          "h": 200,
          "nm": "$name",
          "ddd": 0,
          "assets": [],
          "layers": [
            $effectLayers,
            $coreLayer
          ]
        }
        """.trimIndent()
    }

    fun parseHexColor(hexString: String, fallbackHex: Long = 0xFF00E5FFL): Long {
        val cleaned = hexString.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        val rgb = cleaned.takeLast(6).toLongOrNull(16) ?: (fallbackHex and 0xFFFFFFL)
        return 0xFF000000L or rgb
    }

    fun buildAiCustomLottieJson(
        title: String,
        primaryArchetypeName: String,
        secondaryArchetypeName: String,
        primaryHex: Long,
        secondaryHex: Long,
        accentHex: Long,
        strokeWidth: Int,
        starPoints: Int,
        dashLen: Int,
        gapLen: Int,
        clockwise: Boolean,
        innerRadiusRatio: Int = 72,
        outerRoundness: Int = 15,
        customContourPoints: List<Pair<Float, Float>> = emptyList()
    ): String {
        val normalizedPrimary = primaryArchetypeName.trim()
            .uppercase(Locale.US)
            .replace(' ', '_')
            .replace('-', '_')
        val primaryArch = runCatching {
            VisualArchetype.valueOf(normalizedPrimary)
        }.getOrElse { resolveArchetype("$primaryArchetypeName $title") }

        val c1 = hexToRgbFloats(primaryHex)
        val c2 = hexToRgbFloats(secondaryHex)
        val c3 = hexToRgbFloats(accentHex)
        val c1Str = "[${fmt(c1[0])}, ${fmt(c1[1])}, ${fmt(c1[2])}, 1]"
        val c2Str = "[${fmt(c2[0])}, ${fmt(c2[1])}, ${fmt(c2[2])}, 1]"
        val c3Str = "[${fmt(c3[0])}, ${fmt(c3[1])}, ${fmt(c3[2])}, 1]"

        val safeTitle = title.replace("\"", "'").take(40).ifBlank { "AI Custom Animation" }
        val baseJson = generateLottieJson(
            name = safeTitle,
            archetype = primaryArch,
            catNumber = strokeWidth.coerceIn(3, 10),
            designIndex = if (clockwise) 2 else 1,
            c1 = c1,
            c2 = c2
        )

        val pts = starPoints.coerceIn(3, 28)
        val dsh = dashLen.coerceIn(1, 36)
        val gp = gapLen.coerceIn(3, 22)
        val stw = strokeWidth.coerceIn(2, 8)
        val rot = if (clockwise) -360 else 360
        val outerR = 88
        val innerR = ((outerR * innerRadiusRatio.coerceIn(30, 92)) / 100).coerceIn(28, 82)
        val roundness = outerRoundness.coerceIn(-40, 60)

        val hybridLayer = when {
            secondaryArchetypeName.contains("SCAN", ignoreCase = true) ||
                secondaryArchetypeName.contains("LASER", ignoreCase = true) -> {
                scanLineSweepLayer(ind = 50, color = c3Str, vertical = false)
            }
            secondaryArchetypeName.contains("POLYGON", ignoreCase = true) ||
                secondaryArchetypeName.contains("HEX", ignoreCase = true) ||
                secondaryArchetypeName.contains("TRIANGLE", ignoreCase = true) -> {
                polystarLayer(
                    ind = 50,
                    points = pts.coerceIn(3, 12),
                    outerRadius = 84,
                    strokeWidth = stw,
                    color = c3Str,
                    endRot = rot,
                    pulse = true
                )
            }
            secondaryArchetypeName.contains("STAR", ignoreCase = true) ||
                secondaryArchetypeName.contains("FIRE", ignoreCase = true) ||
                secondaryArchetypeName.contains("ICE", ignoreCase = true) ||
                secondaryArchetypeName.contains("BLOOM", ignoreCase = true) ||
                secondaryArchetypeName.contains("CRYSTAL", ignoreCase = true) -> {
                starLayer(
                    ind = 50,
                    points = pts,
                    outerRadius = outerR,
                    innerRadius = innerR,
                    outerRound = roundness,
                    innerRound = (roundness / 2),
                    strokeWidth = stw,
                    color = c3Str,
                    endRot = rot
                )
            }
            else -> {
                rotatingDashedEllipseLayer(
                    ind = 50,
                    diameter = 174,
                    strokeWidth = stw,
                    color = c3Str,
                    dash = dsh,
                    gap = gp,
                    endRot = rot
                )
            }
        }

        val customContourLayer = if (customContourPoints.size >= 4) {
            customContourBezierLayer(
                ind = 51,
                points = customContourPoints,
                strokeWidth = (stw + 1).coerceAtMost(8),
                primaryColor = c1Str,
                accentColor = c3Str,
                endRot = if (clockwise) 360 else -360
            )
        } else {
            null
        }

        val extraLayers = if (customContourLayer != null) {
            "$customContourLayer,\n$hybridLayer"
        } else {
            hybridLayer
        }

        val coreTarget = photorealOpticCoreLayer(ind = 99, color1 = c1Str, color2 = c2Str)
        return baseJson.replace(coreTarget, "$extraLayers,\n$coreTarget")
    }

    private fun customContourBezierLayer(
        ind: Int,
        points: List<Pair<Float, Float>>,
        strokeWidth: Int,
        primaryColor: String,
        accentColor: String,
        endRot: Int
    ): String {
        val n = points.size
        val vList = StringBuilder()
        val iList = StringBuilder()
        val oList = StringBuilder()

        for (idx in 0 until n) {
            val prev = points[(idx - 1 + n) % n]
            val curr = points[idx]
            val next = points[(idx + 1) % n]

            val tx = (next.first - prev.first) * 0.16f
            val ty = (next.second - prev.second) * 0.16f

            if (idx > 0) {
                vList.append(", ")
                iList.append(", ")
                oList.append(", ")
            }
            vList.append("[${fmt(curr.first)}, ${fmt(curr.second)}]")
            iList.append("[${fmt(-tx)}, ${fmt(-ty)}]")
            oList.append("[${fmt(tx)}, ${fmt(ty)}]")
        }

        return """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "CustomImageContour_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 96 },
            "r": {
              "a": 1,
              "k": [
                { "t": 0, "s": [0], "e": [${endRot / 6}], "i": { "x": [0.5], "y": [1] }, "o": { "x": [0.5], "y": [0] } },
                { "t": 30, "s": [${endRot / 6}], "e": [0], "i": { "x": [0.5], "y": [1] }, "o": { "x": [0.5], "y": [0] } },
                { "t": 60, "s": [0] }
              ]
            },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [92, 92, 100], "e": [103, 103, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [103, 103, 100], "e": [92, 92, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [92, 92, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [$iList],
                      "o": [$oList],
                      "v": [$vList],
                      "c": true
                    }
                  },
                  "nm": "CustomPath"
                },
                {
                  "ty": "tm",
                  "s": { "a": 0, "k": 0 },
                  "e": {
                    "a": 1,
                    "k": [
                      { "t": 0, "s": [42], "e": [100], "i": { "x": [0.5], "y": [1] }, "o": { "x": [0.5], "y": [0] } },
                      { "t": 30, "s": [100], "e": [42], "i": { "x": [0.5], "y": [1] }, "o": { "x": [0.5], "y": [0] } },
                      { "t": 60, "s": [42] }
                    ]
                  },
                  "o": {
                    "a": 1,
                    "k": [
                      { "t": 0, "s": [0], "e": [360], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                      { "t": 60, "s": [360] }
                    ]
                  },
                  "m": 1,
                  "nm": "ContourTrim"
                },
                { "ty": "st", "c": { "a": 0, "k": [0.95, 0.99, 1, 1] }, "o": { "a": 0, "k": 92 }, "w": { "a": 0, "k": 1.3 }, "lc": 2, "lj": 2, "nm": "SpecularCore" },
                { "ty": "st", "c": { "a": 0, "k": $primaryColor }, "o": { "a": 0, "k": 82 }, "w": { "a": 0, "k": $strokeWidth }, "lc": 2, "lj": 2, "nm": "PrimaryStroke" },
                { "ty": "st", "c": { "a": 0, "k": $accentColor }, "o": { "a": 0, "k": 24 }, "w": { "a": 0, "k": ${strokeWidth * 2 + 4} }, "lc": 2, "lj": 2, "nm": "GlowHalo" },
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "ContourGroup_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
        """.trimIndent()
    }

    /**
     * Precision optical aperture & micro-tick vernier core that leaves the central UDFPS sensor
     * zone optically open while adding a realistic glass rim highlight and micro-engraved bezel.
     */
    private fun photorealOpticCoreLayer(ind: Int, color1: String, color2: String): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "OpticVernierAperture",
          "sr": 1,
          "ks": {
            "o": {
              "a": 1,
              "k": [
                { "t": 0, "s": [58], "e": [96], "i": { "x": [0.5], "y": [1] }, "o": { "x": [0.5], "y": [0] } },
                { "t": 30, "s": [96], "e": [58], "i": { "x": [0.5], "y": [1] }, "o": { "x": [0.5], "y": [0] } },
                { "t": 60, "s": [58] }
              ]
            },
            "r": {
              "a": 1,
              "k": [
                { "t": 0, "s": [0], "e": [180], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                { "t": 60, "s": [180] }
              ]
            },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [116, 116] }, "p": { "a": 0, "k": [0, 0] }, "nm": "VernierBezel" },
                {
                  "ty": "st",
                  "c": { "a": 0, "k": [0.94, 0.98, 1, 1] },
                  "o": { "a": 0, "k": 72 },
                  "w": { "a": 0, "k": 1.2 },
                  "lc": 2,
                  "lj": 2,
                  "d": [
                    { "n": "d", "nm": "dash", "v": { "a": 0, "k": 1 } },
                    { "n": "g", "nm": "gap", "v": { "a": 0, "k": 5 } }
                  ],
                  "nm": "VernierTicks"
                },
                { "ty": "st", "c": { "a": 0, "k": $color1 }, "o": { "a": 0, "k": 18 }, "w": { "a": 0, "k": 10 }, "lc": 2, "lj": 2, "nm": "ApertureBloom" },
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "OpticCoreGroup"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun scanLineSweepLayer(ind: Int, color: String, vertical: Boolean): String {
        val rot = if (vertical) 90 else 0
        return """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "LaserSweep_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 98 },
            "r": { "a": 0, "k": $rot },
            "p": {
              "a": 1,
              "k": [
                { "t": 0, "s": [100, 36, 0], "e": [100, 164, 0], "i": { "x": 0.5, "y": 1 }, "o": { "x": 0.5, "y": 0 } },
                { "t": 30, "s": [100, 164, 0], "e": [100, 36, 0], "i": { "x": 0.5, "y": 1 }, "o": { "x": 0.5, "y": 0 } },
                { "t": 60, "s": [100, 36, 0] }
              ]
            },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [0, 0]],
                      "o": [[0, 0], [0, 0]],
                      "v": [[-78, 0], [78, 0]],
                      "c": false
                    }
                  },
                  "nm": "ScanBar"
                },
                ${hdrStrokeStack(color = color, baseWidth = 4)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
        """.trimIndent()
    }

    private fun heartBezierLayer(ind: Int, scale: Int, strokeWidth: Int, color: String): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "NeonHeart_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 96 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [${scale - 8}, ${scale - 8}, 100], "e": [${scale + 6}, ${scale + 6}, 100], "i": { "x": [0.4, 0.4, 0.4], "y": [1, 1, 1] }, "o": { "x": [0.2, 0.2, 0.2], "y": [0, 0, 0] } },
                { "t": 20, "s": [${scale + 6}, ${scale + 6}, 100], "e": [${scale - 4}, ${scale - 4}, 100], "i": { "x": [0.4, 0.4, 0.4], "y": [1, 1, 1] }, "o": { "x": [0.2, 0.2, 0.2], "y": [0, 0, 0] } },
                { "t": 35, "s": [${scale - 4}, ${scale - 4}, 100], "e": [${scale + 4}, ${scale + 4}, 100], "i": { "x": [0.4, 0.4, 0.4], "y": [1, 1, 1] }, "o": { "x": [0.2, 0.2, 0.2], "y": [0, 0, 0] } },
                { "t": 60, "s": [${scale - 8}, ${scale - 8}, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [-42, -32], [-40, 26], [0, 0], [40, 26], [42, -32]],
                      "o": [[-26, -42], [-42, 22], [0, 0], [40, -24], [42, 22], [26, -42]],
                      "v": [[0, -42], [-74, -22], [0, 74], [0, 74], [74, -22], [0, -42]],
                      "c": true
                    }
                  },
                  "nm": "HeartPath"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun butterflyWingsLayer(ind: Int, strokeWidth: Int, color: String, innerWing: Boolean): String {
        val sc = if (innerWing) 76 else 100
        return """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "ButterflyWings_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 95 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [${sc - 22}, $sc, 100], "e": [${sc + 4}, $sc, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [${sc + 4}, $sc, 100], "e": [${sc - 22}, $sc, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [${sc - 22}, $sc, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [-20, -35], [-25, 0], [-20, -15], [-15, 25], [0, 0], [15, 25], [20, -15], [25, 0], [20, -35]],
                      "o": [[-30, -45], [-20, 35], [-25, 20], [15, 35], [0, 0], [15, 35], [-25, 20], [20, 35], [30, -45], [0, 0]],
                      "v": [[0, -18], [-78, -68], [-42, 4], [-64, 58], [0, 24], [0, 24], [64, 58], [42, 4], [78, -68], [0, -18]],
                      "c": true
                    }
                  },
                  "nm": "WingsPath"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
        """.trimIndent()
    }

    private fun royalCrownBezierLayer(ind: Int, color: String): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "RoyalCrown_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 98 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [94, 94, 100], "e": [104, 104, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [104, 104, 100], "e": [94, 94, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [94, 94, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0]],
                      "o": [[0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0]],
                      "v": [[-44, -48], [-52, -78], [-22, -60], [0, -88], [22, -60], [52, -78], [44, -48]],
                      "c": true
                    }
                  },
                  "nm": "CrownCrest"
                },
                ${hdrStrokeStack(color = color, baseWidth = 4)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun cyberShieldBezierLayer(ind: Int, scale: Int, strokeWidth: Int, color: String): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "CyberShield_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 95 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [${scale - 5}, ${scale - 5}, 100], "e": [${scale + 4}, ${scale + 4}, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [${scale + 4}, ${scale + 4}, 100], "e": [${scale - 5}, ${scale - 5}, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [${scale - 5}, ${scale - 5}, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [0, 0], [0, -28], [-24, -14], [-24, 14], [0, 28]],
                      "o": [[0, 0], [0, 28], [24, 14], [24, -14], [0, -28], [0, 0]],
                      "v": [[0, -82], [-68, -54], [-68, 8], [0, 84], [68, 8], [68, -54]],
                      "c": true
                    }
                  },
                  "nm": "ShieldContour"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun waterDropletBezierLayer(ind: Int, strokeWidth: Int, color: String): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "WaterDrop_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 95 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [92, 96, 100], "e": [102, 98, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [102, 98, 100], "e": [92, 96, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [92, 96, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [0, -34], [-36, 0], [0, 34]],
                      "o": [[-24, 34], [0, 34], [36, 0], [24, -34]],
                      "v": [[0, -84], [-64, 18], [0, 82], [64, 18]],
                      "c": true
                    }
                  },
                  "nm": "DropletPath"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun infinityHelixBezierLayer(ind: Int, strokeWidth: Int, color: String, vertical: Boolean): String {
        val rot = if (vertical) 90 else 0
        return """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "InfinityHelix_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 96 },
            "r": { "a": 0, "k": $rot },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[-26, -34], [0, -32], [26, -34], [0, 32]],
                      "o": [[26, 34], [0, 32], [-26, 34], [0, -32]],
                      "v": [[0, 0], [74, 0], [0, 0], [-74, 0]],
                      "c": true
                    }
                  },
                  "nm": "Lemniscate"
                },
                {
                  "ty": "tm",
                  "s": { "a": 0, "k": 0 },
                  "e": { "a": 0, "k": 75 },
                  "o": {
                    "a": 1,
                    "k": [
                      { "t": 0, "s": [0], "e": [360], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                      { "t": 60, "s": [360] }
                    ]
                  },
                  "m": 1,
                  "nm": "Trim"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
        """.trimIndent()
    }

    private fun ecgWaveformLayer(ind: Int, color: String): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "SonicWave_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 95 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [100, 75, 100], "e": [100, 125, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [100, 125, 100], "e": [100, 75, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [100, 75, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0]],
                      "o": [[0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0]],
                      "v": [[-86, 0], [-56, 0], [-44, -26], [-28, 28], [-12, -46], [12, 46], [28, -28], [44, 24], [86, 0]],
                      "c": false
                    }
                  },
                  "nm": "WaveLine"
                },
                ${hdrStrokeStack(color = color, baseWidth = 4)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun isometricCubeLinesLayer(ind: Int, color: String): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "IsoCube_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 88 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0]],
                      "o": [[0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0]],
                      "v": [[0, -86], [0, 0], [74, 42], [0, 0], [-74, 42], [0, 0]],
                      "c": false
                    }
                  },
                  "nm": "CubeSpokes"
                },
                ${hdrStrokeStack(color = color, baseWidth = 3)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun matrixRainColumnsLayer(ind: Int, color: String): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "MatrixColumns_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 92 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sh",
                  "ks": {
                    "a": 0,
                    "k": {
                      "i": [[0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0]],
                      "o": [[0, 0], [0, 0], [0, 0], [0, 0], [0, 0], [0, 0]],
                      "v": [[-62, -78], [-62, 78], [-34, 78], [-34, -78], [34, -78], [34, 78]],
                      "c": false
                    }
                  },
                  "nm": "CodeStreams"
                },
                {
                  "ty": "tm",
                  "s": { "a": 0, "k": 0 },
                  "e": { "a": 0, "k": 65 },
                  "o": {
                    "a": 1,
                    "k": [
                      { "t": 0, "s": [0], "e": [360], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                      { "t": 60, "s": [360] }
                    ]
                  },
                  "m": 1,
                  "nm": "Trim"
                },
                ${hdrStrokeStack(color = color, baseWidth = 4, dash = 6, gap = 8)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun tiltedOrbitEllipseLayer(
        ind: Int,
        width: Int,
        height: Int,
        tiltDeg: Int,
        strokeWidth: Int,
        color: String
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "TiltedOrbit_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 94 },
            "r": { "a": 0, "k": $tiltDeg },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [$width, $height] }, "p": { "a": 0, "k": [0, 0] }, "nm": "OrbitEl" },
                {
                  "ty": "tm",
                  "s": { "a": 0, "k": 0 },
                  "e": { "a": 0, "k": 68 },
                  "o": {
                    "a": 1,
                    "k": [
                      { "t": 0, "s": [0], "e": [360], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                      { "t": 60, "s": [360] }
                    ]
                  },
                  "m": 1,
                  "nm": "Trim"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun roundedRectHudLayer(
        ind: Int,
        size: Int,
        cornerRadius: Int,
        strokeWidth: Int,
        color: String,
        dash: Int,
        gap: Int,
        endRot: Int
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "HudFrame_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 95 },
            "r": {
              "a": 1,
              "k": [
                { "t": 0, "s": [0], "e": [$endRot], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                { "t": 60, "s": [$endRot] }
              ]
            },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "rc", "s": { "a": 0, "k": [$size, $size] }, "p": { "a": 0, "k": [0, 0] }, "r": { "a": 0, "k": $cornerRadius }, "nm": "Rect" },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth, dash = dash, gap = gap)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun animatedProgressRingLayer(
        ind: Int,
        diameter: Int,
        strokeWidth: Int,
        color: String
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "ProgressRing_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 98 },
            "r": { "a": 0, "k": -90 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [$diameter, $diameter] }, "p": { "a": 0, "k": [0, 0] }, "nm": "El" },
                {
                  "ty": "tm",
                  "s": { "a": 0, "k": 0 },
                  "e": {
                    "a": 1,
                    "k": [
                      { "t": 0, "s": [12], "e": [100], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.2], "y": [0] } },
                      { "t": 60, "s": [100] }
                    ]
                  },
                  "o": { "a": 0, "k": 0 },
                  "m": 1,
                  "nm": "Trim"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun expandingParticleRingLayer(
        ind: Int,
        diameter: Int,
        strokeWidth: Int,
        color: String
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "ParticleBurst_$ind",
          "sr": 1,
          "ks": {
            "o": {
              "a": 1,
              "k": [
                { "t": 0, "s": [100], "e": [0], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.2], "y": [0] } },
                { "t": 60, "s": [0] }
              ]
            },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [58, 58, 100], "e": [108, 108, 100], "i": { "x": [0.4, 0.4, 0.4], "y": [1, 1, 1] }, "o": { "x": [0.2, 0.2, 0.2], "y": [0, 0, 0] } },
                { "t": 60, "s": [108, 108, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [$diameter, $diameter] }, "p": { "a": 0, "k": [0, 0] }, "nm": "El" },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth, dash = 1, gap = 6)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun expandingEllipseLayer(
        ind: Int,
        diameter: Int,
        strokeWidth: Int,
        color: String,
        startScale: Int,
        endScale: Int
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "ExpandingRing_$ind",
          "sr": 1,
          "ks": {
            "o": {
              "a": 1,
              "k": [
                { "t": 0, "s": [100], "e": [0], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.2], "y": [0] } },
                { "t": 60, "s": [0] }
              ]
            },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [$startScale, $startScale, 100], "e": [$endScale, $endScale, 100], "i": { "x": [0.4, 0.4, 0.4], "y": [1, 1, 1] }, "o": { "x": [0.2, 0.2, 0.2], "y": [0, 0, 0] } },
                { "t": 60, "s": [$endScale, $endScale, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [$diameter, $diameter] }, "p": { "a": 0, "k": [0, 0] }, "nm": "El" },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun delayedExpandingEllipseLayer(
        ind: Int,
        diameter: Int,
        strokeWidth: Int,
        color: String
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "WaveRing_$ind",
          "sr": 1,
          "ks": {
            "o": {
              "a": 1,
              "k": [
                { "t": 0, "s": [30], "e": [100], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.2], "y": [0] } },
                { "t": 30, "s": [100], "e": [0], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.2], "y": [0] } },
                { "t": 60, "s": [0] }
              ]
            },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [60, 60, 100], "e": [82, 82, 100], "i": { "x": [0.4, 0.4, 0.4], "y": [1, 1, 1] }, "o": { "x": [0.2, 0.2, 0.2], "y": [0, 0, 0] } },
                { "t": 30, "s": [82, 82, 100], "e": [104, 104, 100], "i": { "x": [0.4, 0.4, 0.4], "y": [1, 1, 1] }, "o": { "x": [0.2, 0.2, 0.2], "y": [0, 0, 0] } },
                { "t": 60, "s": [104, 104, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [$diameter, $diameter] }, "p": { "a": 0, "k": [0, 0] }, "nm": "El" },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun breathingEllipseLayer(
        ind: Int,
        diameter: Int,
        strokeWidth: Int,
        color: String
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "ApertureRing_$ind",
          "sr": 1,
          "ks": {
            "o": {
              "a": 1,
              "k": [
                { "t": 0, "s": [64], "e": [100], "i": { "x": [0.5], "y": [1] }, "o": { "x": [0.5], "y": [0] } },
                { "t": 30, "s": [100], "e": [64], "i": { "x": [0.5], "y": [1] }, "o": { "x": [0.5], "y": [0] } },
                { "t": 60, "s": [64] }
              ]
            },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [95, 95, 100], "e": [102, 102, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [102, 102, 100], "e": [95, 95, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [95, 95, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [$diameter, $diameter] }, "p": { "a": 0, "k": [0, 0] }, "nm": "El" },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun rotatingDashedEllipseLayer(
        ind: Int,
        diameter: Int,
        strokeWidth: Int,
        color: String,
        dash: Int,
        gap: Int,
        endRot: Int
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "DashedRing_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 95 },
            "r": {
              "a": 1,
              "k": [
                { "t": 0, "s": [0], "e": [$endRot], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                { "t": 60, "s": [$endRot] }
              ]
            },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [$diameter, $diameter] }, "p": { "a": 0, "k": [0, 0] }, "nm": "El" },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth, dash = dash, gap = gap)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun trimmedArcLayer(
        ind: Int,
        diameter: Int,
        strokeWidth: Int,
        color: String,
        startPct: Int,
        endPct: Int,
        endRot: Int
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "ArcRing_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 96 },
            "r": {
              "a": 1,
              "k": [
                { "t": 0, "s": [0], "e": [$endRot], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                { "t": 60, "s": [$endRot] }
              ]
            },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 0, "k": [100, 100, 100] }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "d": 1, "ty": "el", "s": { "a": 0, "k": [$diameter, $diameter] }, "p": { "a": 0, "k": [0, 0] }, "nm": "El" },
                { "ty": "tm", "s": { "a": 0, "k": $startPct }, "e": { "a": 0, "k": $endPct }, "o": { "a": 0, "k": 0 }, "m": 1, "nm": "Trim" },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()

    private fun polystarLayer(
        ind: Int,
        points: Int,
        outerRadius: Int,
        strokeWidth: Int,
        color: String,
        endRot: Int,
        pulse: Boolean
    ): String {
        val scaleBlock = if (pulse) {
            """
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [88, 88, 100], "e": [102, 102, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [102, 102, 100], "e": [88, 88, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [88, 88, 100] }
              ]
            }
            """.trimIndent()
        } else {
            """"s": { "a": 0, "k": [100, 100, 100] }"""
        }

        return """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "Polygon_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 94 },
            "r": {
              "a": 1,
              "k": [
                { "t": 0, "s": [0], "e": [$endRot], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                { "t": 60, "s": [$endRot] }
              ]
            },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            $scaleBlock
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sr",
                  "sy": 2,
                  "d": 1,
                  "pt": { "a": 0, "k": $points },
                  "p": { "a": 0, "k": [0, 0] },
                  "r": { "a": 0, "k": 0 },
                  "or": { "a": 0, "k": $outerRadius },
                  "os": { "a": 0, "k": 6 },
                  "nm": "Poly"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
        """.trimIndent()
    }

    private fun starLayer(
        ind: Int,
        points: Int,
        outerRadius: Int,
        innerRadius: Int,
        outerRound: Int,
        innerRound: Int,
        strokeWidth: Int,
        color: String,
        endRot: Int
    ): String = """
        {
          "ddd": 0,
          "ind": $ind,
          "ty": 4,
          "nm": "Star_$ind",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 94 },
            "r": {
              "a": 1,
              "k": [
                { "t": 0, "s": [0], "e": [$endRot], "i": { "x": [0.5], "y": [0.5] }, "o": { "x": [0.5], "y": [0.5] } },
                { "t": 60, "s": [$endRot] }
              ]
            },
            "p": { "a": 0, "k": [100, 100, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": {
              "a": 1,
              "k": [
                { "t": 0, "s": [90, 90, 100], "e": [102, 102, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 30, "s": [102, 102, 100], "e": [90, 90, 100], "i": { "x": [0.5, 0.5, 0.5], "y": [1, 1, 1] }, "o": { "x": [0.5, 0.5, 0.5], "y": [0, 0, 0] } },
                { "t": 60, "s": [90, 90, 100] }
              ]
            }
          },
          "ao": 0,
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sr",
                  "sy": 1,
                  "d": 1,
                  "pt": { "a": 0, "k": $points },
                  "p": { "a": 0, "k": [0, 0] },
                  "r": { "a": 0, "k": 0 },
                  "ir": { "a": 0, "k": $innerRadius },
                  "is": { "a": 0, "k": $innerRound },
                  "or": { "a": 0, "k": $outerRadius },
                  "os": { "a": 0, "k": $outerRound },
                  "nm": "Star"
                },
                ${hdrStrokeStack(color = color, baseWidth = strokeWidth)},
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ],
              "nm": "G_$ind"
            }
          ],
          "ip": 0,
          "op": 60,
          "st": 0,
          "bm": 0
        }
    """.trimIndent()
}

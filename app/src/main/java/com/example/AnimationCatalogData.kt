package com.example

internal data class RawDesignItem(
    val number: Int,
    val name: String
)

internal data class RawCategorySpec(
    val number: Int,
    val id: String,
    val title: String,
    val subtitle: String,
    val accentHex: Long,
    val secondaryHex: Long,
    val names: List<String> = emptyList(),
    val items: List<RawDesignItem> = names.mapIndexed { idx, s -> RawDesignItem(idx + 1, s) }
)

internal object AnimationCatalogData {
    val specs: List<RawCategorySpec> = listOf(
        // Sheet 1: Screenshot_2026-10-29_151759_04708e47.png (50 Designs)
        RawCategorySpec(
            number = 1,
            id = "sheet_1",
            title = "1. Classic & Futuristic Grid (50)",
            subtitle = "Classic Glow, Butterfly, Neon Heart, Crown & Futuristic Grid",
            accentHex = 0xFF00E5FF,
            secondaryHex = 0xFF7C4DFF,
            names = listOf(
                "Classic Glow", "Neon Ring", "Dual Ring", "Morphing Circle", "Scan Line",
                "Particles", "Wave Ripple", "Liquid Flow", "Fire Effect", "Ice Crystal",
                "Galaxy Spiral", "Holographic", "Digital Grid", "Hexagon Frame", "Circuit Lines",
                "Glass Shatter", "Smoke Effect", "Golden Ring", "Aurora Light", "Minimal Line",
                "Butterfly", "Dots Circle", "3D Lift", "Water Drop", "Neon Flame",
                "Diamond", "Energy Burst", "Matrix Code", "Planet Orbit", "Solar Flare",
                "Leaves", "Sand Wave", "Neon Heart", "Pixelate", "Triangle",
                "Fire & Ice", "Ring Pulse", "Butterfly Trail", "Liquid Metal", "Starburst",
                "Space Dust", "Wave Lines", "Crown", "Crystal Prism", "Dynamic Lines",
                "Flower Bloom", "Ring Segments", "Smoke Ring", "Golden Particles", "Futuristic Grid"
            )
        ),

        // Sheet 2: Screenshot_2026-10-29_151721_c260dd21.png (50 Designs)
        RawCategorySpec(
            number = 2,
            id = "sheet_2",
            title = "2. 50 Unique Sensor Animations",
            subtitle = "Modern, Stylish & Futuristic: Lion Fire, Space Rocket, Compass & Infinity Loop",
            accentHex = 0xFF2979FF,
            secondaryHex = 0xFFFF6D00,
            names = listOf(
                "Neon Ring", "Wave Scan", "Particle Burst", "Hologram", "Neon Gradient",
                "Ripple Effect", "Fire Ring", "Ice Crystals", "Golden Glow", "Digital Grid",
                "Liquid Flow", "Dark Mode", "Color Splash", "Orbit Ring", "Polygon Frame",
                "Galaxy Swirl", "Matrix Code", "Crystal Glass", "Smoke Effect", "Electric Pulse",
                "Minimal Circle", "3D Depth", "Flame Vortex", "Blue Light Beam", "Hexagon Glow",
                "Water Drop", "Digital Particles", "Aurora", "Steel Ring", "Heart Shape",
                "Leaf Design", "Lion Fire", "Butterfly", "Space Rocket", "Circuit Lines",
                "Split Scan", "Snowflake", "Neon Spiral", "Lava Flow", "Compass",
                "Sand Effect", "Ring Explosion", "Moon Phase", "Blue Flame", "Digital Frame",
                "Rainbow Ring", "Crystal Flower", "Data Stream", "Pulse Wave", "Infinity Loop"
            )
        ),

        // Sheet 3: Screenshot_2026-10-29_151643_7b5cb7f9.png (Designs 6..30 + 1..5)
        RawCategorySpec(
            number = 3,
            id = "sheet_3",
            title = "3. Styled Color Editions (30)",
            subtitle = "Wave Flow, Laser Sweep, Dual Color, Royal Crown & Glass Bubble",
            accentHex = 0xFFFF4081,
            secondaryHex = 0xFF00E5FF,
            items = listOf(
                RawDesignItem(1, "Classic Glow (Blue)"),
                RawDesignItem(2, "Neon Ring (Pink & Cyan)"),
                RawDesignItem(3, "Particle Burst (Violet)"),
                RawDesignItem(4, "3D Hologram (Aqua)"),
                RawDesignItem(5, "Gradient Pulse (Magenta)"),
                RawDesignItem(6, "Wave Flow (Purple & Blue)"),
                RawDesignItem(7, "Fire Effect (Orange)"),
                RawDesignItem(8, "Ice Crystal (Blue)"),
                RawDesignItem(9, "Rotating Ring (Tech Style)"),
                RawDesignItem(10, "Data Stream (Digital)"),
                RawDesignItem(11, "Hexagon Frame (Neon)"),
                RawDesignItem(12, "Smoke Effect (Colorful)"),
                RawDesignItem(13, "Scanning Bar (Blue)"),
                RawDesignItem(14, "Ripple Effect (Teal)"),
                RawDesignItem(15, "Dots Style (Modern)"),
                RawDesignItem(16, "Laser Sweep (Pink)"),
                RawDesignItem(17, "Minimal Circle (White)"),
                RawDesignItem(18, "Polygon Mesh (Futuristic)"),
                RawDesignItem(19, "Dual Color (Blue & Orange)"),
                RawDesignItem(20, "Liquid Flow (Ocean Blue)"),
                RawDesignItem(21, "Nature Vibes (Green)"),
                RawDesignItem(22, "Glitch Effect (Neon)"),
                RawDesignItem(23, "Royal Crown (Gold)"),
                RawDesignItem(24, "Orbit Particles (Space Style)"),
                RawDesignItem(25, "Energy Ring (Purple)"),
                RawDesignItem(26, "Triangle Frame (Neon)"),
                RawDesignItem(27, "Galaxy Swirl (Space)"),
                RawDesignItem(28, "Pulse Glow (Soft)"),
                RawDesignItem(29, "Pixel Fade (Digital)"),
                RawDesignItem(30, "Glass Bubble (3D Look)")
            )
        ),

        // Sheet 4: Screenshot_2026-10-29_151629_061d4ac6.png (50 Mobile Fingerprint Sensor Animations)
        RawCategorySpec(
            number = 4,
            id = "sheet_4",
            title = "4. 50 Mobile Sensor Animations",
            subtitle = "Security Shield, Progress Circle 100%, 3D Cube, Pyramid Glow & Future Interface",
            accentHex = 0xFF00E676,
            secondaryHex = 0xFF00B0FF,
            names = listOf(
                "Scanning Light", "Neon Glow", "Tech HUD", "Particle Burst", "Fire Energy",
                "Ice Crystal", "Nature Leaves", "Galaxy Orbit", "Digital Lines", "Rainbow Shift",
                "Security Shield", "Wave Pulse", "Matrix Dots", "Rotating Ring", "Hexagon Frame",
                "Golden Orbit", "Pixel Dissolve", "Liquid Flow", "Triangle Frame", "Ripple Effect",
                "Smoke Effect", "Laser Beam", "Electric Sparks", "Progress Circle", "Soft Glow",
                "Square Layers", "Floating Particles", "Royal Crown", "Dual Tone", "Vortex Spin",
                "Polygon Mesh", "Glass Orb", "Data Stream", "Heart Glow", "Glitch Effect",
                "Horizontal Scan", "Paint Stroke", "Concentric Rings", "Water Drop", "Starburst",
                "Moon Phase", "Circuit Board", "Snowflake", "Light Trail", "3D Cube",
                "Nebula", "Pyramid Glow", "Sound Wave", "Color Splash", "Future Interface"
            )
        ),

        // Sheet 5: Screenshot_2026-10-29_151615_7d41fbb6.png (25 Designs)
        RawCategorySpec(
            number = 5,
            id = "sheet_5",
            title = "5. Biometric HUD & Progress (25)",
            subtitle = "Scanning Line, Progress Fill 70%, Heart Beat, Beam Light & Sound Wave",
            accentHex = 0xFF18FFFF,
            secondaryHex = 0xFFFF4081,
            names = listOf(
                "Classic Glow", "Scanning Line", "Color Pulse", "Particle Burst", "Golden Ring",
                "Water Ripple", "Hexagon Frame", "Smoke Effect", "Progress Fill", "Crystal Shatter",
                "Heart Beat", "Beam Light", "Dot Circle", "Fire Ring", "Glitch Effect",
                "3D Hologram", "Orbit Rings", "Gradient Split", "Network Mesh", "Rotating Segments",
                "Nature Vines", "Ice Frost", "Sound Wave", "Paint Splash", "Minimal Elegant"
            )
        ),

        // Sheet 6: Screenshot_2026-10-29_151603_f19df055.png (25 Designs)
        RawCategorySpec(
            number = 6,
            id = "sheet_6",
            title = "6. Touch Interactive Series (25)",
            subtitle = "Hologram 3D, Neon Squares, Crystal Diamond, Butterfly Effect & Circular Scan",
            accentHex = 0xFF7C4DFF,
            secondaryHex = 0xFF00E5FF,
            names = listOf(
                "Classic Neon", "Ripple Effect", "Gradient Glow", "Particle Burst", "Hexagon Frame",
                "Fire Theme", "Ice Theme", "Liquid Flow", "Digital Particles", "Minimal Line",
                "Galaxy Ring", "Hologram 3D", "Neon Squares", "Wave Motion", "Geometric Shapes",
                "Nature Leaf", "Electric Sparks", "Orbit Rings", "Smoke Effect", "Crystal Diamond",
                "Butterfly Effect", "Code Matrix", "Sunset Gradient", "Broken Glass", "Circular Scan"
            )
        ),

        // Sheet 7: Screenshot_2026-10-29_151535_c4e732cf.png (50 Designs)
        RawCategorySpec(
            number = 7,
            id = "sheet_7",
            title = "7. Celestial & Geometric (50)",
            subtitle = "Butterfly Shape, Diamond Glow, Golden Crown, Heart Beat, Ribbon Flow & Celestial",
            accentHex = 0xFF536DFE,
            secondaryHex = 0xFFFFAB00,
            names = listOf(
                "Classic Scan", "Neon Pulse", "Holographic Ring", "Particle Formation", "Fire Effect",
                "Ice Effect", "Minimal Line", "Gradient Glow", "Double Ring", "Wave Flow",
                "Digital Particles", "Galaxy Swirl", "Hexagon Frame", "Circuit Lines", "Liquid Drop",
                "Solar Flare", "Butterfly Shape", "Nature Leaves", "Crystal Glass", "3D Depth",
                "Smoke Effect", "Neon Triangle", "Data Streams", "Orbit Rings", "Matrix Code",
                "Diamond Glow", "Pixel Break", "Light Trail", "Geometric Shape", "Aurora Flow",
                "Golden Crown", "Water Ripple", "Electric Arc", "Polygon Mesh", "Color Splash",
                "Moon Phase", "Leaf Frame", "Neon Square", "Spiral", "Fire & Ice",
                "Heart Beat", "Broken Glass", "Lava Flow", "Neon Hexagon", "Floating Blocks",
                "Star Field", "Ribbon Flow", "Metallic Ring", "Wave Pulse", "Celestial"
            )
        ),

        // Sheet 8: Screenshot_2026-10-29_151521_0eec2d71.png (50 Designs)
        RawCategorySpec(
            number = 8,
            id = "sheet_8",
            title = "8. Prism, Shield & Energy Core (50)",
            subtitle = "Prism Light, Cyber Shield, Energy Core, Infinity Loop, Crystal Ball & Neon Grid",
            accentHex = 0xFF00E5FF,
            secondaryHex = 0xFFFFD740,
            names = listOf(
                "Classic Glow", "Scan Line", "Ripple Effect", "Neon Pulse", "Gradient Flow",
                "Particle Burst", "Fire Effect", "Ice Effect", "Holographic", "Minimal Line",
                "3D Depth", "Hexagon Frame", "Circular Wave", "Matrix Style", "Digital Pixel",
                "Galaxy Swirl", "Lava Flow", "Water Droplet", "Electric Arc", "Prism Light",
                "Galaxy Ring", "Smoke Flow", "Gold Luxury", "Cyber Shield", "Neon Triangle",
                "Liquid Metal", "Crystal", "Nature Leaves", "Sound Wave", "Energy Core",
                "Circuit Board", "Orbit Rings", "Pixel Fade", "Dual Tone", "Fire & Ice",
                "Geometric Lines", "Bloom Effect", "Neon Circle", "Digital Dust", "Underwater",
                "Laser Beam", "Aurora", "Infinity Loop", "Smoke Rings", "Star Burst",
                "Metallic Ring", "Crystal Ball", "Wave Form", "Neon Grid", "Dark Mode"
            )
        ),

        // Sheet 9: Screenshot_2026-10-29_151748_a341c6a0.png (50 Designs)
        RawCategorySpec(
            number = 9,
            id = "sheet_9",
            title = "9. Cyber HUD, Chip & Vortex (50)",
            subtitle = "Circuit Fingerprint, HUD Interface, Security Lock, Protection Shield & Digital Hand",
            accentHex = 0xFF00B0FF,
            secondaryHex = 0xFF00E676,
            names = listOf(
                "Scan Mode", "Circuit Fingerprint", "3D Hologram", "Neon Gradient", "Particle Disperse",
                "HUD Interface", "Fire Effect", "Water Splash", "Golden Luxury", "Glitch Effect",
                "Security Lock", "Network Mesh", "Pixel Build", "Progress Ring", "Smoke Effect",
                "Wireframe", "Sensor Pad", "Binary Code", "Electricity", "Galaxy Style",
                "Rain Drop", "Butterfly Shape", "Triangle Frame", "Shatter Effect", "Matrix Style",
                "Laser Scan", "Dots Style", "Sound Wave", "3D Rotate", "Light Trails",
                "Half Digital", "Mobile Scan", "Rotating Ring", "Ice Effect", "Data Flow",
                "Heartbeat", "Color Splash", "Vortex", "Protection Shield", "3D Dot Mesh",
                "Royal Style", "Loading Bar", "Fire & Ice", "Chip Interface", "Cube Build",
                "Neon Triangle", "Energy Ribbons", "Cosmic Dust", "Infinity Loop", "Digital Hand"
            )
        ),

        // Sheet 10: Screenshot_2026-10-29_151736_9d124531.png (50 Designs)
        RawCategorySpec(
            number = 10,
            id = "sheet_10",
            title = "10. Dragon Flame & Portal Ring (50)",
            subtitle = "Dragon Flame, Crystal Core, Planet Horizon, Dot Mandala, Golden Vortex & Portal Ring",
            accentHex = 0xFFFF6D00,
            secondaryHex = 0xFFAA00FF,
            names = listOf(
                "Neon Pulse", "Gradient Glow", "Minimal Ring", "Holographic", "Particle Burst",
                "Wave Scan", "Fire Ring", "Ice Freeze", "Digital Grid", "Galaxy Swirl",
                "Matrix Code", "Liquid Flow", "Electric Arc", "Glass Effect", "Ripple Effect",
                "Golden Ring", "Aurora Light", "Smoke Effect", "Hexagon Frame", "Circuit Lines",
                "Star Burst", "Flower Bloom", "Water Drop", "Sand Wave", "3D Depth",
                "Color Splash", "Moon Phase", "Neon Square", "Pixel Burst", "Spiral Motion",
                "Dragon Flame", "Crystal Core", "Magnetic Ring", "Binary Stream", "Leaf Pattern",
                "Ocean Wave", "Fire & Ice", "Neon Triangle", "Glitch Effect", "Rainbow Ring",
                "Planet Horizon", "Snowflake", "Vertical Beam", "Neon Heart Aura", "Smoke Ring",
                "Cyber Shield", "Dot Mandala", "Golden Vortex", "Firework Burst", "Portal Ring"
            )
        ),

        // Sheet 11: Screenshot_2026-10-29_151709_cb222a88.png (50 Designs)
        RawCategorySpec(
            number = 11,
            id = "sheet_11",
            title = "11. DNA Helix & Feather Touch (50)",
            subtitle = "Feather Touch, Magnetic Field, 3D Cube, DNA Helix, Neon Vortex & Flame Spiral",
            accentHex = 0xFF651FFF,
            secondaryHex = 0xFF00E5FF,
            names = listOf(
                "Neon Glow", "Liquid Flow", "Digital Particles", "Ring Expand", "Gradient Glow",
                "Crystal Shine", "Fire Pulse", "Ice Frost", "Golden Aura", "Wave Scan",
                "Hexagon Frame", "Matrix Code", "Aurora Light", "Smoke Effect", "Geometric Lines",
                "3D Depth", "Electric Sparks", "Particle Ring", "Minimal Lines", "Dual Color",
                "Rotating Circles", "Fire & Ice", "Feather Touch", "Pixel Burst", "Galaxy Swirl",
                "Heart Shape", "Leaf Pattern", "Water Ripple", "Rainbow Glow", "Sound Wave",
                "Circuit Board", "Moon Surface", "Neon Triangle", "Magnetic Field", "Electric Ring",
                "Splatter Paint", "3D Cube", "Dotted Form", "DNA Helix", "Solar Flare",
                "Butterfly Effect", "Rain Drops", "Golden Particles", "Cyber Shield", "Smoke Ring",
                "Neon Vortex", "Flame Spiral", "Digital Grid", "Leaf Glow", "Infinity Loop"
            )
        ),

        // Sheet 12: Screenshot_2026-10-29_151656_b0f694c2.png (45 Designs)
        RawCategorySpec(
            number = 12,
            id = "sheet_12",
            title = "12. Honeycomb & Sunburst (45)",
            subtitle = "Vertical Scan, Square Scan, Royal Crown, Sunburst, Laser Scan & Honeycomb",
            accentHex = 0xFFFFAB00,
            secondaryHex = 0xFF00B0FF,
            names = listOf(
                "Classic Scan", "Neon Pulse", "Vertical Scan", "Particle Burst", "Gradient Glow",
                "Hexagon Frame", "Ripple Effect", "Fire Effect", "Ice Effect", "Orbit Ring",
                "Heart Shape", "Triangle Shape", "Square Scan", "Digital Dots", "Liquid Flow",
                "3D Layers", "Progress Ring", "Electric Sparks", "Hologram", "Glitch Effect",
                "Sound Wave", "Rotating Circle", "Flowing Lines", "Star Burst", "Royal Crown",
                "Network Mesh", "Energy Ring", "Nature Leaves", "Shatter Effect", "Infinity Loop",
                "Galaxy Spiral", "Water Ripple", "Neon Beam", "Circuit Board", "Color Splash",
                "Metal Ring", "Smoke Mist", "Dual Color", "Crystal", "Heartbeat",
                "Sunburst", "Ribbon Flow", "Laser Scan", "Honeycomb", "Aurora"
            )
        ),

        // Sheet 13: Screenshot_2026-10-29_151550_36a72a07.png (45 Designs)
        RawCategorySpec(
            number = 13,
            id = "sheet_13",
            title = "13. Crystal Prism & Starburst (45)",
            subtitle = "Forest Leaves, Cyber Shield, Flower Petals, Spiral Vortex, Crystal Prism & Blue Starburst",
            accentHex = 0xFF00E5FF,
            secondaryHex = 0xFFD500F9,
            names = listOf(
                "Neon Ring", "Pulsing Glow", "Minimal Lines", "Holographic", "Particle Burst",
                "Liquid Ripple", "Fire Flame", "Ice Crystal", "Electric Pulse", "Galaxy Swirl",
                "Hexagon Frame", "Circuit Board", "3D Depth", "Color Gradient", "Digital Fragment",
                "Water Droplets", "Aurora Light", "Gold Shine", "Smoke Effect", "Waveform",
                "Butterfly Shape", "Moon Phase", "Neon Triangle", "Rain Drop", "Shattered Glass",
                "Forest Leaves", "Space Orbit", "Digital Grid", "Energy Core", "Cyber Shield",
                "Fire & Ice", "Flower Petals", "Matrix Code", "Spiral Vortex", "Crystal Prism",
                "Neon Waves", "Geometric Dots", "Magnetic Field", "Solar Flare", "Smoke Ring",
                "Violet Particles", "Neon Square", "Liquid Metal", "Neon Heart", "Blue Starburst"
            )
        ),

        // Sheet 14: Screenshot_2026-10-29_151507_6f83905b.png (50 Designs)
        RawCategorySpec(
            number = 14,
            id = "sheet_14",
            title = "14. Sakura, Equalizer & Radar (50)",
            subtitle = "Sakura Petals, Crescent Moon, Equalizer Bars, Floral Wreath, Radar Target & Vortex Wave",
            accentHex = 0xFFFF4081,
            secondaryHex = 0xFF00E676,
            names = listOf(
                "Classic Scan", "Neon Glow", "Holographic Ring", "Liquid Flow", "Fire Effect",
                "Ice Crystal", "Green Pulse", "Galaxy Spiral", "Minimal", "Wave Line",
                "Particle Burst", "Hexagon Frame", "Gradient Glow", "Rotating Rings", "Digital Blocks",
                "Butterfly Effect", "Nature Leaves", "Electric Sparks", "Golden Crown", "Color Splash",
                "3D Depth", "Heart Shape", "Starry Sky", "Glass Effect", "Matrix Code",
                "Smoke Effect", "Ripple Effect", "Neon Triangle", "Solar Flare", "Digital Rain",
                "Wave Circle", "Magma Flow", "Crystal Shards", "Circuit Lines", "Dual Tone",
                "Floating Particles", "Fire & Ice", "Infinity Loop", "Geometric Shape", "Sakura Petals",
                "Crescent Moon", "Aurora Flame", "Pixel Matrix", "Equalizer Bars", "Floral Wreath",
                "Radar Target", "Water Droplet Ring", "Golden Swirl", "Cyber Shield", "Vortex Wave"
            )
        ),

        // Sheet 15: Screenshot_2026-10-29_151424_8717a175.png (50 Designs)
        RawCategorySpec(
            number = 15,
            id = "sheet_15",
            title = "15. Yin Yang, Lotus & Mandala (50)",
            subtitle = "Lotus Flower, Dragon Flame, Yin Yang, Sacred Polyhedron, Liquid Chrome & Mystic Mandala",
            accentHex = 0xFFFFD740,
            secondaryHex = 0xFF7C4DFF,
            names = listOf(
                "Classic Scan", "Neon Glow", "Pulse Ring", "Digital Particles", "Holographic",
                "Fire Effect", "Ice Effect", "Golden Glow", "Minimal Line", "Rainbow Gradient",
                "Liquid Flow", "Galaxy Swirl", "Matrix Style", "Electric Sparks", "3D Depth",
                "Wave Motion", "Hexagon Frame", "Ring Explosion", "Smoke Effect", "Water Ripple",
                "Lotus Flower", "Crystal Shine", "Neon Triangle", "Digital Grid", "Aurora Light",
                "Solar Flare", "Circuit Lines", "Broken Glass", "Pixel Art", "Biometric Rings",
                "Leaf Nature", "Heart Shape", "Dragon Flame", "Butterfly", "Space Orbit",
                "Cloud Sync", "Sand Effect", "Metal Ring", "Neon Fire", "Code Rain",
                "Yin Yang", "Split Fire & Ice", "Sonic Ripple", "Neon Square", "Sacred Polyhedron",
                "Color Burst", "Neon Star", "Crescent Moon", "Liquid Chrome", "Mystic Mandala"
            )
        ),

        // Sheet 16: Futuristic_Fingerprint_Unlock_Animation_Grid-1_4fd32dd3.png (50 Designs)
        RawCategorySpec(
            number = 16,
            id = "sheet_16",
            title = "16. Futuristic Unlock Grid I (50)",
            subtitle = "Mountain Peaks, DNA Helix, Solar Orbit, Compass, Warp Speed, Rose Petals & Universe Core",
            accentHex = 0xFF00E5FF,
            secondaryHex = 0xFFFF4081,
            names = listOf(
                "Neon Pulse", "Holographic Ring", "Particle Burst", "Liquid Flow", "Fire Glow",
                "Ice Crystal", "Green Scan", "Purple Aura", "Gold Shine", "Digital Grid",
                "Wave Motion", "Butterfly Effect", "Galaxy Rotate", "Hexagon Frame", "Mountain Peaks",
                "Smoke Trail", "Electric Sparks", "Circular Dots", "Rainbow Glow", "3D Elevation",
                "DNA Helix", "Matrix Code", "Solar Orbit", "Crystal Prism", "Neon Triangle",
                "Fire & Ice", "Sand Dust", "Water Ripple", "Pixel Break", "Light Beam",
                "Heart Shape", "Minimal Ring", "Vertical Lines", "Rotating Lines", "Space Particles",
                "Cyber Shield", "Leaf Pattern", "Flame Ring", "Water Droplets", "Compass",
                "Butterfly Wings", "Circuit Board", "Warp Speed", "Color Splash", "Moon Orbit",
                "Digital Fractals", "Neon Square", "Rose Petals", "Star Burst", "Universe Core"
            )
        ),

        // Sheet 17: Futuristic_Fingerprint_Animation_Catalog_93b0c1f9.png (51 Designs)
        RawCategorySpec(
            number = 17,
            id = "sheet_17",
            title = "17. Futuristic Catalog I (51)",
            subtitle = "Upward Flow, Day & Night, Dragon Wrap, Feather Touch, Topographic & Animal Aura",
            accentHex = 0xFF00B0FF,
            secondaryHex = 0xFFFF6D00,
            names = listOf(
                "Neon Ring Scan", "Light Ribbon", "Water Splash", "Fire Embers", "Glitch Pixel",
                "Gradient Glow", "Moon Phase", "Nature Leaves (Silver)", "Nature Leaves (Green)", "Electric Charge",
                "HUD Interface", "Particle Swirl", "Heart Pulse", "Prism Light", "Glass Break",
                "Disintegration", "Circle Scan", "Sand Wave", "Ice Crystal", "Upward Flow",
                "Liquid Flow", "Dark Energy", "3D Hologram", "Smoke Effect", "Progress Ring",
                "Galaxy Orbit", "Geometric Lines", "Metallic Gold", "Digital Rain", "Energy Frame",
                "Sound Wave", "Orbit Particles", "Day & Night", "Dragon Wrap", "Paint Drip",
                "Feather Touch", "Triangle Glow", "Polygon Burst", "Crown Effect", "Topographic",
                "Underwater", "Animal Aura", "Rotating Ring", "Ripple Effect", "Circuit Board",
                "Cube Grid", "Flower Bloom", "Hexagon Frame", "Smoke Mist", "Star Burst",
                "Binary Code"
            )
        ),

        // Sheet 18: Screenshot_2026-10-29_151452_c4a2de5d.png (50 Designs — Alternate Edition)
        RawCategorySpec(
            number = 18,
            id = "sheet_18",
            title = "18. Sakura & Radar Pro Edition (50)",
            subtitle = "Alternate High-Contrast Edition: Magma Flow, Sakura Petals, Equalizer Bars & Radar Target",
            accentHex = 0xFF00E676,
            secondaryHex = 0xFFFF4081,
            names = listOf(
                "Classic Scan", "Neon Glow", "Holographic Ring", "Liquid Flow", "Fire Effect",
                "Ice Crystal", "Green Pulse", "Galaxy Spiral", "Minimal", "Wave Line",
                "Particle Burst", "Hexagon Frame", "Gradient Glow", "Rotating Rings", "Digital Blocks",
                "Butterfly Effect", "Nature Leaves", "Electric Sparks", "Golden Crown", "Color Splash",
                "3D Depth", "Heart Shape", "Starry Sky", "Glass Effect", "Matrix Code",
                "Smoke Effect", "Ripple Effect", "Neon Triangle", "Solar Flare", "Digital Rain",
                "Wave Circle", "Magma Flow", "Crystal Shards", "Circuit Lines", "Dual Tone",
                "Floating Particles", "Fire & Ice", "Infinity Loop", "Geometric Shape", "Sakura Petals",
                "Crescent Moon", "Aurora Flame", "Pixel Matrix", "Equalizer Bars", "Floral Wreath",
                "Radar Target", "Water Droplet Ring", "Golden Swirl", "Cyber Shield", "Vortex Wave"
            )
        ),

        // Sheet 19: Screenshot_2026-10-29_151816_c81eb1a3.png (50 Designs — Touch Preview Edition)
        RawCategorySpec(
            number = 19,
            id = "sheet_19",
            title = "19. Futuristic Unlock Grid II (50)",
            subtitle = "Touch-Calibrated Edition: DNA Helix, Crystal Prism, Cyber Shield, Compass & Rose Petals",
            accentHex = 0xFF7C4DFF,
            secondaryHex = 0xFF00E5FF,
            names = listOf(
                "Neon Pulse", "Holographic Ring", "Particle Burst", "Liquid Flow", "Fire Glow",
                "Ice Crystal", "Green Scan", "Purple Aura", "Gold Shine", "Digital Grid",
                "Wave Motion", "Butterfly Effect", "Galaxy Rotate", "Hexagon Frame", "Mountain Peaks",
                "Smoke Trail", "Electric Sparks", "Circular Dots", "Rainbow Glow", "3D Elevation",
                "DNA Helix", "Matrix Code", "Solar Orbit", "Crystal Prism", "Neon Triangle",
                "Fire & Ice", "Sand Dust", "Water Ripple", "Pixel Break", "Light Beam",
                "Heart Shape", "Minimal Ring", "Vertical Lines", "Rotating Lines", "Space Particles",
                "Cyber Shield", "Leaf Pattern", "Flame Ring", "Water Droplets", "Compass",
                "Butterfly Wings", "Circuit Board", "Warp Speed", "Color Splash", "Moon Orbit",
                "Digital Fractals", "Neon Square", "Rose Petals", "Star Burst", "Universe Core"
            )
        ),

        // Sheet 20: Screenshot_2026-10-29_151830_b6603feb.png (51 Designs — AMOLED Edition)
        RawCategorySpec(
            number = 20,
            id = "sheet_20",
            title = "20. Futuristic Catalog II (51)",
            subtitle = "AMOLED Deep-Contrast Edition: Light Ribbon, Upward Flow, Dragon Wrap & Binary Code",
            accentHex = 0xFFFF6D00,
            secondaryHex = 0xFF00B0FF,
            names = listOf(
                "Neon Ring Scan", "Light Ribbon", "Water Splash", "Fire Embers", "Glitch Pixel",
                "Gradient Glow", "Moon Phase", "Nature Leaves (Silver)", "Nature Leaves (Green)", "Electric Charge",
                "HUD Interface", "Particle Swirl", "Heart Pulse", "Prism Light", "Glass Break",
                "Disintegration", "Circle Scan", "Sand Wave", "Ice Crystal", "Upward Flow",
                "Liquid Flow", "Dark Energy", "3D Hologram", "Smoke Effect", "Progress Ring",
                "Galaxy Orbit", "Geometric Lines", "Metallic Gold", "Digital Rain", "Energy Frame",
                "Sound Wave", "Orbit Particles", "Day & Night", "Dragon Wrap", "Paint Drip",
                "Feather Touch", "Triangle Glow", "Polygon Burst", "Crown Effect", "Topographic",
                "Underwater", "Animal Aura", "Rotating Ring", "Ripple Effect", "Circuit Board",
                "Cube Grid", "Flower Bloom", "Hexagon Frame", "Smoke Mist", "Star Burst",
                "Binary Code"
            )
        )
    )
}

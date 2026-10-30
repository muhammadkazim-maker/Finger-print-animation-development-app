package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberpunkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF002024),
    primaryContainer = Color(0xFF003B42),
    onPrimaryContainer = NeonCyan,
    secondary = CyberMagenta,
    onSecondary = Color(0xFF2B0027),
    secondaryContainer = Color(0xFF4B0043),
    onSecondaryContainer = Color(0xFFFFB1F3),
    tertiary = MatrixGreen,
    onTertiary = Color(0xFF002916),
    background = CyberObsidian,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = PlasmaCrimson
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberpunkColorScheme,
        typography = Typography,
        content = content
    )
}

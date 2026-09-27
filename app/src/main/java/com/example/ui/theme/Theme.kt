package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanEdge,
    onPrimary = Color.Black,
    primaryContainer = CyanEdgeContainer,
    onPrimaryContainer = CyanEdge,
    secondary = NeonAmber,
    onSecondary = Color.Black,
    secondaryContainer = NeonAmberContainer,
    onSecondaryContainer = NeonAmber,
    tertiary = RoadGreen,
    onTertiary = Color.Black,
    tertiaryContainer = RoadGreenContainer,
    onTertiaryContainer = RoadGreen,
    error = HazardRed,
    onError = Color.White,
    errorContainer = HazardRedContainer,
    onErrorContainer = HazardRed,
    background = RoadDeepNavy,
    onBackground = TextPrimary,
    surface = RoadDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = RoadSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = RoadBorder,
    outlineVariant = Color(0xFF1E293B)
)

private val LightColorScheme = darkColorScheme( // We prefer a dark automotive cockpit theme by default
    primary = CyanEdge,
    onPrimary = Color.Black,
    primaryContainer = CyanEdgeContainer,
    secondary = NeonAmber,
    onSecondary = Color.Black,
    tertiary = RoadGreen,
    background = RoadDeepNavy,
    surface = RoadDarkSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun RoadVisionTheme(
    darkTheme: Boolean = true, // Default to sleek automotive night cockpit HUD
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

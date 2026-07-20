package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val WarmLightColorScheme = lightColorScheme(
    primary = Terracotta40,               // Main primary brand terracotta
    onPrimary = WarmIvory,
    primaryContainer = PeachContainer90,  // Selected tab / highlight container
    onPrimaryContainer = WarmBrown10,
    secondary = Sage40,                   // Secondary sage green
    onSecondary = WarmIvory,
    secondaryContainer = SageContainer90, // Unselected tab / secondary container
    onSecondaryContainer = OliveBrown10,
    tertiary = Amber40,                   // Golden amber accent
    onTertiary = WarmIvory,
    tertiaryContainer = AmberContainer90,
    onTertiaryContainer = GoldenBrown10,
    background = WarmIvory,               // Main background tint
    onBackground = WarmNearBlack,
    surface = WarmIvory,                  // Surface container matches bg for warm flow
    onSurface = WarmNearBlack,
    surfaceVariant = WarmTan90,           // Bottom nav and cards
    onSurfaceVariant = WarmTaupe40,
    outline = WarmOutline40,              // Warm borders
    outlineVariant = WarmOutlineVariant90
)

private val WarmDarkColorScheme = darkColorScheme(
    primary = Terracotta80,
    onPrimary = WarmBrown20,
    primaryContainer = TerracottaContainer30,
    onPrimaryContainer = PeachContainer90,
    secondary = Sage80,
    onSecondary = OliveBrown20,
    secondaryContainer = SageContainer30,
    onSecondaryContainer = SageContainer90,
    tertiary = Amber80,
    onTertiary = GoldenBrown20,
    tertiaryContainer = AmberContainer30,
    onTertiaryContainer = AmberContainer90,
    background = WarmDarkBackground,
    onBackground = WarmDarkOnBackground,
    surface = WarmDarkBackground,
    onSurface = WarmDarkOnBackground,
    surfaceVariant = WarmDarkSurfaceVariant,
    onSurfaceVariant = WarmDarkOnSurfaceVariant,
    outline = WarmDarkOutline,
    outlineVariant = WarmDarkOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize our custom Warm & Friendly palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) WarmDarkColorScheme else WarmLightColorScheme
    val extendedColors = if (darkTheme) DarkAppExtendedColors else LightAppExtendedColors

    CompositionLocalProvider(LocalAppExtendedColors provides extendedColors) {
        MaterialTheme(colorScheme = colorScheme, shapes = AppShapes, typography = Typography, content = content)
    }
}

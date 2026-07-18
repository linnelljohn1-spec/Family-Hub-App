package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val SleekLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),         // Main primary brand purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF), // Selected tab / highlight container
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFFD0BCFF),        // Secondary light purple
    onSecondary = Color(0xFF21005D),
    secondaryContainer = Color(0xFFF3EDF7), // Unselected tab / secondary container
    onSecondaryContainer = Color(0xFF49454F),
    background = Color(0xFFFEF7FF),       // Main background tint
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),          // Surface container matches bg for sleek flow
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFF3EDF7),   // Bottom nav and cards
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),          // Sleek borders
    outlineVariant = Color(0xFFE6E1E5)
)

private val SleekDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize our custom Sleek Theme palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SleekDarkColorScheme else SleekLightColorScheme
    
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

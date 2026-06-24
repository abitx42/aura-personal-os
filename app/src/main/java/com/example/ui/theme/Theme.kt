package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PremiumDarkColorScheme = darkColorScheme(
    primary = AuraCyanNeon,
    onPrimary = Color(0xFF001518),
    primaryContainer = AuraCyanMuted,
    onPrimaryContainer = Color.White,
    secondary = AuraPurpleAccent,
    onSecondary = Color.White,
    tertiary = AuraCopperWarm,
    onTertiary = Color.Black,
    background = AuraObsidian,
    onBackground = Color.White,
    surface = AuraCharcoalBase,
    onSurface = AuraWhiteMedium,
    surfaceVariant = AuraSlateCard,
    onSurfaceVariant = AuraWhiteMedium,
    outline = AuraSlateLight
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = AuraCyanMuted,
    onPrimary = Color.White,
    primaryContainer = AuraCyanNeon,
    onPrimaryContainer = Color(0xFF001518),
    secondary = AuraPurpleAccent,
    onSecondary = Color.White,
    tertiary = AuraCopperWarm,
    onTertiary = Color.White,
    background = Color(0xFFFAFBFD),
    onBackground = Color(0xFF1B1D22),
    surface = Color.White,
    onSurface = Color(0xFF2C2E35),
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF454B54),
    outline = Color(0xFFCFD5DF)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "DARK",
    themePalette: String = "CYAN_GLOW",
    content: @Composable () -> Unit
) {
    val isDark = themeMode != "LIGHT"
    
    // Choose backgrounds
    val (bg, surface, card, lightOutline) = when (themeMode) {
        "AMOLED" -> Quadruple(
            Color(0xFF000000), // Pure Black background
            Color(0xFF0B0C10), // Charcoal Base
            Color(0xFF121620), // Slate Card
            Color(0xFF1E2433)  // Slate Light
        )
        "LIGHT" -> Quadruple(
            Color(0xFFFAFBFD), // Premium light bone-white background
            Color(0xFFF1F3F6), // Warm light grey surface
            Color(0xFFFFFFFF), // White Card
            Color(0xFFE2E7ED)  // Lighter gray outline
        )
        else -> Quadruple( // "DARK" standard Aura
            Color(0xFF0C0E12), // Obsidian base
            Color(0xFF14171E), // Warm charcoal
            Color(0xFF1D222E), // Slate card
            Color(0xFF2B3244)  // Slate light
        )
    }
    
    // Choose accents
    val (primary, secondary, tertiary) = when (themePalette) {
        "EMERALD_GARDEN" -> Triple(
            Color(0xFF2ECD71), // Mint Emerald
            Color(0xFF00B0FF), // Ocean Indigo
            Color(0xFFFFC300)  // Golden Plum
        )
        "RADIANT_SUNSET" -> Triple(
            Color(0xFFFF5722), // Radiant Orange
            Color(0xFFE91E63), // Warm Red/Pink
            Color(0xFFFFC107)  // Sunset Gold
        )
        "ROYAL_AMETHYST" -> Triple(
            Color(0xFFBB86FC), // Orchid Purple
            Color(0xFF7C4DFF), // Tech Purple
            Color(0xFF03DAC6)  // Cool Cyan
        )
        "OCEAN_BREEZE" -> Triple(
            Color(0xFF0288D1), // Sky Blue
            Color(0xFF00E676), // Deep Green
            Color(0xFFFFD54F)  // Sand Yellow
        )
        else -> Triple( // "CYAN_GLOW"
            Color(0xFF00E5FF), // Digital neon cyan
            Color(0xFF7C4DFF), // Tech purple
            Color(0xFFFFA726)  // Copper warm
        )
    }

    // Choose text colors based on Dark/Light
    val (textMedium, textMuted) = if (isDark) {
        Pair(Color(0xFFCFD8DC), Color(0xFF90A4AE))
    } else {
        Pair(Color(0xFF1F2937), Color(0xFF6B7280)) // Deep gray and Slate gray for light theme
    }

    // Update global variables in Color.kt
    AuraObsidian = bg
    AuraCharcoalBase = surface
    AuraSlateCard = card
    AuraSlateLight = lightOutline
    AuraCyanNeon = primary
    AuraCyanMuted = primary.copy(alpha = 0.7f)
    AuraPurpleAccent = secondary
    AuraCopperWarm = tertiary
    AuraWhiteMedium = textMedium
    AuraWhiteMuted = textMuted

    // Create the material color scheme
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            primaryContainer = primary.copy(alpha = 0.2f),
            onPrimaryContainer = Color.White,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = tertiary,
            onTertiary = Color.Black,
            background = bg,
            onBackground = Color.White,
            surface = surface,
            onSurface = textMedium,
            surfaceVariant = card,
            onSurfaceVariant = textMedium,
            outline = lightOutline
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.2f),
            onPrimaryContainer = Color(0xFF1B1D22),
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = tertiary,
            onTertiary = Color.White,
            background = bg,
            onBackground = Color(0xFF1B1D22),
            surface = surface,
            onSurface = textMedium,
            surfaceVariant = card,
            onSurfaceVariant = textMedium,
            outline = lightOutline
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = bg.toArgb()
            window.navigationBarColor = bg.toArgb()
            val viewCompat = WindowCompat.getInsetsController(window, view)
            viewCompat.isAppearanceLightStatusBars = !isDark
            viewCompat.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private data class Triple<A, B, C>(val first: A, val second: B, val third: C)
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

package com.glide.android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * Glide's look (D-031). PLACEHOLDER brand colours until the team sends its own: violet + rose, chosen so text on them
 * meets WCAG AA contrast. Swapping the brand = changing the values in this file only.
 */

internal object BrandColors {
    val Violet = Color(0xFF6D28D9)
    val VioletDeep = Color(0xFF4C1D95)
    val VioletLight = Color(0xFFC4B5FD)
    val Rose = Color(0xFFBE185D)
    val RoseLight = Color(0xFFF9A8D4)
    val Amber = Color(0xFFB45309)
}

/** The hero gradient behind headers (login, home). White text on it is ≥ 4.5:1. */
val BrandGradient: Brush = Brush.linearGradient(listOf(BrandColors.VioletDeep, BrandColors.Violet, BrandColors.Rose))

internal val LightColors =
    lightColorScheme(
        primary = BrandColors.Violet,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEDE4FF),
        onPrimaryContainer = Color(0xFF2E1065),
        secondary = BrandColors.Rose,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE1EE),
        onSecondaryContainer = Color(0xFF5B0A2E),
        tertiary = BrandColors.Amber,
        onTertiary = Color.White,
        background = Color(0xFFFCFAFF),
        onBackground = Color(0xFF1C1726),
        surface = Color(0xFFFCFAFF),
        onSurface = Color(0xFF1C1726),
        surfaceVariant = Color(0xFFEFE9F7),
        onSurfaceVariant = Color(0xFF4A4458),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF8F4FD),
        surfaceContainer = Color(0xFFF3EEFA),
        surfaceContainerHigh = Color(0xFFEDE7F6),
        surfaceContainerHighest = Color(0xFFE7E0F1),
        outline = Color(0xFF7A7289),
        outlineVariant = Color(0xFFD2CADF),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )

internal val DarkColors =
    darkColorScheme(
        primary = BrandColors.VioletLight,
        onPrimary = Color(0xFF2E1065),
        primaryContainer = Color(0xFF4C1D95),
        onPrimaryContainer = Color(0xFFEDE4FF),
        secondary = BrandColors.RoseLight,
        onSecondary = Color(0xFF5B0A2E),
        secondaryContainer = Color(0xFF831843),
        onSecondaryContainer = Color(0xFFFFE1EE),
        tertiary = Color(0xFFFCD34D),
        onTertiary = Color(0xFF451A03),
        background = Color(0xFF131018),
        onBackground = Color(0xFFEAE5F2),
        surface = Color(0xFF131018),
        onSurface = Color(0xFFEAE5F2),
        surfaceVariant = Color(0xFF2B2635),
        onSurfaceVariant = Color(0xFFCCC4DA),
        surfaceContainerLowest = Color(0xFF0E0B13),
        surfaceContainerLow = Color(0xFF1B1722),
        surfaceContainer = Color(0xFF1F1B27),
        surfaceContainerHigh = Color(0xFF2A2532),
        surfaceContainerHighest = Color(0xFF35303D),
        outline = Color(0xFF968EA4),
        outlineVariant = Color(0xFF4A4458),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )

package com.glide.android.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * Glide's colours, taken from the team's mockup (D-041, docs/design/customer-flow-1.webp): brand red on warm white with
 * light-pink tints. Light mode only (D-040). Every text colour here passes WCAG AA on its background (ColorContrastTest).
 */

internal object BrandColors {
    /** The mockup's red is #E4242E; this is 1% darker so white text on red buttons reads at ≥ 4.5:1. */
    val Red = Color(0xFFE02430)
    val RedDeep = Color(0xFFB0141E)
    val Ink = Color(0xFF16131A)
    val WarmWhite = Color(0xFFFDF9F9)
    val PinkTint = Color(0xFFFDF1F0)

    // The three petals of the leaf mark.
    val PetalPeach = Color(0xFFF8B49A)
    val PetalOrange = Color(0xFFF26B3A)
    val PetalRed = Red

    /** Rating stars. Decorative only: the number next to them carries the meaning. */
    val Star = Color(0xFFE39915)
}

/** Placeholder behind a salon until its photo exists. White text on it is ≥ 4.5:1. */
val BrandGradient: Brush = Brush.linearGradient(listOf(BrandColors.Red, BrandColors.RedDeep))

internal val GlideColors =
    lightColorScheme(
        primary = BrandColors.Red,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFDE6E5),
        onPrimaryContainer = Color(0xFF7A0E16),
        secondary = BrandColors.Ink,
        onSecondary = Color.White,
        secondaryContainer = BrandColors.PinkTint,
        onSecondaryContainer = Color(0xFF5A1018),
        tertiary = Color(0xFF8A5A00),
        onTertiary = Color.White,
        background = BrandColors.WarmWhite,
        onBackground = BrandColors.Ink,
        surface = Color.White,
        onSurface = BrandColors.Ink,
        surfaceVariant = Color(0xFFF6EDEC),
        onSurfaceVariant = Color(0xFF5E5660),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFFFAFA),
        surfaceContainer = Color(0xFFFDF3F2),
        surfaceContainerHigh = Color(0xFFFBEDEC),
        surfaceContainerHighest = Color(0xFFF6E6E5),
        outline = Color(0xFF8E8590),
        outlineVariant = Color(0xFFEDE3E3),
        inverseSurface = Color(0xFF2A2326),
        inverseOnSurface = Color(0xFFFBEFEF),
        inversePrimary = Color(0xFFFFB3AE),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFFFE2DF),
        onErrorContainer = Color(0xFF5C0A06),
    )

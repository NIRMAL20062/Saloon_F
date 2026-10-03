package com.glide.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.glide.android.R

/** Poppins, the rounded geometric font of the team's mockup (D-041). OFL 1.1, see android/licenses. */
val Poppins =
    FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
    )

private val base = Typography()

private fun TextStyle.poppins(
    weight: FontWeight,
    letterSpacingSp: Float? = null,
) = copy(fontFamily = Poppins, fontWeight = weight, letterSpacing = letterSpacingSp?.sp ?: letterSpacing)

/** Material 3 type scale in Poppins: bold headings, medium labels, regular body text. */
internal val GlideTypography =
    Typography(
        displayLarge = base.displayLarge.poppins(FontWeight.Bold),
        displayMedium = base.displayMedium.poppins(FontWeight.Bold),
        displaySmall = base.displaySmall.poppins(FontWeight.Bold, -0.5f),
        headlineLarge = base.headlineLarge.poppins(FontWeight.Bold, -0.5f),
        headlineMedium = base.headlineMedium.poppins(FontWeight.Bold, -0.25f),
        headlineSmall = base.headlineSmall.poppins(FontWeight.SemiBold),
        titleLarge = base.titleLarge.poppins(FontWeight.SemiBold),
        titleMedium = base.titleMedium.poppins(FontWeight.SemiBold),
        titleSmall = base.titleSmall.poppins(FontWeight.Medium),
        bodyLarge = base.bodyLarge.poppins(FontWeight.Normal),
        bodyMedium = base.bodyMedium.poppins(FontWeight.Normal),
        bodySmall = base.bodySmall.poppins(FontWeight.Normal),
        labelLarge = base.labelLarge.poppins(FontWeight.Medium, 0.2f),
        labelMedium = base.labelMedium.poppins(FontWeight.Medium),
        labelSmall = base.labelSmall.poppins(FontWeight.Medium),
    )

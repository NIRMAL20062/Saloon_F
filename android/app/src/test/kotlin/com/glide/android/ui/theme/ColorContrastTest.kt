package com.glide.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG AA: every text colour on the background it is used on is at least 4.5:1, in light and dark (APP-011, D-031).
 * If a brand colour is swapped in Color.kt and this fails, the new colour is too weak for text.
 */
class ColorContrastTest {
    @Test
    fun lightSchemeTextIsReadable() = assertReadable(LightColors, "light")

    @Test
    fun darkSchemeTextIsReadable() = assertReadable(DarkColors, "dark")

    @Test
    fun whiteTextOnTheBrandGradientIsReadable() {
        listOf(BrandColors.VioletDeep, BrandColors.Violet, BrandColors.Rose).forEach { stop ->
            assertAtLeast(Color.White, stop, "white on gradient $stop")
        }
    }

    private fun assertReadable(
        s: ColorScheme,
        name: String,
    ) {
        val pairs =
            mapOf(
                "onPrimary/primary" to (s.onPrimary to s.primary),
                "onPrimaryContainer/primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
                "onSecondary/secondary" to (s.onSecondary to s.secondary),
                "onSecondaryContainer/secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
                "onTertiary/tertiary" to (s.onTertiary to s.tertiary),
                "onSurface/surface" to (s.onSurface to s.surface),
                "onSurfaceVariant/surface" to (s.onSurfaceVariant to s.surface),
                "onSurface/card (surfaceContainerLow)" to (s.onSurface to s.surfaceContainerLow),
                "onSurfaceVariant/card" to (s.onSurfaceVariant to s.surfaceContainerLow),
                "onSurfaceVariant/neutral chip" to (s.onSurfaceVariant to s.surfaceContainerHighest),
                "error text/surface" to (s.error to s.surface),
                "primary text/surface" to (s.primary to s.surface),
                "onError/error" to (s.onError to s.error),
                "onErrorContainer/errorContainer" to (s.onErrorContainer to s.errorContainer),
                "snackbar text" to (s.inverseOnSurface to s.inverseSurface),
                "snackbar action" to (s.inversePrimary to s.inverseSurface),
            )
        pairs.forEach { (label, colors) -> assertAtLeast(colors.first, colors.second, "$name $label") }
    }

    private fun assertAtLeast(
        text: Color,
        background: Color,
        label: String,
    ) {
        val ratio = contrast(text, background)
        assertTrue("$label is %.2f:1, needs 4.5:1".format(ratio), ratio >= MIN_TEXT_CONTRAST)
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5
    }
}

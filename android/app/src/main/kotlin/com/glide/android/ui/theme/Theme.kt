package com.glide.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Glide's design system root (D-031): brand colours (Color.kt), type scale (Type.kt), shapes and spacing (Shape.kt).
 * Wallpaper-based dynamic colours are off on purpose, so the app looks like Glide on every phone. Light and dark follow
 * the system setting.
 */
@Composable
fun GlideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GlideTypography,
        shapes = GlideShapes,
        content = content,
    )
}

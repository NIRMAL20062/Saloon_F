package com.glide.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Glide's design system root (D-031, D-041): brand colours (Color.kt), Poppins type (Type.kt), shapes and spacing
 * (Shape.kt). **Light mode only** (D-040): the app looks the same whatever the phone's dark-mode setting. Wallpaper-based
 * dynamic colours are off, so it looks like Glide on every phone.
 */
@Composable
fun GlideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlideColors,
        typography = GlideTypography,
        shapes = GlideShapes,
        content = content,
    )
}

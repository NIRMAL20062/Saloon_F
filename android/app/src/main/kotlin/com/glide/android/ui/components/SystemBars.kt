package com.glide.android.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Status-bar icon colour for the current screen. The app's default is dark icons on its light screens (MainActivity,
 * D-040). A screen with a dark photo behind the status bar (the welcome screen) calls this with `darkIcons = false` so the
 * clock and battery stay visible; the default comes back when the screen leaves.
 */
@Composable
fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(darkIcons) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = darkIcons
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }
}

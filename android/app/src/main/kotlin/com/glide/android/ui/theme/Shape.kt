package com.glide.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Soft, rounded corners everywhere (cards 24 dp, sheets 32 dp). */
internal val GlideShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

/** Spacing steps. Screens use these instead of one-off numbers. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** Minimum height of anything tappable: above the 48 dp accessibility minimum. */
val TouchTarget = 56.dp

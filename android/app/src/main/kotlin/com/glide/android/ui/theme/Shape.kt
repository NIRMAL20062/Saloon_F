package com.glide.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corners as in the mockup (D-041): fields and chips 12 dp, cards 16 dp, sheets 24 dp; buttons are pills. */
internal val GlideShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

/** Fully rounded ends: every button and filter chip. */
val PillShape = RoundedCornerShape(percent = 50)

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

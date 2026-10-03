package com.glide.android.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The few icons the app needs, drawn here on a 24 × 24 grid so we don't pull in the large Material icons library.
 * Outline style, 2 dp strokes; they take the current content colour (tint) like any icon.
 */
object GlideIcons {
    val Back: ImageVector by lazy {
        outline("back") {
            moveTo(15f, 5f)
            lineTo(8f, 12f)
            lineTo(15f, 19f)
        }
    }
    val Close: ImageVector by lazy {
        outline("close") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }
    val Check: ImageVector by lazy {
        outline("check") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 7f)
        }
    }
    val ChevronRight: ImageVector by lazy {
        outline("chevron") {
            moveTo(9f, 5f)
            lineTo(16f, 12f)
            lineTo(9f, 19f)
        }
    }
    val Location: ImageVector by lazy {
        outline("location") {
            moveTo(12f, 21.5f)
            curveTo(12f, 21.5f, 19f, 15f, 19f, 9.5f)
            arcTo(7f, 7f, 0f, false, false, 5f, 9.5f)
            curveTo(5f, 15f, 12f, 21.5f, 12f, 21.5f)
            close()
            moveTo(12f, 7f)
            arcTo(2.5f, 2.5f, 0f, true, true, 12f, 12f)
            arcTo(2.5f, 2.5f, 0f, true, true, 12f, 7f)
        }
    }
    val Star: ImageVector by lazy {
        filled("star") {
            moveTo(12f, 2.8f)
            lineTo(14.8f, 8.6f)
            lineTo(21.1f, 9.4f)
            lineTo(16.5f, 13.8f)
            lineTo(17.7f, 20.1f)
            lineTo(12f, 17.1f)
            lineTo(6.3f, 20.1f)
            lineTo(7.5f, 13.8f)
            lineTo(2.9f, 9.4f)
            lineTo(9.2f, 8.6f)
            close()
        }
    }
    val Calendar: ImageVector by lazy {
        outline("calendar") {
            moveTo(5f, 6f)
            lineTo(19f, 6f)
            lineTo(19f, 20f)
            lineTo(5f, 20f)
            close()
            moveTo(5f, 10f)
            lineTo(19f, 10f)
            moveTo(9f, 3.5f)
            lineTo(9f, 7.5f)
            moveTo(15f, 3.5f)
            lineTo(15f, 7.5f)
        }
    }
    val Search: ImageVector by lazy {
        outline("search") {
            moveTo(10.5f, 4f)
            arcTo(6.5f, 6.5f, 0f, true, true, 10.5f, 17f)
            arcTo(6.5f, 6.5f, 0f, true, true, 10.5f, 4f)
            moveTo(15.5f, 15.5f)
            lineTo(20f, 20f)
        }
    }

    private fun outline(
        name: String,
        path: PathBuilder.() -> Unit,
    ) = ImageVector
        .Builder(name, 24.dp, 24.dp, 24f, 24f)
        .path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = path,
        ).build()

    private fun filled(
        name: String,
        path: PathBuilder.() -> Unit,
    ) = ImageVector
        .Builder(name, 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black), pathBuilder = path)
        .build()
}

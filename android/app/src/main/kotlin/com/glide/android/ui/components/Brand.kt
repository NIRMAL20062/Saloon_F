package com.glide.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glide.android.ui.theme.BrandColors
import com.glide.android.ui.theme.Poppins

/**
 * The "Glide" wordmark from the team's mockup (D-041): bold Poppins with a red **G**. On a photo ([onPhoto]) it's all
 * white with the red leaf mark in front, as on the welcome screen. [name] is the app name from resources.
 */
@Composable
fun GlideWordmark(
    name: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 24.sp,
    onPhoto: Boolean = false,
) {
    val rest = if (onPhoto) Color.White else BrandColors.Ink
    val first = if (onPhoto) Color.White else BrandColors.Red
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = name },
    ) {
        if (onPhoto) LeafMark(size = (size.value * 0.9f).dp)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = first)) { append(name.take(1)) }
                withStyle(SpanStyle(color = rest)) { append(name.drop(1)) }
            },
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = size,
            letterSpacing = (-0.5).sp,
        )
    }
}

/** The three-petal leaf of the logo: peach on top, orange left, red right. */
@Composable
fun LeafMark(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val base = Offset(w * 0.5f, h * 0.95f)
        petal(base, Offset(w * 0.5f, h * 0.05f), w * 0.22f, BrandColors.PetalPeach)
        petal(base, Offset(w * 0.06f, h * 0.30f), w * 0.22f, BrandColors.PetalOrange)
        petal(base, Offset(w * 0.94f, h * 0.30f), w * 0.22f, BrandColors.PetalRed)
    }
}

private fun DrawScope.petal(
    base: Offset,
    tip: Offset,
    width: Float,
    color: Color,
) {
    val mid = Offset((base.x + tip.x) / 2, (base.y + tip.y) / 2)
    val dx = tip.x - base.x
    val dy = tip.y - base.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy)
    val nx = -dy / length * width
    val ny = dx / length * width
    val path =
        Path().apply {
            moveTo(base.x, base.y)
            quadraticTo(mid.x + nx, mid.y + ny, tip.x, tip.y)
            quadraticTo(mid.x - nx, mid.y - ny, base.x, base.y)
            close()
        }
    drawPath(path, color)
}

/** India's flag, drawn small for the +91 box. Decorative: the "+91" text next to it carries the meaning. */
@Composable
fun IndiaFlag(
    modifier: Modifier = Modifier,
    width: Dp = 22.dp,
) {
    Canvas(modifier.size(width = width, height = width * 2 / 3)) {
        val stripe = size.height / 3
        val corner = CornerRadius(2.dp.toPx())
        drawRoundRect(Color(0xFFFF9933), size = Size(size.width, size.height), cornerRadius = corner)
        drawRect(Color.White, topLeft = Offset(0f, stripe), size = Size(size.width, stripe))
        drawRect(Color(0xFF138808), topLeft = Offset(0f, stripe * 2), size = Size(size.width, stripe - 0.5f))
        drawCircle(
            Color(0xFF000080),
            radius = stripe * 0.38f,
            center = center,
            style =
                Stroke(
                    width =
                        1.dp.toPx() * 0.8f,
                ),
        )
    }
}

/**
 * The login illustration from the mockup: an outlined phone with a red message bubble, on soft peach leaves.
 * Decorative only.
 */
@Composable
fun PhoneMessageIllustration(modifier: Modifier = Modifier) {
    val red = BrandColors.Red
    val leaf = BrandColors.PetalPeach.copy(alpha = 0.45f)
    val leafDeep = BrandColors.PetalOrange.copy(alpha = 0.25f)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Leaves behind the phone.
        listOf(
            Triple(Offset(w * 0.22f, h * 0.95f), -40f, leaf),
            Triple(Offset(w * 0.30f, h * 0.98f), -15f, leafDeep),
            Triple(Offset(w * 0.78f, h * 0.95f), 40f, leaf),
            Triple(Offset(w * 0.70f, h * 0.98f), 15f, leafDeep),
        ).forEach { (at, angle, color) ->
            rotate(angle, pivot = at) {
                petal(at, Offset(at.x, at.y - h * 0.62f), w * 0.07f, color)
            }
        }
        // The phone.
        val phoneW = w * 0.30f
        val phoneH = h * 0.78f
        val phoneTop = Offset((w - phoneW) / 2, h - phoneH)
        val stroke = 3.dp.toPx()
        drawRoundRect(
            Color.White,
            topLeft = phoneTop,
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(18.dp.toPx()),
        )
        drawRoundRect(
            red,
            topLeft = phoneTop,
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(18.dp.toPx()),
            style = Stroke(stroke),
        )
        drawRoundRect(
            red.copy(alpha = 0.25f),
            topLeft = Offset(phoneTop.x + phoneW * 0.32f, phoneTop.y + phoneH * 0.05f),
            size = Size(phoneW * 0.36f, 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        listOf(0.45f, 0.55f, 0.65f).forEachIndexed { i, y ->
            drawRoundRect(
                red.copy(alpha = 0.15f),
                topLeft = Offset(phoneTop.x + phoneW * 0.18f, phoneTop.y + phoneH * y),
                size = Size(phoneW * (if (i == 2) 0.40f else 0.64f), 6.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
        // The message bubble with three dots.
        val bubbleW = w * 0.24f
        val bubbleH = h * 0.24f
        val bubbleTop = Offset(phoneTop.x + phoneW * 0.62f, phoneTop.y + phoneH * 0.20f)
        drawRoundRect(
            red,
            topLeft = bubbleTop,
            size = Size(bubbleW, bubbleH),
            cornerRadius = CornerRadius(12.dp.toPx()),
        )
        repeat(3) { i ->
            drawCircle(
                Color.White,
                radius = bubbleH * 0.09f,
                center = Offset(bubbleTop.x + bubbleW * (0.28f + i * 0.22f), bubbleTop.y + bubbleH / 2),
            )
        }
    }
}

package com.glide.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glide.android.ui.theme.BrandGradient
import com.glide.android.ui.theme.Spacing

/**
 * The brand hero at the top of a screen: the gradient runs under the status bar (edge to edge), with the brand mark and
 * a heading in white. [bottomOverlap] leaves room for a [SheetCard] that slides up over its lower edge.
 */
@Composable
fun BrandHeader(
    brandName: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    bottomOverlap: Dp = Spacing.xl,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(BrandGradient),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = Spacing.l, end = Spacing.l, top = Spacing.xl, bottom = Spacing.xl + bottomOverlap),
        ) {
            BrandMark(brandName)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Placeholder logo: the brand's first letter on a frosted circle, until the team's logo exists (Pre-launch). */
@Composable
fun BrandMark(
    brandName: String,
    modifier: Modifier = Modifier,
) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), modifier = modifier.size(72.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(brandName.take(1), style = MaterialTheme.typography.headlineLarge, color = Color.White)
        }
    }
}

/** The rounded sheet that holds a screen's content, sliding up over a [BrandHeader]. */
@Composable
fun SheetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xl),
            content = content,
        )
    }
}

/** A friendly error with a way out. Keep the message in our own words; never show raw server text. */
@Composable
fun ErrorState(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.padding(Spacing.l),
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(text = retryLabel, onClick = onRetry)
        }
    }
}

/** Grey placeholder blocks that gently pulse while content loads: shows the shape of what's coming. */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SKELETON_PULSE_MS), RepeatMode.Reverse),
        label = "skeleton alpha",
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m), modifier = modifier.fillMaxWidth().alpha(pulse)) {
        repeat(lines) { index ->
            Box(
                Modifier
                    .fillMaxWidth(if (index == lines - 1) 0.6f else 1f)
                    .height(if (index == 0) 28.dp else 18.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small),
            )
        }
    }
}

private const val SKELETON_PULSE_MS = 800

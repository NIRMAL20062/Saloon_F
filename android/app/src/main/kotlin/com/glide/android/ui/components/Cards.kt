package com.glide.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.glide.android.ui.theme.BrandGradient
import com.glide.android.ui.theme.Spacing
import com.glide.android.ui.theme.TouchTarget

/**
 * A rounded card. With [onClick] it is tappable: it shrinks slightly while pressed and is at least 56 dp tall.
 * Without it, it's a plain container.
 */
@Composable
fun GlideCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "card press")
    val shape = MaterialTheme.shapes.large
    val colors = MaterialTheme.colorScheme
    val inner: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier.padding(Spacing.m),
            content = content,
        )
    }
    val cardModifier =
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = colors.surfaceContainerLow,
            tonalElevation = 1.dp,
            interactionSource = interaction,
            modifier = cardModifier.heightIn(min = TouchTarget),
            content = inner,
        )
    } else {
        Surface(
            shape = shape,
            color = colors.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = cardModifier,
            content = inner,
        )
    }
}

/**
 * A salon in a list (Home, Search): photo area, name, area, rating and distance, and the salon type.
 * Until photos arrive (Phase 4) the photo area shows the brand gradient with the salon's first letter.
 * [ratingLabel] is what TalkBack reads for the rating (e.g. "Rated 4.6 from 128 reviews").
 */
@Composable
fun SalonCard(
    name: String,
    area: String,
    typeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null,
    ratingLabel: String? = null,
    distance: String? = null,
) {
    GlideCard(onClick = onClick, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(BrandGradient, MaterialTheme.shapes.medium),
        ) {
            Text(name.take(1), style = MaterialTheme.typography.displaySmall, color = Color.White)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (rating != null) RatingBadge(rating, ratingLabel)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Icon(
                GlideIcons.Location,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                listOfNotNull(area, distance).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusChip(text = typeLabel)
        }
    }
}

@Composable
private fun RatingBadge(
    rating: String,
    label: String?,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier =
            if (label !=
                null
            ) {
                Modifier.semantics(mergeDescendants = true) { contentDescription = label }
            } else {
                Modifier
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = 2.dp),
        ) {
            Icon(GlideIcons.Star, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(rating, style = MaterialTheme.typography.labelLarge)
        }
    }
}

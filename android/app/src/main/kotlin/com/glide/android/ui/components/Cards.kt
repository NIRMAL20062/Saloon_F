package com.glide.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.glide.android.ui.theme.BrandColors
import com.glide.android.ui.theme.BrandGradient
import com.glide.android.ui.theme.Spacing
import com.glide.android.ui.theme.TouchTarget

/**
 * A white card with a hairline border, as in the mockup. With [onClick] it is tappable: it shrinks slightly while
 * pressed and is at least 56 dp tall. Without it, it's a plain container.
 */
@Composable
fun GlideCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "card press")
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val color = if (highlighted) colors.secondaryContainer else colors.surface
    val border = BorderStroke(1.dp, if (highlighted) colors.primaryContainer else colors.outlineVariant)
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
            color = color,
            border = border,
            shadowElevation = 1.dp,
            interactionSource = interaction,
            modifier = cardModifier.heightIn(min = TouchTarget),
            content = inner,
        )
    } else {
        Surface(
            shape = shape,
            color = color,
            border = border,
            shadowElevation = 1.dp,
            modifier = cardModifier,
            content = inner,
        )
    }
}

/**
 * A salon in a list (Home, Search), as in mockup 8: photo on the left; name; rating with review count and the distance;
 * what it offers ([tags], e.g. "Unisex • Hair • Beauty"); the starting price ([priceFrom], e.g. "₹500 onwards").
 * Until photos arrive (Phase 4) the photo spot shows the brand red with the salon's first letter.
 * [ratingLabel] is what TalkBack reads for the rating (e.g. "Rated 4.8 from 320 reviews").
 */
@Composable
fun SalonCard(
    name: String,
    tags: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null,
    reviewCount: String? = null,
    ratingLabel: String? = null,
    distance: String? = null,
    priceFrom: String? = null,
) {
    GlideCard(onClick = onClick, modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp).background(BrandGradient, MaterialTheme.shapes.medium),
            ) {
                Text(name.take(1), style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .then(
                                        if (ratingLabel != null) {
                                            Modifier.semantics(mergeDescendants = true) {
                                                contentDescription =
                                                    ratingLabel
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                        ) {
                            Icon(
                                GlideIcons.Star,
                                contentDescription = null,
                                tint = BrandColors.Star,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(rating, style = MaterialTheme.typography.labelLarge)
                            reviewCount?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        Box(Modifier.weight(1f))
                    }
                    distance?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    tags,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                priceFrom?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * A big choice, as in mockup 4 ("I want to book salons" / "I run a salon"): an icon in a soft circle, a title, a line
 * under it and a chevron. [highlighted] gives it the light-pink look of the mockup's first card.
 */
@Composable
fun ChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    GlideCard(onClick = onClick, enabled = enabled, highlighted = highlighted, modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Spacing.s),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(
                            color = if (highlighted) colors.surface else colors.secondaryContainer,
                            shape = CircleShape,
                        ),
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(30.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Icon(GlideIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

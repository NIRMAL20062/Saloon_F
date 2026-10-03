package com.glide.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.glide.android.ui.theme.Spacing

/** A filter the person turns on and off (Search: "Open now", "Unisex"…). A check mark slides in when selected. */
@Composable
fun GlideFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    FilterChip(
        selected = selected,
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = {
            AnimatedVisibility(
                selected,
                enter = fadeIn() + expandHorizontally(),
                exit =
                    fadeOut() + shrinkHorizontally(),
            ) {
                Icon(GlideIcons.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.heightIn(min = 48.dp),
    )
}

/** How a [StatusChip] is coloured. */
enum class ChipTone { NEUTRAL, POSITIVE, WARNING, NEGATIVE }

/** A small read-only label: appointment status ("Confirmed"), salon type ("Unisex"), "Under verification"… */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.NEUTRAL,
) {
    val colors = MaterialTheme.colorScheme
    val (background, content) =
        when (tone) {
            ChipTone.NEUTRAL -> colors.surfaceContainerHighest to colors.onSurfaceVariant
            ChipTone.POSITIVE -> colors.primaryContainer to colors.onPrimaryContainer
            ChipTone.WARNING -> colors.secondaryContainer to colors.onSecondaryContainer
            ChipTone.NEGATIVE -> colors.errorContainer to colors.onErrorContainer
        }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = background, contentColor = content, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier =
                Modifier.padding(
                    horizontal =
                        Spacing.s + 2.dp,
                    vertical = 4.dp,
                ),
        )
    }
}

package com.glide.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glide.android.ui.theme.Spacing
import com.glide.android.ui.theme.TouchTarget

/**
 * Indian mobile number as in the mockup: a flag + country-code box ([countryCode], e.g. "+91") next to a digits-only field. Tag the
 * field through [fieldModifier].
 */
@Composable
fun PhoneNumberField(
    countryCode: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    /** Focus the field (and open the keyboard) as soon as it appears. */
    autoFocus: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        // Intrinsic height so the +91 box is exactly as tall as the number field.
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxHeight(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.fillMaxHeight().padding(horizontal = Spacing.m),
            ) {
                IndiaFlag()
                Text(countryCode, style = MaterialTheme.typography.titleMedium)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(label) },
            singleLine = true,
            isError = isError,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.titleMedium,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier =
                fieldModifier.weight(1f).focusRequester(focus).semantics {
                    contentType =
                        ContentType.PhoneNumberNational
                },
        )
    }
}

/**
 * One-time code shown as [length] separate boxes. The current box is highlighted with a blinking caret, a typed digit
 * pops in, and on error the row shakes once with a "reject" haptic. Android's keyboard can offer the SMS code
 * (autofill content type). Tag the field through [modifier]; [label] is what TalkBack reads.
 */
@Composable
fun OtpCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    label: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    enabled: Boolean = true,
    /** Focus the boxes (and open the number keyboard) as soon as they appear. */
    autoFocus: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    val shake = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(isError) {
        if (isError) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            shake.animateTo(
                targetValue = 0f,
                animationSpec =
                    keyframes {
                        durationMillis = SHAKE_MS
                        SHAKE_PX at 50
                        -SHAKE_PX at 120
                        SHAKE_PX * 0.6f at 190
                        -SHAKE_PX * 0.6f at 260
                        0f at SHAKE_MS
                    },
            )
        }
    }
    BasicTextField(
        // The cursor is pinned to the end: a tap on any box (or the test's typing) can't insert a digit in the middle.
        value = TextFieldValue(text = code, selection = TextRange(code.length)),
        onValueChange = { onCodeChange(it.text) },
        enabled = enabled,
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier =
            modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .offset(x = shake.value.dp)
                .semantics {
                    contentDescription = label
                    contentType = ContentType.SmsOtpCode
                },
        decorationBox = { innerTextField ->
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.fillMaxWidth()) {
                    repeat(length) { index ->
                        OtpBox(
                            digit = code.getOrNull(index),
                            current = focused && enabled && index == code.length.coerceAtMost(length - 1),
                            isError = isError,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // The real text field stays invisible on top, so taps focus it and the keyboard types into it.
                Box(Modifier.matchParentSize().alpha(0f)) { innerTextField() }
            }
        },
    )
}

@Composable
private fun OtpBox(
    digit: Char?,
    current: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        when {
            isError -> colors.error
            current -> colors.primary
            digit != null -> colors.outline
            else -> colors.outlineVariant
        },
        label = "otp border",
    )
    val borderWidth by animateDpAsState(if (current || isError) 2.dp else 1.dp, label = "otp border width")
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .heightIn(min = TouchTarget)
                .background(colors.surfaceContainerLowest, MaterialTheme.shapes.medium)
                .border(borderWidth, borderColor, MaterialTheme.shapes.medium),
    ) {
        AnimatedVisibility(visible = digit != null, enter = scaleIn(initialScale = 0.6f) + fadeIn(), exit = fadeOut()) {
            Text(
                text = digit?.toString().orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
        if (current && digit == null) Caret()
    }
}

@Composable
private fun Caret() {
    val blink by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(CARET_BLINK_MS), RepeatMode.Reverse),
        label = "caret alpha",
    )
    Box(
        Modifier
            .width(2.dp)
            .height(24.dp)
            .alpha(blink)
            .background(MaterialTheme.colorScheme.primary),
    )
}

/**
 * A text field with its label above it, as in the mockup's profile form ("Full name", "Email (optional)"). [error] is
 * shown under it in red. Tag the field through [fieldModifier].
 */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    autoFocus: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = error != null,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            modifier = fieldModifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label },
        )
        FieldMessage(error)
    }
}

/** A message under a field that slides in and out instead of popping. */
@Composable
fun FieldMessage(
    text: String?,
    modifier: Modifier = Modifier,
    isError: Boolean = true,
) {
    // Keep showing the last message while it animates out.
    val shown = remember { mutableStateOf(text) }
    if (text != null) shown.value = text
    AnimatedVisibility(
        visible = text != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Text(
            text = shown.value.orEmpty(),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private const val SHAKE_MS = 330
private const val SHAKE_PX = 10f
private const val CARET_BLINK_MS = 530

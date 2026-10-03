package com.glide.android.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.glide.android.R
import com.glide.android.ui.components.ChipTone
import com.glide.android.ui.components.EmptyState
import com.glide.android.ui.components.ErrorState
import com.glide.android.ui.components.FieldMessage
import com.glide.android.ui.components.GlideBottomSheet
import com.glide.android.ui.components.GlideCard
import com.glide.android.ui.components.GlideFilterChip
import com.glide.android.ui.components.GlideListItem
import com.glide.android.ui.components.GlideSnackbarHost
import com.glide.android.ui.components.GlideTopBar
import com.glide.android.ui.components.LoadingSkeleton
import com.glide.android.ui.components.OtpCodeField
import com.glide.android.ui.components.PhoneNumberField
import com.glide.android.ui.components.PrimaryButton
import com.glide.android.ui.components.QuietButton
import com.glide.android.ui.components.SalonCard
import com.glide.android.ui.components.SecondaryButton
import com.glide.android.ui.components.StatusChip
import com.glide.android.ui.theme.GlideTheme
import com.glide.android.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Debug builds only (opened from the signed-in screen): every design-system piece on one page, so the team can see the
 * look on a real phone and new screens can copy from it (APP-011, D-031, D-041). Light only (D-040).
 */
@Composable
fun ComponentsGalleryScreen(onBack: () -> Unit) {
    GlideTheme {
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        Scaffold(
            topBar = {
                GlideTopBar(
                    title = stringResource(R.string.gallery_title),
                    onBack = onBack,
                    backLabel = stringResource(R.string.common_back),
                )
            },
            snackbarHost = { GlideSnackbarHost(snackbar) },
        ) { padding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.m),
            ) {
                Buttons(onShowSnackbar = { message -> scope.launch { snackbar.showSnackbar(message) } })
                Fields()
                Chips()
                Cards()
                States()
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun Buttons(onShowSnackbar: (String) -> Unit) {
    var sheetOpen by remember { mutableStateOf(false) }
    val saved = stringResource(R.string.gallery_saved)
    Section(stringResource(R.string.gallery_buttons)) {
        PrimaryButton(text = stringResource(R.string.gallery_show_snackbar), onClick = { onShowSnackbar(saved) })
        PrimaryButton(text = stringResource(R.string.gallery_loading), onClick = {}, loading = true)
        PrimaryButton(text = stringResource(R.string.gallery_disabled), onClick = {}, enabled = false)
        SecondaryButton(text = stringResource(R.string.gallery_open_sheet), onClick = { sheetOpen = true })
        QuietButton(text = stringResource(R.string.gallery_quiet), onClick = {})
    }
    GlideBottomSheet(visible = sheetOpen, title = stringResource(R.string.gallery_sheet_title), onDismiss = {
        sheetOpen =
            false
    }) {
        GlideListItem(title = stringResource(R.string.gallery_sort_nearest), onClick = { sheetOpen = false })
        GlideListItem(title = stringResource(R.string.gallery_sort_rating), onClick = { sheetOpen = false })
    }
}

@Composable
private fun Fields() {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("12") }
    Section(stringResource(R.string.gallery_fields)) {
        PhoneNumberField(
            countryCode = stringResource(R.string.login_country_code),
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit).take(PHONE_DIGITS) },
            label = stringResource(R.string.login_phone_label),
            onDone = {},
        )
        OtpCodeField(
            code = code,
            onCodeChange = { code = it.filter(Char::isDigit).take(CODE_DIGITS) },
            label = stringResource(R.string.login_code_label),
            onDone = {},
        )
        OtpCodeField(code = "", onCodeChange = {
        }, label = stringResource(R.string.login_code_label), onDone = {}, isError = true)
        FieldMessage(stringResource(R.string.login_error_invalid_code))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips() {
    val filters = listOf(R.string.gallery_chip_open_now, R.string.gallery_chip_unisex, R.string.gallery_chip_top_rated)
    var selected by remember { mutableStateOf(setOf(filters.first())) }
    Section(stringResource(R.string.gallery_chips)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            filters.forEach { filter ->
                GlideFilterChip(
                    label = stringResource(filter),
                    selected = filter in selected,
                    onClick = { selected = if (filter in selected) selected - filter else selected + filter },
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            StatusChip(stringResource(R.string.gallery_status_confirmed), tone = ChipTone.POSITIVE)
            StatusChip(stringResource(R.string.gallery_status_waiting), tone = ChipTone.WARNING)
            StatusChip(stringResource(R.string.gallery_status_cancelled), tone = ChipTone.NEGATIVE)
            StatusChip(stringResource(R.string.gallery_status_completed))
        }
    }
}

@Composable
private fun Cards() {
    Section(stringResource(R.string.gallery_cards)) {
        SalonCard(
            name = stringResource(R.string.gallery_salon_name),
            tags = stringResource(R.string.gallery_salon_tags),
            rating = "4.8",
            reviewCount = stringResource(R.string.gallery_salon_reviews),
            ratingLabel = stringResource(R.string.gallery_salon_rating_label),
            distance = stringResource(R.string.gallery_salon_distance),
            priceFrom = stringResource(R.string.gallery_salon_price),
            onClick = {},
        )
        GlideCard { Text(stringResource(R.string.gallery_plain_card), style = MaterialTheme.typography.bodyLarge) }
        GlideListItem(
            title = stringResource(R.string.gallery_customer_name),
            supporting = stringResource(R.string.gallery_customer_detail),
            leadingText = stringResource(R.string.gallery_customer_initials),
            onClick = {},
        )
        GlideListItem(
            title = stringResource(R.string.gallery_appointment),
            supporting = stringResource(R.string.gallery_appointment_detail),
            trailing = { StatusChip(stringResource(R.string.gallery_status_confirmed), tone = ChipTone.POSITIVE) },
        )
    }
}

@Composable
private fun States() {
    Section(stringResource(R.string.gallery_states)) {
        LoadingSkeleton()
        EmptyState(
            title = stringResource(R.string.gallery_empty_title),
            message = stringResource(R.string.gallery_empty_message),
            actionLabel = stringResource(R.string.gallery_empty_action),
            onAction = {},
        )
        ErrorState(
            message =
                stringResource(
                    R.string.home_load_error,
                ),
            retryLabel = stringResource(R.string.status_retry),
            onRetry = {
            },
        )
    }
}

private const val PHONE_DIGITS = 10
private const val CODE_DIGITS = 6

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun GalleryPreview() {
    ComponentsGalleryScreen(onBack = {})
}

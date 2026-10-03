package com.glide.android.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glide.android.R
import com.glide.android.ui.components.ChoiceCard
import com.glide.android.ui.components.ErrorState
import com.glide.android.ui.components.FieldMessage
import com.glide.android.ui.components.GlideBottomSheet
import com.glide.android.ui.components.GlideHeader
import com.glide.android.ui.components.GlideIcons
import com.glide.android.ui.components.LabeledTextField
import com.glide.android.ui.components.LoadingSkeleton
import com.glide.android.ui.components.PrimaryButton
import com.glide.android.ui.components.QuietButton
import com.glide.android.ui.components.ScreenTitle
import com.glide.android.ui.home.SignedInScreen
import com.glide.android.ui.home.SignedInUiState
import com.glide.android.ui.theme.GlideTheme
import com.glide.android.ui.theme.Spacing
import com.glide.shared.me.UserSide

/** Everything a signed-in person sees until their side's real home exists: onboarding, then that home (APP-005). */
@Composable
fun OnboardingRoute(
    onOpenStatus: () -> Unit,
    onOpenComponents: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        onRetry = viewModel::load,
        onPick = viewModel::pick,
        onCancelPick = viewModel::cancelPick,
        onConfirmPick = viewModel::confirmPick,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onSaveProfile = viewModel::saveProfile,
        onLogout = viewModel::logout,
        onOpenStatus = onOpenStatus,
        onOpenComponents = onOpenComponents,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onRetry: () -> Unit,
    onPick: (UserSide) -> Unit,
    onCancelPick: () -> Unit,
    onConfirmPick: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit,
    onOpenStatus: () -> Unit,
    onOpenComponents: (() -> Unit)? = null,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut() },
        label = "onboarding step",
    ) { current ->
        when (current) {
            OnboardingUiState.Loading -> {
                Page { LoadingSkeleton() }
            }

            is OnboardingUiState.LoadError -> {
                Page {
                    ErrorState(
                        message = stringResource(R.string.home_load_error),
                        retryLabel = stringResource(R.string.status_retry),
                        onRetry = onRetry,
                    )
                    QuietButton(
                        text = stringResource(R.string.home_logout),
                        onClick = onLogout,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }

            is OnboardingUiState.ChooseSide -> {
                ChooseSide(current, onPick, onCancelPick, onConfirmPick, onLogout)
            }

            is OnboardingUiState.Profile -> {
                Profile(current, onNameChange, onEmailChange, onSaveProfile)
            }

            is OnboardingUiState.Home -> {
                SignedInScreen(
                    state = SignedInUiState.Ready(current.me),
                    onRetry = onRetry,
                    onLogout = onLogout,
                    onOpenStatus = onOpenStatus,
                    onOpenComponents = onOpenComponents,
                )
            }
        }
    }
}

/** White page with the wordmark on top, as every light screen of the mockup. */
@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .imePadding()
                .verticalScroll(rememberScrollState()),
    ) {
        GlideHeader(brandName = stringResource(R.string.app_name))
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.fillMaxWidth().padding(Spacing.l).navigationBarsPadding(),
            content = content,
        )
    }
}

/** Mockup 4: "How will you use Glide?". The choice is final (D-030), so a tap asks to confirm first. */
@Composable
private fun ChooseSide(
    state: OnboardingUiState.ChooseSide,
    onPick: (UserSide) -> Unit,
    onCancelPick: () -> Unit,
    onConfirmPick: () -> Unit,
    onLogout: () -> Unit,
) {
    Page {
        ScreenTitle(title = stringResource(R.string.onboard_title), modifier = Modifier.padding(bottom = Spacing.s))
        ChoiceCard(
            title = stringResource(R.string.onboard_customer),
            subtitle = stringResource(R.string.onboard_customer_hint),
            icon = GlideIcons.Search,
            onClick = { onPick(UserSide.CUSTOMER) },
            highlighted = true,
            enabled = !state.saving,
        )
        ChoiceCard(
            title = stringResource(R.string.onboard_salon),
            subtitle = stringResource(R.string.onboard_salon_hint),
            icon = GlideIcons.Scissors,
            onClick = { onPick(UserSide.SALON) },
            enabled = !state.saving,
        )
        FieldMessage(state.error?.let { stringResource(R.string.onboard_error_save) })
        QuietButton(
            text = stringResource(R.string.home_logout),
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
    GlideBottomSheet(
        visible = state.confirming != null,
        title =
            stringResource(
                if (state.confirming ==
                    UserSide.SALON
                ) {
                    R.string.onboard_confirm_salon
                } else {
                    R.string.onboard_confirm_customer
                },
            ),
        onDismiss = { if (!state.saving) onCancelPick() },
    ) {
        Text(
            stringResource(R.string.onboard_confirm_note),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = stringResource(R.string.onboard_confirm_yes),
            onClick = onConfirmPick,
            loading = state.saving,
        )
        QuietButton(
            text = stringResource(R.string.onboard_confirm_no),
            onClick = onCancelPick,
            enabled = !state.saving,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** Mockup 5: "Tell us a bit about yourself" (customers): full name, optional email. */
@Composable
private fun Profile(
    state: OnboardingUiState.Profile,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Page {
        ScreenTitle(title = stringResource(R.string.profile_title), modifier = Modifier.padding(bottom = Spacing.s))
        LabeledTextField(
            label = stringResource(R.string.profile_name),
            value = state.name,
            onValueChange = onNameChange,
            error = if (state.nameError) stringResource(R.string.profile_error_name) else null,
            enabled = !state.saving,
            fieldModifier = Modifier.testTag("name"),
            autoFocus = true,
        )
        LabeledTextField(
            label = stringResource(R.string.profile_email),
            value = state.email,
            onValueChange = onEmailChange,
            error = if (state.emailError) stringResource(R.string.profile_error_email) else null,
            enabled = !state.saving,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = onSave,
            fieldModifier = Modifier.testTag("email"),
        )
        FieldMessage(state.error?.let { stringResource(R.string.onboard_error_save) })
        PrimaryButton(
            text = stringResource(R.string.profile_continue),
            onClick = onSave,
            loading = state.saving,
            modifier = Modifier.padding(top = Spacing.s),
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ChooseSidePreview() {
    GlideTheme { OnboardingScreen(OnboardingUiState.ChooseSide(), {}, {}, {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ProfilePreview() {
    GlideTheme {
        OnboardingScreen(OnboardingUiState.Profile(name = "Priya Sharma", emailError = true), {
        }, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

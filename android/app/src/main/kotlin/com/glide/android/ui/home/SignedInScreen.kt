package com.glide.android.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.glide.android.R
import com.glide.android.data.network.ApiError
import com.glide.android.ui.auth.formatIndianPhone
import com.glide.android.ui.components.ErrorState
import com.glide.android.ui.components.GlideHeader
import com.glide.android.ui.components.LoadingSkeleton
import com.glide.android.ui.components.QuietButton
import com.glide.android.ui.components.SecondaryButton
import com.glide.android.ui.theme.GlideTheme
import com.glide.android.ui.theme.Spacing
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UserSide

sealed interface SignedInUiState {
    data object Loading : SignedInUiState

    /** The backend confirmed who this is. */
    data class Ready(
        val me: MeResponse,
    ) : SignedInUiState

    data class Error(
        val error: ApiError,
    ) : SignedInUiState
}

/** Temporary home of either side after onboarding (APP-005), until the customer tabs (APP-007) and salon home (APP-008). */
@Composable
fun SignedInScreen(
    state: SignedInUiState,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    onOpenStatus: () -> Unit,
    modifier: Modifier = Modifier,
    /** Debug builds only: opens the design components gallery (APP-011). */
    onOpenComponents: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
    ) {
        GlideHeader(brandName = stringResource(R.string.app_name))
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.padding(Spacing.l).navigationBarsPadding(),
        ) {
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = { (fadeIn() + slideInVertically { it / 8 }) togetherWith fadeOut() },
                label = "home state",
            ) { current ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    when (current) {
                        SignedInUiState.Loading -> {
                            LoadingSkeleton()
                        }

                        is SignedInUiState.Ready -> {
                            Account(current.me)
                        }

                        is SignedInUiState.Error -> {
                            ErrorState(
                                message = stringResource(R.string.home_load_error),
                                retryLabel = stringResource(R.string.status_retry),
                                onRetry = onRetry,
                            )
                        }
                    }
                }
            }
            SecondaryButton(text = stringResource(R.string.home_logout), onClick = onLogout)
            QuietButton(
                text = stringResource(R.string.home_system_status),
                onClick = onOpenStatus,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (onOpenComponents != null) {
                QuietButton(
                    text = stringResource(R.string.home_components),
                    onClick = onOpenComponents,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun Account(me: MeResponse) {
    Text(
        me.name?.let { stringResource(R.string.home_hi, it) } ?: stringResource(R.string.home_signed_in),
        style = MaterialTheme.typography.headlineSmall,
    )
    me.phone?.let {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                formatIndianPhone(it),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            )
        }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(if (me.side == UserSide.SALON) R.string.home_next_salon else R.string.home_next_customer),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.m),
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SignedInPreview() {
    GlideTheme { SignedInScreen(SignedInUiState.Ready(MeResponse("u-1", "919000000001")), {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SignedInErrorPreview() {
    GlideTheme { SignedInScreen(SignedInUiState.Error(ApiError.Network), {}, {}, {}) }
}

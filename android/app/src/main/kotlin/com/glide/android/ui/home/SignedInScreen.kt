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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glide.android.R
import com.glide.android.data.network.ApiError
import com.glide.android.ui.auth.formatIndianPhone
import com.glide.android.ui.components.BrandHeader
import com.glide.android.ui.components.ErrorState
import com.glide.android.ui.components.LoadingSkeleton
import com.glide.android.ui.components.QuietButton
import com.glide.android.ui.components.SecondaryButton
import com.glide.android.ui.components.SheetCard
import com.glide.android.ui.theme.GlideTheme
import com.glide.android.ui.theme.Spacing
import com.glide.shared.me.MeResponse

@Composable
fun SignedInRoute(
    onOpenStatus: () -> Unit,
    onOpenComponents: (() -> Unit)? = null,
    viewModel: SignedInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SignedInScreen(
        state = state,
        onRetry = viewModel::load,
        onLogout = viewModel::logout,
        onOpenStatus = onOpenStatus,
        onOpenComponents = onOpenComponents,
    )
}

/** Temporary home until onboarding (APP-005): who is signed in, log out, system status. Same look as the login. */
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
        BrandHeader(brandName = stringResource(R.string.app_name), title = stringResource(R.string.app_name))
        SheetCard(modifier = Modifier.offset(y = -Spacing.xl).navigationBarsPadding()) {
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
    Text(stringResource(R.string.home_signed_in), style = MaterialTheme.typography.headlineSmall)
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
            stringResource(R.string.home_next_step),
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
    GlideTheme(darkTheme = true) { SignedInScreen(SignedInUiState.Error(ApiError.Network), {}, {}, {}) }
}

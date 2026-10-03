package com.glide.android.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.glide.android.domain.status.SystemStatus
import com.glide.android.ui.components.BrandHeader
import com.glide.android.ui.components.PrimaryButton
import com.glide.android.ui.components.SecondaryButton
import com.glide.android.ui.components.SheetCard
import com.glide.android.ui.theme.GlideTheme
import com.glide.android.ui.theme.Spacing
import com.glide.shared.health.HealthStatus

/** Wires the screen to its ViewModel. Keep logic out of here. */
@Composable
fun StatusRoute(viewModel: StatusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatusScreen(state = state, onRetry = viewModel::refresh)
}

/** Stateless: renders [state] only, so every state can be previewed and UI-tested on its own. */
@Composable
fun StatusScreen(
    state: StatusUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
    ) {
        BrandHeader(brandName = stringResource(R.string.app_name), title = stringResource(R.string.home_system_status))
        SheetCard(modifier = Modifier.offset(y = -Spacing.xl).navigationBarsPadding()) {
            when (state) {
                StatusUiState.Loading -> Loading()
                is StatusUiState.Loaded -> Loaded(state.status, onRetry)
                is StatusUiState.Error -> Failed(state.error, onRetry)
            }
        }
    }
}

@Composable
private fun Loading() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        CircularProgressIndicator()
        Text(stringResource(R.string.status_checking), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Loaded(
    status: SystemStatus,
    onRefresh: () -> Unit,
) {
    StatusRow(stringResource(R.string.status_server), status.server)
    StatusRow(stringResource(R.string.status_database), status.database)
    if (status.database == HealthStatus.DOWN) {
        Text(stringResource(R.string.status_database_down_explanation), style = MaterialTheme.typography.bodyMedium)
    }
    Text(stringResource(R.string.status_version, status.version), style = MaterialTheme.typography.bodySmall)
    SecondaryButton(text = stringResource(R.string.status_refresh), onClick = onRefresh)
}

@Composable
private fun Failed(
    error: ApiError,
    onRetry: () -> Unit,
) {
    // Only our own wording is shown. Server text never reaches the screen; the code and request ID help support.
    val message =
        when (error) {
            ApiError.Network -> stringResource(R.string.status_error_network)
            is ApiError.Http -> stringResource(R.string.status_error_server, error.status)
            ApiError.Unexpected -> stringResource(R.string.status_error_unexpected)
        }
    Text(message, style = MaterialTheme.typography.bodyLarge)
    if (error is ApiError.Http && error.requestId != null) {
        Text(stringResource(R.string.status_reference, error.requestId), style = MaterialTheme.typography.bodySmall)
    }
    PrimaryButton(text = stringResource(R.string.status_retry), onClick = onRetry)
}

@Composable
private fun StatusRow(
    label: String,
    status: HealthStatus,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(
            text = status.name,
            style = MaterialTheme.typography.titleMedium,
            color =
                if (status ==
                    HealthStatus.UP
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun StatusLoadedPreview() {
    GlideTheme {
        StatusScreen(StatusUiState.Loaded(SystemStatus(HealthStatus.UP, HealthStatus.DOWN, "0.1.0")), onRetry = {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun StatusErrorPreview() {
    GlideTheme { StatusScreen(StatusUiState.Error(ApiError.Network), onRetry = {}) }
}

package com.glide.android.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glide.android.R
import com.glide.android.data.network.ApiError
import com.glide.android.domain.status.SystemStatus
import com.glide.android.ui.theme.GlideTheme
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
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
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
    CircularProgressIndicator()
    Text(stringResource(R.string.status_checking))
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
    OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.status_refresh)) }
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
    Button(onClick = onRetry) { Text(stringResource(R.string.status_retry)) }
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

@Preview(showBackground = true)
@Composable
private fun StatusLoadedPreview() {
    GlideTheme {
        StatusScreen(StatusUiState.Loaded(SystemStatus(HealthStatus.UP, HealthStatus.DOWN, "0.1.0")), onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusErrorPreview() {
    GlideTheme { StatusScreen(StatusUiState.Error(ApiError.Network), onRetry = {}) }
}

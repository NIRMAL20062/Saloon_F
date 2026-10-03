package com.glide.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.glide.android.ui.auth.formatIndianPhone
import com.glide.android.ui.theme.GlideTheme
import com.glide.shared.me.MeResponse

@Composable
fun SignedInRoute(
    onOpenStatus: () -> Unit,
    viewModel: SignedInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SignedInScreen(state = state, onRetry = viewModel::load, onLogout = viewModel::logout, onOpenStatus = onOpenStatus)
}

@Composable
fun SignedInScreen(
    state: SignedInUiState,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    onOpenStatus: () -> Unit,
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
                SignedInUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is SignedInUiState.Ready -> {
                    Text(stringResource(R.string.home_signed_in), style = MaterialTheme.typography.titleMedium)
                    state.me.phone?.let { Text(formatIndianPhone(it), style = MaterialTheme.typography.bodyLarge) }
                    Text(stringResource(R.string.home_next_step), style = MaterialTheme.typography.bodyMedium)
                }

                is SignedInUiState.Error -> {
                    Text(stringResource(R.string.home_load_error), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onRetry) { Text(stringResource(R.string.status_retry)) }
                }
            }
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.home_logout)) }
            TextButton(onClick = onOpenStatus) { Text(stringResource(R.string.home_system_status)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignedInPreview() {
    GlideTheme { SignedInScreen(SignedInUiState.Ready(MeResponse("u-1", "919000000001")), {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun SignedInErrorPreview() {
    GlideTheme { SignedInScreen(SignedInUiState.Error(ApiError.Network), {}, {}, {}) }
}

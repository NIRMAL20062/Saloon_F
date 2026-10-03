package com.glide.android.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glide.android.R
import com.glide.android.data.auth.AuthError
import com.glide.android.ui.theme.GlideTheme

@Composable
fun LoginRoute(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onPhoneChange = viewModel::onPhoneChange,
        onSendCode = viewModel::onSendCode,
        onCodeChange = viewModel::onCodeChange,
        onVerify = viewModel::onVerify,
        onResend = viewModel::onResend,
        onChangeNumber = viewModel::onChangeNumber,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onPhoneChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
            when (state) {
                is LoginUiState.EnterPhone -> PhoneStep(state, onPhoneChange, onSendCode)
                is LoginUiState.EnterCode -> CodeStep(state, onCodeChange, onVerify, onResend, onChangeNumber)
            }
        }
    }
}

@Composable
private fun PhoneStep(
    state: LoginUiState.EnterPhone,
    onPhoneChange: (String) -> Unit,
    onSendCode: () -> Unit,
) {
    Text(stringResource(R.string.login_phone_prompt), style = MaterialTheme.typography.bodyLarge)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("+91", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(R.string.login_phone_label)) },
            singleLine = true,
            isError = state.error != null,
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSendCode() }),
            modifier = Modifier.fillMaxWidth().testTag("phone"),
        )
    }
    state.error?.let { ErrorText(it) }
    PrimaryButton(stringResource(R.string.login_send_code), state.loading, onSendCode)
}

@Composable
private fun CodeStep(
    state: LoginUiState.EnterCode,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
) {
    Text(
        stringResource(R.string.login_code_sent, formatIndianPhone(toE164(state.phone))),
        style = MaterialTheme.typography.bodyLarge,
    )
    OutlinedTextField(
        value = state.code,
        onValueChange = onCodeChange,
        label = { Text(stringResource(R.string.login_code_label)) },
        singleLine = true,
        isError = state.error != null,
        enabled = !state.loading,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onVerify() }),
        modifier = Modifier.fillMaxWidth().testTag("code"),
    )
    state.error?.let { ErrorText(it) }
    PrimaryButton(stringResource(R.string.login_verify), state.loading, onVerify, enabled = state.code.length == 6)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(
            onClick = onChangeNumber,
            enabled = !state.loading,
        ) { Text(stringResource(R.string.login_change_number)) }
        if (state.resendInSeconds > 0) {
            TextButton(
                onClick = {},
                enabled = false,
            ) { Text(stringResource(R.string.login_resend_in, state.resendInSeconds)) }
        } else {
            TextButton(onClick = onResend, enabled = !state.loading) { Text(stringResource(R.string.login_resend)) }
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = Modifier.fillMaxWidth()) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text(text)
    }
}

@Composable
private fun ErrorText(error: AuthError) {
    val message =
        when (error) {
            AuthError.INVALID_PHONE -> R.string.login_error_invalid_phone
            AuthError.INVALID_CODE -> R.string.login_error_invalid_code
            AuthError.RATE_LIMITED -> R.string.login_error_rate_limited
            AuthError.NETWORK -> R.string.login_error_network
            AuthError.UNEXPECTED -> R.string.login_error_unexpected
        }
    Text(stringResource(message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

/** `919000000001` → `+91 90000 00001`. Anything else is shown unchanged. */
fun formatIndianPhone(e164Digits: String): String =
    if (e164Digits.length == 12 && e164Digits.startsWith("91")) {
        "+91 ${e164Digits.substring(2, 7)} ${e164Digits.substring(7)}"
    } else {
        e164Digits
    }

@Preview(showBackground = true)
@Composable
private fun PhoneStepPreview() {
    GlideTheme { LoginScreen(LoginUiState.EnterPhone("98765"), {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun CodeStepPreview() {
    GlideTheme {
        LoginScreen(LoginUiState.EnterCode("9876543210", error = AuthError.INVALID_CODE, resendInSeconds = 42), {
        }, {}, {}, {}, {}, {})
    }
}

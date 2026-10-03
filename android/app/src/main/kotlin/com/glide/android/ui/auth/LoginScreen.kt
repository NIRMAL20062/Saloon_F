package com.glide.android.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glide.android.R
import com.glide.android.data.auth.AuthError
import com.glide.android.ui.components.BrandHeader
import com.glide.android.ui.components.FieldMessage
import com.glide.android.ui.components.OtpCodeField
import com.glide.android.ui.components.PhoneNumberField
import com.glide.android.ui.components.PrimaryButton
import com.glide.android.ui.components.QuietButton
import com.glide.android.ui.components.SheetCard
import com.glide.android.ui.theme.GlideTheme
import com.glide.android.ui.theme.Spacing

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

/**
 * Brand header on top, a rounded sheet below with the current step. Moving between "phone" and "code" slides sideways
 * (forward to the code, back when changing the number). The page scrolls, so it also fits with the largest font.
 */
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
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .imePadding()
                .verticalScroll(rememberScrollState()),
    ) {
        BrandHeader(brandName = stringResource(R.string.app_name), title = stringResource(R.string.login_title))
        SheetCard(modifier = Modifier.offset(y = -Spacing.xl).navigationBarsPadding()) {
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = { stepTransition() },
                label = "login step",
            ) { step ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    when (step) {
                        is LoginUiState.EnterPhone -> PhoneStep(step, onPhoneChange, onSendCode)
                        is LoginUiState.EnterCode -> CodeStep(step, onCodeChange, onVerify, onResend, onChangeNumber)
                    }
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<LoginUiState>.stepTransition(): ContentTransform {
    val forward = targetState is LoginUiState.EnterCode
    val direction = if (forward) 1 else -1
    return (slideInHorizontally { width -> direction * width / 3 } + fadeIn())
        .togetherWith(slideOutHorizontally { width -> -direction * width / 3 } + fadeOut())
        .using(SizeTransform(clip = false))
}

@Composable
private fun PhoneStep(
    state: LoginUiState.EnterPhone,
    onPhoneChange: (String) -> Unit,
    onSendCode: () -> Unit,
) {
    Text(
        stringResource(R.string.login_phone_prompt),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PhoneNumberField(
        countryCode = stringResource(R.string.login_country_code),
        value = state.phone,
        onValueChange = onPhoneChange,
        label = stringResource(R.string.login_phone_label),
        onDone = onSendCode,
        fieldModifier = Modifier.testTag("phone"),
        isError = state.error != null,
        enabled = !state.loading,
    )
    FieldMessage(state.error?.let { stringResource(it.messageRes()) })
    PrimaryButton(text = stringResource(R.string.login_send_code), onClick = onSendCode, loading = state.loading)
}

@Composable
private fun CodeStep(
    state: LoginUiState.EnterCode,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
) {
    VerifyWhenComplete(code = state.code, onVerify = onVerify)
    Text(
        stringResource(R.string.login_code_sent, formatIndianPhone(toE164(state.phone))),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OtpCodeField(
        code = state.code,
        onCodeChange = onCodeChange,
        label = stringResource(R.string.login_code_label),
        onDone = onVerify,
        length = CODE_DIGITS,
        isError = state.error != null,
        enabled = !state.loading,
        modifier = Modifier.testTag("code"),
    )
    FieldMessage(state.error?.let { stringResource(it.messageRes()) })
    PrimaryButton(
        text = stringResource(R.string.login_verify),
        onClick = onVerify,
        enabled = state.code.length == CODE_DIGITS,
        loading = state.loading,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuietButton(
            text = stringResource(R.string.login_change_number),
            onClick = onChangeNumber,
            enabled = !state.loading,
        )
        if (state.resendInSeconds > 0) {
            QuietButton(
                text = stringResource(R.string.login_resend_in, state.resendInSeconds),
                onClick = {},
                enabled = false,
            )
        } else {
            QuietButton(text = stringResource(R.string.login_resend), onClick = onResend, enabled = !state.loading)
        }
    }
}

/** Typing (or autofilling) the last digit verifies straight away, like most apps. A code already complete on first show doesn't. */
@Composable
private fun VerifyWhenComplete(
    code: String,
    onVerify: () -> Unit,
) {
    val verify by rememberUpdatedState(onVerify)
    val previous = remember { mutableStateOf(code) }
    LaunchedEffect(code) {
        if (code.length == CODE_DIGITS && previous.value.length < CODE_DIGITS) verify()
        previous.value = code
    }
}

private fun AuthError.messageRes(): Int =
    when (this) {
        AuthError.INVALID_PHONE -> R.string.login_error_invalid_phone
        AuthError.INVALID_CODE -> R.string.login_error_invalid_code
        AuthError.RATE_LIMITED -> R.string.login_error_rate_limited
        AuthError.NETWORK -> R.string.login_error_network
        AuthError.UNEXPECTED -> R.string.login_error_unexpected
    }

/** `919000000001` → `+91 90000 00001`. Anything else is shown unchanged. */
fun formatIndianPhone(e164Digits: String): String =
    if (e164Digits.length == 12 && e164Digits.startsWith("91")) {
        "+91 ${e164Digits.substring(2, 7)} ${e164Digits.substring(7)}"
    } else {
        e164Digits
    }

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun PhoneStepPreview() {
    GlideTheme { LoginScreen(LoginUiState.EnterPhone("98765"), {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CodeStepPreview() {
    GlideTheme(darkTheme = true) {
        LoginScreen(
            LoginUiState.EnterCode(
                "9876543210",
                code = "1234",
                error = AuthError.INVALID_CODE,
                resendInSeconds = 42,
            ),
            {
            },
            {},
            {},
            {},
            {},
            {},
        )
    }
}

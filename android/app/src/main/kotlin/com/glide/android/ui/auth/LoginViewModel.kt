package com.glide.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glide.android.data.auth.AuthError
import com.glide.android.data.auth.AuthResult
import com.glide.android.data.auth.PhoneLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The two login steps. The phone is the 10 digits the person typed (no +91). */
sealed interface LoginUiState {
    data class EnterPhone(
        val phone: String = "",
        val error: AuthError? = null,
        val loading: Boolean = false,
    ) : LoginUiState

    data class EnterCode(
        val phone: String,
        val code: String = "",
        val error: AuthError? = null,
        val loading: Boolean = false,
        /** 0 = the person may ask for a new code. */
        val resendInSeconds: Int = RESEND_WAIT_SECONDS,
    ) : LoginUiState
}

const val RESEND_WAIT_SECONDS = 60
private const val PHONE_DIGITS = 10
private const val CODE_DIGITS = 6
private val INDIAN_MOBILE = Regex("^[6-9][0-9]{9}$")

/** 10-digit Indian mobile → E.164 digits without "+", as Supabase expects (e.g. 9000000001 → 919000000001). */
fun toE164(tenDigits: String) = "91$tenDigits"

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val login: PhoneLogin,
    ) : ViewModel() {
        private val _state = MutableStateFlow<LoginUiState>(LoginUiState.EnterPhone())
        val state: StateFlow<LoginUiState> = _state.asStateFlow()

        private var countdown: Job? = null

        fun onPhoneChange(input: String) {
            val phone = input.filter(Char::isDigit).take(PHONE_DIGITS)
            _state.update { (it as? LoginUiState.EnterPhone)?.copy(phone = phone, error = null) ?: it }
        }

        fun onSendCode() {
            val current = _state.value as? LoginUiState.EnterPhone ?: return
            if (current.loading) return
            if (!INDIAN_MOBILE.matches(current.phone)) {
                _state.value = current.copy(error = AuthError.INVALID_PHONE)
                return
            }
            _state.value = current.copy(loading = true, error = null)
            viewModelScope.launch {
                when (val result = login.requestOtp(toE164(current.phone))) {
                    is AuthResult.Success -> {
                        _state.value = LoginUiState.EnterCode(phone = current.phone)
                        startCountdown()
                    }

                    is AuthResult.Failure -> {
                        _state.value = current.copy(loading = false, error = result.error)
                    }
                }
            }
        }

        fun onCodeChange(input: String) {
            val code = input.filter(Char::isDigit).take(CODE_DIGITS)
            _state.update { (it as? LoginUiState.EnterCode)?.copy(code = code, error = null) ?: it }
        }

        fun onVerify() {
            val current = _state.value as? LoginUiState.EnterCode ?: return
            if (current.loading || current.code.length != CODE_DIGITS) return
            _state.value = current.copy(loading = true, error = null)
            viewModelScope.launch {
                when (val result = login.verifyOtp(toE164(current.phone), current.code)) {
                    // Navigation follows the session (AppViewModel), so nothing else to do here.
                    is AuthResult.Success -> {
                        countdown?.cancel()
                    }

                    is AuthResult.Failure -> {
                        _state.update {
                            (it as? LoginUiState.EnterCode)?.copy(loading = false, code = "", error = result.error)
                                ?: it
                        }
                    }
                }
            }
        }

        fun onResend() {
            val current = _state.value as? LoginUiState.EnterCode ?: return
            if (current.resendInSeconds > 0 || current.loading) return
            _state.value = current.copy(loading = true, error = null)
            viewModelScope.launch {
                when (val result = login.requestOtp(toE164(current.phone))) {
                    is AuthResult.Success -> {
                        _state.value = current.copy(loading = false, code = "", resendInSeconds = RESEND_WAIT_SECONDS)
                        startCountdown()
                    }

                    is AuthResult.Failure -> {
                        _state.value = current.copy(loading = false, error = result.error)
                    }
                }
            }
        }

        fun onChangeNumber() {
            val current = _state.value as? LoginUiState.EnterCode ?: return
            countdown?.cancel()
            _state.value = LoginUiState.EnterPhone(phone = current.phone)
        }

        private fun startCountdown() {
            countdown?.cancel()
            countdown =
                viewModelScope.launch {
                    for (left in RESEND_WAIT_SECONDS downTo 0) {
                        _state.update { (it as? LoginUiState.EnterCode)?.copy(resendInSeconds = left) ?: it }
                        if (left > 0) delay(ONE_SECOND_MS)
                    }
                }
        }

        private companion object {
            const val ONE_SECOND_MS = 1_000L
        }
    }

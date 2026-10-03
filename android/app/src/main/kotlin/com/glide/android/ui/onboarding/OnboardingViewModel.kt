package com.glide.android.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glide.android.data.auth.PhoneLogin
import com.glide.android.data.me.MeRepository
import com.glide.android.data.network.ApiError
import com.glide.android.data.network.ApiResult
import com.glide.shared.me.MeErrorCodes
import com.glide.shared.me.MeResponse
import com.glide.shared.me.ProfileRules
import com.glide.shared.me.UserSide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What a signed-in person sees, decided from `GET /v1/me` (D-024, D-030, APP-005). */
sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState

    data class LoadError(
        val error: ApiError,
    ) : OnboardingUiState

    /** "How will you use Glide?" (mockup 4). [confirming] = the side waiting for "Continue" in the sheet. */
    data class ChooseSide(
        val confirming: UserSide? = null,
        val saving: Boolean = false,
        val error: ApiError? = null,
    ) : OnboardingUiState

    /** "Tell us a bit about yourself" (mockup 5), for customers without a name yet. */
    data class Profile(
        val name: String = "",
        val email: String = "",
        val nameError: Boolean = false,
        val emailError: Boolean = false,
        val saving: Boolean = false,
        val error: ApiError? = null,
    ) : OnboardingUiState

    /** Onboarding is done: the side's home. */
    data class Home(
        val me: MeResponse,
    ) : OnboardingUiState
}

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val repository: MeRepository,
        private val login: PhoneLogin,
    ) : ViewModel() {
        private val _state = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
        val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

        init {
            load()
        }

        fun load() {
            _state.value = OnboardingUiState.Loading
            viewModelScope.launch {
                _state.value =
                    when (val result = repository.me()) {
                        is ApiResult.Success -> decide(result.data)
                        is ApiResult.Failure -> OnboardingUiState.LoadError(result.error)
                    }
            }
        }

        /** A card was tapped: ask "Continue as …? You can't change this later." first, because the choice is final. */
        fun pick(side: UserSide) {
            _state.update { (it as? OnboardingUiState.ChooseSide)?.copy(confirming = side, error = null) ?: it }
        }

        fun cancelPick() {
            _state.update { (it as? OnboardingUiState.ChooseSide)?.copy(confirming = null) ?: it }
        }

        fun confirmPick() {
            val current = _state.value as? OnboardingUiState.ChooseSide ?: return
            val side = current.confirming ?: return
            if (current.saving) return
            _state.value = current.copy(saving = true, error = null)
            viewModelScope.launch {
                when (val result = repository.chooseSide(side)) {
                    is ApiResult.Success -> {
                        _state.value = decide(result.data)
                    }

                    is ApiResult.Failure -> {
                        val alreadyChosen = (result.error as? ApiError.Http)?.code == MeErrorCodes.SIDE_ALREADY_CHOSEN
                        // Chosen on another phone already: follow what the backend has.
                        if (alreadyChosen) {
                            load()
                        } else {
                            _state.value = current.copy(saving = false, confirming = null, error = result.error)
                        }
                    }
                }
            }
        }

        fun onNameChange(name: String) {
            _state.update {
                (it as? OnboardingUiState.Profile)?.copy(
                    name = name.take(ProfileRules.NAME_MAX + NAME_SLACK),
                    nameError = false,
                    error = null,
                )
                    ?: it
            }
        }

        fun onEmailChange(email: String) {
            _state.update {
                (it as? OnboardingUiState.Profile)?.copy(
                    email = email.take(ProfileRules.EMAIL_MAX),
                    emailError = false,
                    error = null,
                )
                    ?: it
            }
        }

        fun saveProfile() {
            val current = _state.value as? OnboardingUiState.Profile ?: return
            if (current.saving) return
            val name = current.name.trim()
            val email = current.email.trim()
            val nameBad = name.length !in ProfileRules.NAME_MIN..ProfileRules.NAME_MAX
            val emailBad = email.isNotEmpty() && !EMAIL.matches(email)
            if (nameBad || emailBad) {
                _state.value = current.copy(nameError = nameBad, emailError = emailBad)
                return
            }
            _state.value = current.copy(saving = true, error = null)
            viewModelScope.launch {
                when (val result = repository.saveProfile(name, email)) {
                    is ApiResult.Success -> {
                        _state.value = decide(result.data)
                    }

                    is ApiResult.Failure -> {
                        val code = (result.error as? ApiError.Http)?.code
                        _state.value =
                            when (code) {
                                MeErrorCodes.INVALID_NAME -> current.copy(nameError = true)
                                MeErrorCodes.INVALID_EMAIL -> current.copy(emailError = true)
                                else -> current.copy(error = result.error)
                            }
                    }
                }
            }
        }

        /** Navigation follows the session, so the welcome screen appears by itself. */
        fun logout() {
            viewModelScope.launch { login.logout() }
        }

        private fun decide(me: MeResponse): OnboardingUiState =
            when {
                me.side == null -> OnboardingUiState.ChooseSide()
                me.side == UserSide.CUSTOMER && me.name == null -> OnboardingUiState.Profile()
                else -> OnboardingUiState.Home(me)
            }

        private companion object {
            /** Let people type a little past the limit so the error shows instead of keys silently doing nothing. */
            const val NAME_SLACK = 5
        }
    }

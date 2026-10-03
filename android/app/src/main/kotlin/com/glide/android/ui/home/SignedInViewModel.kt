package com.glide.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glide.android.data.auth.PhoneLogin
import com.glide.android.data.me.MeRepository
import com.glide.android.data.network.ApiError
import com.glide.android.data.network.ApiResult
import com.glide.shared.me.MeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SignedInUiState {
    data object Loading : SignedInUiState

    /** The backend confirmed who this is (proves the login token works end to end). */
    data class Ready(
        val me: MeResponse,
    ) : SignedInUiState

    data class Error(
        val error: ApiError,
    ) : SignedInUiState
}

/** Temporary home after login (APP-004). APP-005 replaces it with onboarding: customer or salon. */
@HiltViewModel
class SignedInViewModel
    @Inject
    constructor(
        private val me: MeRepository,
        private val login: PhoneLogin,
    ) : ViewModel() {
        private val _state = MutableStateFlow<SignedInUiState>(SignedInUiState.Loading)
        val state: StateFlow<SignedInUiState> = _state.asStateFlow()

        init {
            load()
        }

        fun load() {
            _state.value = SignedInUiState.Loading
            viewModelScope.launch {
                _state.value =
                    when (val result = me.me()) {
                        is ApiResult.Success -> SignedInUiState.Ready(result.data)
                        is ApiResult.Failure -> SignedInUiState.Error(result.error)
                    }
            }
        }

        /** Navigation follows the session, so the login screen appears by itself. */
        fun logout() {
            viewModelScope.launch { login.logout() }
        }
    }

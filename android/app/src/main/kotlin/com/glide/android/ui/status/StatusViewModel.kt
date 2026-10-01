package com.glide.android.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glide.android.data.network.ApiError
import com.glide.android.data.network.ApiResult
import com.glide.android.domain.status.GetSystemStatusUseCase
import com.glide.android.domain.status.SystemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StatusUiState {
    data object Loading : StatusUiState

    data class Loaded(
        val status: SystemStatus,
    ) : StatusUiState

    data class Error(
        val error: ApiError,
    ) : StatusUiState
}

@HiltViewModel
class StatusViewModel
    @Inject
    constructor(
        private val getSystemStatus: GetSystemStatusUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow<StatusUiState>(StatusUiState.Loading)
        val state: StateFlow<StatusUiState> = _state.asStateFlow()

        private var refreshJob: Job? = null

        init {
            refresh()
        }

        fun refresh() {
            // A second tap while a check is running restarts it instead of running two in parallel.
            refreshJob?.cancel()
            // Set synchronously so the tap shows Loading at once, never a stale result.
            _state.value = StatusUiState.Loading
            refreshJob =
                viewModelScope.launch {
                    _state.value =
                        when (val result = getSystemStatus()) {
                            is ApiResult.Success -> StatusUiState.Loaded(result.data)
                            is ApiResult.Failure -> StatusUiState.Error(result.error)
                        }
                }
        }
    }

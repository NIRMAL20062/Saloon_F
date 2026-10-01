package com.glide.android.ui.status

import com.glide.android.data.network.ApiError
import com.glide.android.data.network.ApiResult
import com.glide.android.domain.status.GetSystemStatusUseCase
import com.glide.android.domain.status.SystemStatus
import com.glide.android.testing.FakeHealthRepository
import com.glide.android.testing.MainDispatcherRule
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** ViewModel + real use case + fake repository: one test per UI state and transition. */
class StatusViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeHealthRepository()

    private fun viewModel() = StatusViewModel(GetSystemStatusUseCase(repository))

    @Test
    fun `starts in Loading and checks once`() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            assertEquals(StatusUiState.Loading, vm.state.value)
            advanceUntilIdle()
            assertEquals(1, repository.calls)
        }

    @Test
    fun `server and database up`() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(StatusUiState.Loaded(SystemStatus(HealthStatus.UP, HealthStatus.UP, "0.1.0")), vm.state.value)
        }

    @Test
    fun `server up but database down`() =
        runTest(mainDispatcher.dispatcher) {
            repository.next = ApiResult.Success(HealthResponse(HealthStatus.DOWN, "0.1.0", HealthStatus.DOWN))

            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(
                StatusUiState.Loaded(SystemStatus(HealthStatus.UP, HealthStatus.DOWN, "0.1.0")),
                vm.state.value,
            )
        }

    @Test
    fun `no connection shows the network error`() =
        runTest(mainDispatcher.dispatcher) {
            repository.next = ApiResult.Failure(ApiError.Network)

            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(StatusUiState.Error(ApiError.Network), vm.state.value)
        }

    @Test
    fun `server error keeps code and request id`() =
        runTest(mainDispatcher.dispatcher) {
            val error = ApiError.Http(500, "INTERNAL", "req-9")
            repository.next = ApiResult.Failure(error)

            val vm = viewModel()
            advanceUntilIdle()

            assertEquals(StatusUiState.Error(error), vm.state.value)
        }

    @Test
    fun `retry after an error recovers`() =
        runTest(mainDispatcher.dispatcher) {
            repository.next = ApiResult.Failure(ApiError.Network)
            val vm = viewModel()
            advanceUntilIdle()

            repository.next = ApiResult.Success(HealthResponse(HealthStatus.UP, "0.1.0", HealthStatus.UP))
            vm.refresh()
            assertEquals(StatusUiState.Loading, vm.state.value)
            advanceUntilIdle()

            assertEquals(StatusUiState.Loaded(SystemStatus(HealthStatus.UP, HealthStatus.UP, "0.1.0")), vm.state.value)
            assertEquals(2, repository.calls)
        }
}

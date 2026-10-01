package com.glide.android.ui.status

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.data.network.ApiError
import com.glide.android.domain.status.SystemStatus
import com.glide.android.ui.theme.GlideTheme
import com.glide.shared.health.HealthStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every screen state rendered on Robolectric (no emulator needed). */
@RunWith(AndroidJUnit4::class)
class StatusScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var retries = 0

    private fun show(state: StatusUiState) =
        compose.setContent { GlideTheme { StatusScreen(state = state, onRetry = { retries++ }) } }

    @Test
    fun loadingShowsProgressText() {
        show(StatusUiState.Loading)

        compose.onNodeWithText("Checking the server…").assertIsDisplayed()
    }

    @Test
    fun allUpShowsBothStatusesAndVersion() {
        show(StatusUiState.Loaded(SystemStatus(HealthStatus.UP, HealthStatus.UP, "0.1.0")))

        compose.onNodeWithText("Server").assertIsDisplayed()
        compose.onNodeWithText("Database").assertIsDisplayed()
        compose.onNodeWithText("Server version 0.1.0").assertIsDisplayed()
        compose.onNodeWithText("The server is running but can't reach its database.").assertDoesNotExist()
    }

    @Test
    fun databaseDownIsExplained() {
        show(StatusUiState.Loaded(SystemStatus(HealthStatus.UP, HealthStatus.DOWN, "0.1.0")))

        compose.onNodeWithText("DOWN").assertIsDisplayed()
        compose.onNodeWithText("The server is running but can't reach its database.").assertIsDisplayed()
    }

    @Test
    fun refreshFromLoadedStateCallsRetry() {
        show(StatusUiState.Loaded(SystemStatus(HealthStatus.UP, HealthStatus.UP, "0.1.0")))

        compose.onNodeWithText("Refresh").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun networkErrorOffersRetry() {
        show(StatusUiState.Error(ApiError.Network))

        compose.onNodeWithText("Can't reach the server. Check your connection and try again.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun serverErrorShowsOnlyOurTextWithStatusAndReference() {
        show(StatusUiState.Error(ApiError.Http(500, "INTERNAL", "req-9")))

        compose.onNodeWithText("The server had a problem (error 500).").assertIsDisplayed()
        compose.onNodeWithText("Reference: req-9").assertIsDisplayed()
        compose.onNodeWithText("INTERNAL", substring = true).assertDoesNotExist()
    }

    @Test
    fun unexpectedResponseAsksToUpdate() {
        show(StatusUiState.Error(ApiError.Unexpected))

        compose
            .onNodeWithText(
                "The server sent a response this app doesn't understand. Please update the app.",
            ).assertIsDisplayed()
    }
}

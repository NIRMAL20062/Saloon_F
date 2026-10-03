package com.glide.android.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.data.me.MeRepository
import com.glide.android.data.network.ApiError
import com.glide.android.data.network.ApiResult
import com.glide.android.data.network.GlideApi
import com.glide.android.testing.FakePhoneLogin
import com.glide.android.testing.MainDispatcherRule
import com.glide.android.ui.theme.GlideTheme
import com.glide.shared.health.HealthResponse
import com.glide.shared.me.MeResponse
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException

/** The temporary signed-in home: ViewModel states and screen states. */
@RunWith(AndroidJUnit4::class)
class SignedInTest {
    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val api = FakeGlideApi()
    private val login = FakePhoneLogin(signedIn = FakePhoneLogin.SESSION)

    @Test
    fun loadsWhoIsSignedInFromTheBackend() =
        runTest(mainDispatcher.dispatcher) {
            val vm = SignedInViewModel(MeRepository(api), login)

            runCurrent()

            assertEquals(SignedInUiState.Ready(MeResponse("user-1", "919000000001")), vm.state.value)
        }

    @Test
    fun backendUnreachableShowsErrorAndRetryRecovers() =
        runTest(mainDispatcher.dispatcher) {
            api.fail = true
            val vm = SignedInViewModel(MeRepository(api), login)
            runCurrent()
            assertEquals(SignedInUiState.Error(ApiError.Network), vm.state.value)

            api.fail = false
            vm.load()
            runCurrent()

            assertEquals(SignedInUiState.Ready(MeResponse("user-1", "919000000001")), vm.state.value)
        }

    @Test
    fun logoutEndsTheSession() =
        runTest(mainDispatcher.dispatcher) {
            val vm = SignedInViewModel(MeRepository(api), login)

            vm.logout()
            runCurrent()

            assertEquals(1, login.logouts)
            assertNull(login.session.value)
        }

    @Test
    fun readyScreenShowsTheFormattedPhoneAndLogout() {
        var loggedOut = false
        compose.setContent {
            GlideTheme {
                SignedInScreen(
                    SignedInUiState.Ready(MeResponse("user-1", "919000000001")),
                    {},
                    { loggedOut = true },
                    {},
                )
            }
        }

        compose.onNodeWithText("You're signed in").assertIsDisplayed()
        compose.onNodeWithText("+91 90000 00001").assertIsDisplayed()
        compose.onNodeWithText("Log out").performClick()

        assertEquals(true, loggedOut)
    }

    @Test
    fun errorScreenOffersRetry() {
        var retried = false
        compose.setContent {
            GlideTheme { SignedInScreen(SignedInUiState.Error(ApiError.Network), { retried = true }, {}, {}) }
        }

        compose.onNodeWithText("Couldn't load your account. Check your connection and try again.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        assertEquals(true, retried)
    }

    private class FakeGlideApi : GlideApi {
        var fail = false

        override suspend fun health(): Response<HealthResponse> = error("not used")

        override suspend fun me(): MeResponse =
            if (fail) throw IOException("offline") else MeResponse("user-1", "919000000001")
    }
}

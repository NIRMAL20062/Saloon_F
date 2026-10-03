package com.glide.android.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.data.network.ApiError
import com.glide.android.ui.theme.GlideTheme
import com.glide.shared.me.MeResponse
import com.glide.shared.me.UserSide
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The temporary home after onboarding: its screen states. Its logic is OnboardingViewModelTest. */
@RunWith(AndroidJUnit4::class)
class SignedInTest {
    @get:Rule
    val compose = createComposeRule()

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
        compose.onNodeWithText("Finding and booking salons comes in the next update.").assertIsDisplayed()
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

    @Test
    fun aNamedSalonPersonIsGreetedAndToldWhatComesNext() {
        compose.setContent {
            GlideTheme {
                SignedInScreen(
                    SignedInUiState.Ready(MeResponse("u-1", "919000000001", UserSide.SALON, "Rahul")),
                    {},
                    {},
                    {},
                )
            }
        }

        compose.onNodeWithText("Hi, Rahul").assertIsDisplayed()
        compose.onNodeWithText("Creating your salon comes in the next update.").assertIsDisplayed()
    }
}

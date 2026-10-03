package com.glide.android.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.data.network.ApiError
import com.glide.android.testing.FakeBackend
import com.glide.android.ui.theme.GlideTheme
import com.glide.shared.me.UserSide
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every onboarding screen state (mockups 4 and 5) on Robolectric. */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun show(state: OnboardingUiState) =
        compose.setContent {
            GlideTheme {
                OnboardingScreen(
                    state = state,
                    onRetry = { events += "retry" },
                    onPick = { events += "pick:$it" },
                    onCancelPick = { events += "cancel" },
                    onConfirmPick = { events += "confirm" },
                    onNameChange = { events += "name:$it" },
                    onEmailChange = { events += "email:$it" },
                    onSaveProfile = { events += "save" },
                    onLogout = { events += "logout" },
                    onOpenStatus = { events += "status" },
                )
            }
        }

    @Test
    fun theChoiceShowsBothCardsAsInTheMockup() {
        show(OnboardingUiState.ChooseSide())

        compose.onNodeWithText("How will you use Glide?").assertIsDisplayed()
        compose.onNodeWithText("Find and book salons near you").assertIsDisplayed()
        compose.onNodeWithText("Manage my salon, staff and appointments").assertIsDisplayed()
    }

    @Test
    fun tappingACardPicksThatSide() {
        show(OnboardingUiState.ChooseSide())

        compose.onNodeWithText("I want to book salons").performClick()
        compose.onNodeWithText("I run a salon").performClick()

        assertEquals(listOf("pick:CUSTOMER", "pick:SALON"), events)
    }

    @Test
    fun confirmingWarnsThatTheChoiceIsFinal() {
        show(OnboardingUiState.ChooseSide(confirming = UserSide.SALON))

        compose.onNodeWithText("Continue as a salon owner?").assertIsDisplayed()
        compose.onNodeWithText("You can't change this later.").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()

        assertEquals(listOf("confirm"), events)
    }

    @Test
    fun cancelInTheSheet() {
        show(OnboardingUiState.ChooseSide(confirming = UserSide.CUSTOMER))

        compose.onNodeWithText("Continue as a customer?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()

        assertEquals(listOf("cancel"), events)
    }

    @Test
    fun aFailedSaveSaysSo() {
        show(OnboardingUiState.ChooseSide(error = ApiError.Network))

        compose.onNodeWithText("Couldn't save. Check your connection and try again.").assertIsDisplayed()
    }

    @Test
    fun aWrongNumberCanStillLogOut() {
        show(OnboardingUiState.ChooseSide())

        compose.onNodeWithText("Log out").performClick()

        assertEquals(listOf("logout"), events)
    }

    @Test
    fun theProfileFormAsInTheMockup() {
        show(OnboardingUiState.Profile())
        compose.waitForIdle()

        compose.onNodeWithText("Tell us a bit about yourself").assertIsDisplayed()
        compose.onNodeWithText("Full name").assertIsDisplayed()
        compose.onNodeWithText("Email (optional)").assertIsDisplayed()
        compose.onNodeWithTag("name").assertIsFocused()
    }

    @Test
    fun typingAndContinue() {
        show(OnboardingUiState.Profile())

        compose.onNodeWithTag("name").performTextInput("Priya")
        compose.onNodeWithTag("email").performTextInput("p@x.in")
        compose.onNodeWithText("Continue").performClick()

        // This screen is stateless (the ViewModel stores the text), so only what it reports matters here.
        assertEquals(true, "name:Priya" in events && "email:p@x.in" in events)
        assertEquals("save", events.last())
    }

    @Test
    fun fieldErrorsShowUnderTheirField() {
        show(OnboardingUiState.Profile(nameError = true, emailError = true))

        compose.onNodeWithText("Enter a name between 2 and 60 characters.").assertIsDisplayed()
        compose.onNodeWithText("Enter a valid email address, or leave it empty.").assertIsDisplayed()
    }

    @Test
    fun loadingAndLoadError() {
        show(OnboardingUiState.LoadError(ApiError.Network))

        compose.onNodeWithText("Retry").performClick()

        assertEquals(listOf("retry"), events)
    }

    @Test
    fun afterOnboardingItIsTheHome() {
        show(OnboardingUiState.Home(FakeBackend.CUSTOMER))

        compose.onNodeWithText("Hi, Test Customer").assertIsDisplayed()
    }
}

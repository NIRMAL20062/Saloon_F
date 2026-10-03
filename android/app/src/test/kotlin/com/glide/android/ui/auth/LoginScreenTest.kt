package com.glide.android.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.data.auth.AuthError
import com.glide.android.ui.theme.GlideTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every login screen state on Robolectric. */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun show(state: LoginUiState) =
        compose.setContent {
            GlideTheme {
                LoginScreen(
                    state = state,
                    onPhoneChange = { events += "phone:$it" },
                    onSendCode = { events += "send" },
                    onCodeChange = { events += "code:$it" },
                    onVerify = { events += "verify" },
                    onResend = { events += "resend" },
                    onChangeNumber = { events += "change" },
                )
            }
        }

    @Test
    fun phoneStepShowsCountryCodeAndSendsTypedDigits() {
        show(LoginUiState.EnterPhone())

        compose.onNodeWithText("Enter your mobile number").assertIsDisplayed()
        compose.onNodeWithText("We'll send you a 6-digit code").assertIsDisplayed()
        compose.onNodeWithText("+91").assertIsDisplayed()
        compose.onNodeWithTag("phone").performTextInput("9")
        compose.onNodeWithText("Send Code").performClick()

        assertEquals(listOf("phone:9", "send"), events)
    }

    @Test
    fun invalidPhoneShowsOurMessage() {
        show(LoginUiState.EnterPhone("123", AuthError.INVALID_PHONE))

        compose.onNodeWithText("Enter a valid 10-digit mobile number.").assertIsDisplayed()
    }

    @Test
    fun codeStepShowsTheFormattedNumber() {
        show(LoginUiState.EnterCode("9000000001"))

        compose.onNodeWithText("Enter the 6-digit code").assertIsDisplayed()
        compose.onNodeWithText("We've sent a code to +91 90000 00001").assertIsDisplayed()
    }

    @Test
    fun whileCheckingTheCodeItSaysSoAndThereIsNoVerifyButton() {
        show(LoginUiState.EnterCode("9000000001", code = "123456", loading = true))

        compose.onNodeWithText("Verifying automatically…").assertIsDisplayed()
        compose.onNodeWithText("Verify").assertDoesNotExist()
    }

    @Test
    fun keyboardDoneVerifies() {
        show(LoginUiState.EnterCode("9000000001", code = "123456"))

        compose.onNodeWithTag("code").performImeAction()

        assertEquals(listOf("verify"), events)
    }

    @Test
    fun backOnTheCodeStepReturnsToThePhoneStep() {
        show(LoginUiState.EnterCode("9000000001"))

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("change"), events)
    }

    @Test
    fun resendShowsTheWaitThenBecomesAvailable() {
        show(LoginUiState.EnterCode("9000000001", resendInSeconds = 42))
        compose.onNodeWithText("Resend in 42 s").assertIsNotEnabled()
    }

    @Test
    fun resendWhenTheWaitIsOver() {
        show(LoginUiState.EnterCode("9000000001", resendInSeconds = 0))

        compose.onNodeWithText("Resend code").performClick()

        assertEquals(listOf("resend"), events)
    }

    @Test
    fun wrongCodeAndOtherErrorsShowOurWording() {
        show(LoginUiState.EnterCode("9000000001", error = AuthError.INVALID_CODE))
        compose.onNodeWithText("That code is wrong or has expired. Try again or resend it.").assertIsDisplayed()
    }

    @Test
    fun changeNumber() {
        show(LoginUiState.EnterCode("9000000001"))

        compose.onNodeWithText("Change number").performClick()

        assertEquals(listOf("change"), events)
    }

    @Test
    fun typingTheSixthDigitVerifiesAutomatically() {
        compose.setContent {
            var state by remember { mutableStateOf<LoginUiState>(LoginUiState.EnterCode("9000000001", code = "12345")) }
            GlideTheme {
                LoginScreen(
                    state = state,
                    onPhoneChange = {},
                    onSendCode = {},
                    onCodeChange = {
                        events += "code:$it"
                        state = (state as LoginUiState.EnterCode).copy(code = it)
                    },
                    onVerify = { events += "verify" },
                    onResend = {},
                    onChangeNumber = {},
                )
            }
        }

        compose.onNodeWithTag("code").performTextInput("6")
        compose.waitForIdle()

        assertEquals(listOf("code:123456", "verify"), events)
    }

    @Test
    fun aCodeAlreadyCompleteWhenShownIsNotSentByItself() {
        show(LoginUiState.EnterCode("9000000001", code = "123456"))
        compose.waitForIdle()

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun backOnThePhoneStepLeavesLogin() {
        var left = false
        compose.setContent {
            GlideTheme { LoginScreen(LoginUiState.EnterPhone(), {}, {}, {}, {}, {}, {}, onBack = { left = true }) }
        }

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(true, left)
    }

    @Test
    fun theWordmarkIsOnTop() {
        show(LoginUiState.EnterPhone())

        compose.onNodeWithContentDescription("Glide").assertIsDisplayed()
    }
}

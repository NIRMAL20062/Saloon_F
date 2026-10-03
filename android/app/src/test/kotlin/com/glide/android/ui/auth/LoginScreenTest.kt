package com.glide.android.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        compose.onNodeWithText("Welcome to Glide").assertIsDisplayed()
        compose.onNodeWithText("+91").assertIsDisplayed()
        compose.onNodeWithTag("phone").performTextInput("9")
        compose.onNodeWithText("Send code").performClick()

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

        compose.onNodeWithText("Enter the 6-digit code sent to +91 90000 00001").assertIsDisplayed()
    }

    @Test
    fun verifyIsEnabledOnlyWithSixDigits() {
        show(LoginUiState.EnterCode("9000000001", code = "12345"))
        compose.onNodeWithText("Verify").assertIsNotEnabled()
    }

    @Test
    fun verifyWithSixDigits() {
        show(LoginUiState.EnterCode("9000000001", code = "123456"))

        compose.onNodeWithText("Verify").assertIsEnabled().performClick()

        assertEquals(listOf("verify"), events)
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
}

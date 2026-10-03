package com.glide.android.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.ui.theme.DarkColors
import com.glide.android.ui.theme.GlideTheme
import com.glide.android.ui.theme.LightColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The design-system pieces (D-031) in each of their states, on Robolectric. */
@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun primaryButtonClicks() {
        var clicks = 0
        compose.setContent { GlideTheme { PrimaryButton(text = "Continue", onClick = { clicks++ }) } }

        compose.onNodeWithText("Continue").assertIsEnabled().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun primaryButtonWhileLoadingShowsNoLabelAndCantBeTapped() {
        compose.setContent { GlideTheme { PrimaryButton(text = "Continue", onClick = {}, loading = true) } }

        compose.onNodeWithText("Continue").assertDoesNotExist()
        compose.onNode(hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun disabledPrimaryButton() {
        compose.setContent { GlideTheme { PrimaryButton(text = "Verify", onClick = {}, enabled = false) } }

        compose.onNodeWithText("Verify").assertIsNotEnabled()
    }

    @Test
    fun phoneFieldShowsTheCountryCodeAndPassesTypedText() {
        val typed = mutableListOf<String>()
        compose.setContent {
            GlideTheme {
                PhoneNumberField(
                    countryCode = "+91",
                    value = "",
                    onValueChange = { typed += it },
                    label = "Mobile number",
                    onDone = {},
                    fieldModifier = Modifier.testTag("phone"),
                )
            }
        }

        compose.onNodeWithText("+91").assertIsDisplayed()
        compose.onNodeWithTag("phone").performTextInput("98")

        assertEquals(listOf("98"), typed)
    }

    @Test
    fun otpFieldShowsEachTypedDigitInItsOwnBox() {
        compose.setContent {
            GlideTheme {
                OtpCodeField(
                    code = "42",
                    onCodeChange = {},
                    label = "6-digit code",
                    onDone = {},
                )
            }
        }

        compose.onNodeWithText("4", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("2", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun otpFieldPassesTypedDigits() {
        val typed = mutableListOf<String>()
        compose.setContent {
            GlideTheme {
                OtpCodeField(
                    code = "",
                    onCodeChange = { typed += it },
                    label = "6-digit code",
                    onDone = {},
                    modifier = Modifier.testTag("code"),
                )
            }
        }

        compose.onNodeWithTag("code").performTextInput("123")

        assertEquals(listOf("123"), typed)
    }

    @Test
    fun otpFieldInErrorStillShowsAndAcceptsInput() {
        val typed = mutableListOf<String>()
        compose.setContent {
            GlideTheme {
                OtpCodeField(
                    code = "",
                    onCodeChange = { typed += it },
                    label = "6-digit code",
                    onDone = {},
                    isError = true,
                    modifier = Modifier.testTag("code"),
                )
            }
        }

        compose.onNodeWithTag("code").assertIsDisplayed().performTextInput("9")

        assertEquals(listOf("9"), typed)
    }

    @Test
    fun fieldMessageShowsOnlyWhenThereIsOne() {
        compose.setContent { GlideTheme { FieldMessage("Enter a valid number.") } }
        compose.onNodeWithText("Enter a valid number.").assertIsDisplayed()
    }

    @Test
    fun noFieldMessageWhenNull() {
        compose.setContent { GlideTheme { FieldMessage(null) } }
        compose.onNodeWithText("Enter a valid number.").assertDoesNotExist()
    }

    @Test
    fun errorStateOffersRetry() {
        var retried = false
        compose.setContent {
            GlideTheme {
                ErrorState(message = "Can't load.", retryLabel = "Retry", onRetry = {
                    retried =
                        true
                })
            }
        }

        compose.onNodeWithText("Can't load.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        assertEquals(true, retried)
    }

    @Test
    fun brandHeaderShowsMarkAndTitle() {
        compose.setContent { GlideTheme { BrandHeader(brandName = "Glide", title = "Welcome to Glide") } }

        compose.onNodeWithText("G").assertIsDisplayed()
        compose.onNodeWithText("Welcome to Glide").assertIsDisplayed()
    }

    @Test
    fun themeUsesOurBrandColoursInLightAndDark() {
        var light = Color.Unspecified
        var dark = Color.Unspecified
        compose.setContent {
            GlideTheme(darkTheme = false) { light = MaterialTheme.colorScheme.primary }
            GlideTheme(darkTheme = true) { dark = MaterialTheme.colorScheme.primary }
        }

        assertEquals(LightColors.primary, light)
        assertEquals(DarkColors.primary, dark)
    }
}

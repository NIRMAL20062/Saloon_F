package com.glide.android.ui.gallery

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.ui.home.SignedInScreen
import com.glide.android.ui.home.SignedInUiState
import com.glide.android.ui.theme.GlideTheme
import com.glide.shared.me.MeResponse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The debug-only design components gallery (APP-011) and how it's reached. */
@RunWith(AndroidJUnit4::class)
class ComponentsGalleryTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsEverySectionAndGoesBack() {
        var back = 0
        compose.setContent { ComponentsGalleryScreen(onBack = { back++ }) }

        listOf("Buttons", "Fields", "Chips", "Cards and lists", "Loading, empty and error").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, back)
    }

    @Test
    fun darkModeSwitch() {
        compose.setContent { ComponentsGalleryScreen(onBack = {}) }

        compose.onNodeWithTag("darkSwitch").assertIsOff().performClick()

        compose.onNodeWithTag("darkSwitch").assertIsOn()
        compose.onNodeWithText("Glow Studio").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun statusBarIconsFollowTheBackground() {
        compose.setContent { ComponentsGalleryScreen(onBack = {}) }
        val window = compose.activity.window
        val controller = { WindowCompat.getInsetsController(window, window.decorView) }

        assertEquals(true, controller().isAppearanceLightStatusBars)
        compose.onNodeWithTag("darkSwitch").performClick()
        compose.waitForIdle()

        assertEquals(false, controller().isAppearanceLightStatusBars)
    }

    @Test
    fun startsInDarkModeWhenAsked() {
        compose.setContent { ComponentsGalleryScreen(onBack = {}, startDark = true) }

        compose.onNodeWithTag("darkSwitch").assertIsOn()
    }

    @Test
    fun opensTheBottomSheetAndShowsAMessage() {
        compose.setContent { ComponentsGalleryScreen(onBack = {}) }

        compose.onNodeWithText("Open a bottom sheet").performScrollTo().performClick()
        compose.onNodeWithText("Sort by").assertIsDisplayed()
        compose.onNodeWithText("Nearest first").performClick()
        compose.onNodeWithText("Show a message").performScrollTo().performClick()

        compose.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun signedInScreenOffersTheGalleryOnlyWhenGivenTheLink() {
        var opened = 0
        compose.setContent {
            GlideTheme {
                SignedInScreen(
                    state = SignedInUiState.Ready(MeResponse("u-1", "919000000001")),
                    onRetry = {},
                    onLogout = {},
                    onOpenStatus = {},
                    onOpenComponents = { opened++ },
                )
            }
        }

        compose.onNodeWithText("Design components").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun noGalleryLinkWithoutIt() {
        compose.setContent {
            GlideTheme { SignedInScreen(SignedInUiState.Ready(MeResponse("u-1", "919000000001")), {}, {}, {}) }
        }

        compose.onNodeWithText("Design components").assertDoesNotExist()
    }
}

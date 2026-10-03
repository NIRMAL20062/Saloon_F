package com.glide.android.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.ui.theme.GlideTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The welcome screen from the team's mockup (APP-013, D-041). */
@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsTheMockupWordsAndGetsStarted() {
        var started = 0
        compose.setContent { GlideTheme { WelcomeScreen(onGetStarted = { started++ }) } }

        compose.onNodeWithContentDescription("Glide").assertIsDisplayed()
        compose.onNodeWithText("Look Good\nFeel Amazing").assertIsDisplayed()
        compose.onNodeWithText("Book trusted salons near you").assertIsDisplayed()
        compose.onNodeWithText("Get Started").performClick()

        assertEquals(1, started)
    }

    @Test
    fun statusBarIconsAreLightOnThePhotoAndDarkAgainAfter() {
        var showWelcome by mutableStateOf(true)
        compose.setContent { GlideTheme { if (showWelcome) WelcomeScreen(onGetStarted = {}) } }
        val window = compose.activity.window
        val icons = { WindowCompat.getInsetsController(window, window.decorView) }

        assertEquals(false, icons().isAppearanceLightStatusBars)
        showWelcome = false
        compose.waitForIdle()

        assertEquals(true, icons().isAppearanceLightStatusBars)
    }
}

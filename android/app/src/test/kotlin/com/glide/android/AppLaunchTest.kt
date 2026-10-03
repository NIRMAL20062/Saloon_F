package com.glide.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Fresh install, real Hilt graph: nobody is signed in, so the app opens on login. */
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aFreshInstallOpensOnWelcomeThenLogin() {
        composeRule.onNodeWithText("Book trusted salons near you").assertIsDisplayed()

        composeRule.onNodeWithText("Get Started").performClick()

        composeRule.onNodeWithText("Enter your mobile number").assertIsDisplayed()
        composeRule.onNodeWithText("Send Code").assertIsDisplayed()
    }

    @Test
    fun backFromLoginReturnsToWelcome() {
        composeRule.onNodeWithText("Get Started").performClick()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Get Started").assertIsDisplayed()
    }
}

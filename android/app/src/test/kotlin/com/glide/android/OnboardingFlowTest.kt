package com.glide.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.data.auth.AuthModule
import com.glide.android.data.auth.PhoneLogin
import com.glide.android.data.network.GlideApi
import com.glide.android.data.network.NetworkModule
import com.glide.android.testing.FakeBackend
import com.glide.android.testing.FakePhoneLogin
import com.glide.shared.me.UserSide
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A brand-new person, the whole way in the real app (fake login + fake backend): choice → profile → home (APP-005). */
@HiltAndroidTest
@UninstallModules(AuthModule::class, NetworkModule::class)
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val login: PhoneLogin = FakePhoneLogin(signedIn = FakePhoneLogin.SESSION)

    @BindValue
    @JvmField
    val backend: FakeBackend = FakeBackend()

    @BindValue
    @JvmField
    val api: GlideApi = backend

    @Test
    fun aNewCustomerChoosesGivesTheirNameAndLandsHome() {
        composeRule.onNodeWithText("How will you use Glide?").assertIsDisplayed()

        composeRule.onNodeWithText("I want to book salons").performClick()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.waitUntil(5_000) { backend.me.side == UserSide.CUSTOMER }
        composeRule.onNodeWithText("Tell us a bit about yourself").assertIsDisplayed()
        composeRule.onNodeWithTag("name").performTextInput("Priya Sharma")
        composeRule.onNodeWithText("Continue").performClick()

        composeRule.waitUntil(5_000) { backend.me.name != null }
        composeRule.onNodeWithText("Hi, Priya Sharma").assertIsDisplayed()
        assertEquals("Priya Sharma", backend.me.name)
    }
}

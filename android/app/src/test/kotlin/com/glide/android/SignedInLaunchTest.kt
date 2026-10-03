package com.glide.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.data.auth.AuthModule
import com.glide.android.data.auth.PhoneLogin
import com.glide.android.data.network.GlideApi
import com.glide.android.data.network.NetworkModule
import com.glide.android.testing.FakePhoneLogin
import com.glide.shared.health.HealthResponse
import com.glide.shared.health.HealthStatus
import com.glide.shared.me.MeResponse
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import retrofit2.Response

/** Signed in (fake login + fake backend, no network): home, system status, and logout back to login. */
@HiltAndroidTest
@UninstallModules(AuthModule::class, NetworkModule::class)
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class SignedInLaunchTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val login: PhoneLogin = FakePhoneLogin(signedIn = FakePhoneLogin.SESSION)

    @BindValue
    @JvmField
    val api: GlideApi = HealthyBackend()

    @Test
    fun aSignedInUserLandsOnHomeAndCanOpenSystemStatus() {
        composeRule.onNodeWithText("You're signed in").assertIsDisplayed()
        composeRule.onNodeWithText("+91 90000 00001").assertIsDisplayed()

        composeRule.onNodeWithText("System status").performClick()

        composeRule.onNodeWithText("Server version 0.1.0").assertIsDisplayed()
    }

    @Test
    fun logoutGoesBackToLogin() {
        composeRule.onNodeWithText("Log out").performClick()

        composeRule.onNodeWithText("Welcome to Glide").assertIsDisplayed()
    }
}

/** Backend that is up and knows the test user. */
class HealthyBackend : GlideApi {
    override suspend fun health(): Response<HealthResponse> =
        Response.success(HealthResponse(HealthStatus.UP, "0.1.0", HealthStatus.UP))

    override suspend fun me(): MeResponse = MeResponse("user-1", "919000000001")
}

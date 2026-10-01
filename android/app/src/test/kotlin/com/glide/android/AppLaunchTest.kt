package com.glide.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glide.android.data.DataModule
import com.glide.android.data.health.HealthRepository
import com.glide.android.testing.FakeHealthRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Launches the real MainActivity with the real Hilt graph, except the repository, which is a fake so the
 * test never depends on a running backend. Proves wiring end to end: Activity → navigation → screen →
 * ViewModel → use case → repository.
 */
@HiltAndroidTest
@UninstallModules(DataModule::class)
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val repository: HealthRepository = FakeHealthRepository()

    @Test
    fun appLaunchesOnTheStatusScreenAndShowsTheBackendState() {
        composeRule.onNodeWithText("Server version 0.1.0").assertIsDisplayed()
        composeRule.onNodeWithText("Refresh").assertIsDisplayed()
    }
}

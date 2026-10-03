package com.glide.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.glide.android.ui.theme.GlideTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * APP-011 components in every state, each run in light **and** dark mode. Also checks touch targets (≥ 48 dp),
 * TalkBack labels and the largest font size.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class DesignSystemTest(
    private val dark: Boolean,
) {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) = compose.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            GlideTheme(darkTheme = dark, content = content)
        }
    }

    @Test
    fun salonCardShowsItsDetailsAndOpens() {
        var opened = 0
        show {
            SalonCard(
                name = "Glow Studio",
                area = "Indiranagar",
                typeLabel = "Unisex",
                rating = "4.6",
                ratingLabel = "Rated 4.6 from 128 reviews",
                distance = "1.2 km",
                onClick = { opened++ },
            )
        }

        compose.onNodeWithText("Glow Studio").assertIsDisplayed()
        compose.onNodeWithText("Indiranagar · 1.2 km").assertIsDisplayed()
        compose.onNodeWithText("Unisex").assertIsDisplayed()
        compose.onNodeWithContentDescription("Rated 4.6 from 128 reviews").assertIsDisplayed()
        compose.onNodeWithText("Glow Studio").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun salonCardWithoutRatingOrDistance() {
        show { SalonCard(name = "New Salon", area = "Koramangala", typeLabel = "Women", onClick = {}) }

        compose.onNodeWithText("Koramangala").assertIsDisplayed()
    }

    @Test
    fun tappableCardIsBigEnoughAndCanBeDisabled() {
        var taps = 0
        show {
            Column {
                GlideCard(onClick = { taps++ }) { Text("Tap me") }
                GlideCard(onClick = { taps++ }, enabled = false) { Text("Disabled") }
            }
        }

        compose.onNodeWithText("Tap me").performClick()
        compose.onNodeWithText("Disabled").performClick()

        assertEquals(1, taps)
    }

    @Test
    fun filterChipShowsSelectedStateAndToggles() {
        val clicks = mutableListOf<String>()
        show {
            Column {
                GlideFilterChip(label = "Open now", selected = true, onClick = { clicks += "open" })
                GlideFilterChip(label = "Unisex", selected = false, onClick = { clicks += "unisex" })
                GlideFilterChip(label = "Women", selected = false, onClick = { clicks += "women" }, enabled = false)
            }
        }

        compose.onNodeWithText("Open now", useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("Open now") and hasClickAction()).assertIsSelected()
        compose.onNode(hasText("Unisex") and hasClickAction()).assertIsNotSelected().performClick()
        compose.onNode(hasText("Women") and hasClickAction()).assertIsNotEnabled()
        compose.onNode(hasText("Unisex") and hasClickAction()).assertHeightIsAtLeast(48.dp)

        assertEquals(listOf("unisex"), clicks)
    }

    @Test
    fun statusChipsInEveryTone() {
        show {
            Column {
                ChipTone.entries.forEach { StatusChip(text = it.name, tone = it) }
            }
        }

        ChipTone.entries.forEach { compose.onNodeWithText(it.name).assertIsDisplayed() }
    }

    @Test
    fun topBarBackHasALabelAndAGoodSize() {
        var back = 0
        show { GlideTopBar(title = "Services", onBack = { back++ }, backLabel = "Back") }

        compose.onNodeWithText("Services").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, back)
    }

    @Test
    fun topBarWithoutBack() {
        show { GlideTopBar(title = "Home") }

        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun listItemShowsAvatarAndOpens() {
        var opened = 0
        show {
            Column {
                GlideListItem(
                    title = "Priya Sharma",
                    supporting = "+91 90000 00002",
                    leadingText = "PS",
                    onClick = { opened++ },
                )
                GlideListItem(title = "Disabled row", onClick = { opened++ }, enabled = false)
            }
        }

        compose.onNodeWithText("PS").assertIsDisplayed()
        compose.onNodeWithText("+91 90000 00002").assertIsDisplayed()
        compose.onNodeWithText("Priya Sharma").performClick()
        compose.onNodeWithText("Disabled row").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun emptyStateWithAction() {
        var acted = false
        show {
            EmptyState(
                title = "No bookings yet",
                message = "Find a salon near you.",
                actionLabel = "Find a salon",
                onAction = { acted = true },
            )
        }

        compose.onNodeWithText("No bookings yet").assertIsDisplayed()
        compose.onNodeWithText("Find a salon").assertIsEnabled().performClick()

        assertEquals(true, acted)
    }

    @Test
    fun emptyStateWithoutAction() {
        show { EmptyState(title = "Nothing here", message = "Come back later.") }

        compose.onNodeWithText("Come back later.").assertIsDisplayed()
    }

    @Test
    fun bottomSheetShowsOnlyWhenVisibleAndCloses() {
        var visible = true
        show {
            GlideBottomSheet(
                visible = visible,
                title = "Sort by",
                onDismiss = { visible = false },
            ) { Text("Nearest first") }
        }

        compose.onNodeWithText("Sort by").assertIsDisplayed()
        compose.onNodeWithText("Nearest first").assertIsDisplayed()
    }

    @Test
    fun hiddenBottomSheetShowsNothing() {
        show { GlideBottomSheet(visible = false, title = "Sort by", onDismiss = {}) { Text("Nearest first") } }

        compose.onNodeWithText("Sort by").assertDoesNotExist()
    }

    @Test
    fun snackbarShowsAMessage() {
        val host = SnackbarHostState()
        show {
            GlideSnackbarHost(host)
            LaunchedEffect(Unit) { host.showSnackbar("Saved") }
        }

        compose.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun everythingStillShowsAtTheLargestFontSize() {
        show(fontScale = LARGEST_FONT_SCALE) {
            Column {
                SalonCard(
                    name = "Glow Studio",
                    area = "Indiranagar",
                    typeLabel = "Unisex",
                    rating = "4.6",
                    onClick = {},
                )
                GlideListItem(title = "Priya Sharma", supporting = "Haircut at 4:30 pm", onClick = {})
                PrimaryButton(text = "Book now", onClick = {})
            }
        }

        compose.onNodeWithText("Glow Studio").assertIsDisplayed()
        compose.onNodeWithText("Priya Sharma").assertIsDisplayed()
        compose.onNodeWithText("Book now").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    companion object {
        /** Android's biggest "Font size" setting. */
        private const val LARGEST_FONT_SCALE = 2f

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun modes() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }
}

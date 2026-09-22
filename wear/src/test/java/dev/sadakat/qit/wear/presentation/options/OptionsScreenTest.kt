package dev.sadakat.qit.wear.presentation.options

import android.app.Application
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.AppScaffold
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class OptionsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows the speed section and its choices`() {
        options(WearOptionsUiState())

        composeRule.onNodeWithText("Speed").assertExists()
        composeRule.onNodeWithText("0.75×").assertExists()
        composeRule.onNodeWithText("1×").assertExists()
        composeRule.onNodeWithText("1.25×").assertExists()
        composeRule.onNodeWithText("1.5×").assertExists()
    }

    @Test
    fun `shows the repeat section further down the list`() {
        options(WearOptionsUiState())

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(REPEAT_FOREVER_INDEX)
        composeRule.onNodeWithText("Repeat ayah").assertExists()
        composeRule.onNodeWithText("3 times").assertExists()
        composeRule.onNodeWithText("Forever").assertExists()
    }

    @Test
    fun `a running countdown shows in the sleep header`() {
        options(WearOptionsUiState(sleepRemainingMs = 12 * 60_000L + 5_000L))

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(SLEEP_HEADER_INDEX)
        composeRule.onNodeWithText("Sleep timer").assertExists()
        // Rounded up: 12 min and 5 s left reads as 13 min until the twelfth has fully passed.
        composeRule.onNodeWithText("13 min left").assertExists()
    }

    @Test
    fun `an end-of-surah timer explains when it stops`() {
        options(WearOptionsUiState(sleepAtEndOfSurah = true))

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(SLEEP_HEADER_INDEX)
        composeRule.onNodeWithText("Stops at the end of the surah").assertExists()
    }

    @Test
    fun `sleep options read as minutes`() {
        options(WearOptionsUiState())

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(SLEEP_HEADER_INDEX + 4)
        composeRule.onNodeWithText("Sleep timer").assertExists()
        composeRule.onNodeWithText("15 min").assertExists()
        composeRule.onNodeWithText("30 min").assertExists()
        composeRule.onNodeWithText("60 min").assertExists()
        composeRule.onNodeWithText("End of surah").assertExists()
    }

    @Test
    fun `picking a speed reports it`() {
        val speeds = mutableListOf<PlaybackSpeed>()
        composeRule.setContent {
            AppScaffold {
                OptionsScreen(
                    uiState = WearOptionsUiState(),
                    onSelectSpeed = { speeds += it },
                    onSelectRepeat = {},
                    onSelectSleep = {},
                )
            }
        }

        composeRule.onNodeWithText("1.5×").performClick()
        assertEquals(listOf(PlaybackSpeed.X1_5), speeds)
    }

    @Test
    fun `picking a repeat reports it`() {
        val repeats = mutableListOf<RepeatSelection>()
        composeRule.setContent {
            AppScaffold {
                OptionsScreen(
                    uiState = WearOptionsUiState(),
                    onSelectSpeed = {},
                    onSelectRepeat = { repeats += it },
                    onSelectSleep = {},
                )
            }
        }

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(REPEAT_FOREVER_INDEX)
        composeRule.onNodeWithText("Forever").performClick()
        assertEquals(listOf(RepeatSelection.FOREVER), repeats)
    }

    private fun options(state: WearOptionsUiState) {
        composeRule.setContent {
            AppScaffold {
                OptionsScreen(
                    uiState = state,
                    onSelectSpeed = {},
                    onSelectRepeat = {},
                    onSelectSleep = {},
                )
            }
        }
    }

    private companion object {
        // Item positions in the list: speed header + 4 speeds + repeat header + 3 repeats…
        const val REPEAT_FOREVER_INDEX = 8
        const val SLEEP_HEADER_INDEX = 9
    }
}

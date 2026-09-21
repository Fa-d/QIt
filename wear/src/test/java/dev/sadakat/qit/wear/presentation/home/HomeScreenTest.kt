package dev.sadakat.qit.wear.presentation.home

import android.app.Application
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.TestQuran
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val uiState = WearHomeViewModel.UiState(
        rows = listOf(
            WearHomeViewModel.SurahRow(TestQuran.surah(2), SurahDownloadState.Downloading(123, 286)),
            WearHomeViewModel.SurahRow(TestQuran.surah(112), SurahDownloadState.Downloaded),
            WearHomeViewModel.SurahRow(TestQuran.surah(114), SurahDownloadState.Failed(1, 6)),
        ),
        mode = RecitationMode.ARABIC_BANGLA,
        nowPlayingChip = WearHomeViewModel.NowPlayingChip("Al-Baqara", "2:255"),
    )

    @Test
    fun `renders the now playing chip, the mode chip and the surah rows`() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = uiState,
                    onSurahClick = {},
                    onNowPlayingClick = {},
                    onContinueClick = {},
                    onCycleMode = {},
                )
            }
        }

        composeRule.onNodeWithText("Now playing — Al-Baqara 2:255").assertExists()
        composeRule.onNodeWithText("Arabic + Bangla").assertExists()
        composeRule.onNodeWithText("2. Al-Baqara").assertExists()
        composeRule.onNodeWithText("286 ayahs · 43%").assertExists()
        composeRule.onNodeWithText("112. Al-Ikhlaas").assertExists()
        composeRule.onNodeWithText("4 ayahs · Downloaded").assertExists()
    }

    @Test
    fun `a surah without downloads shows only its ayah count`() {
        // Found on a Galaxy Watch: not-downloaded surahs were labelled "Offline", which reads as
        // "available offline" — the opposite of the truth.
        val empty = uiState.copy(
            nowPlayingChip = null,
            rows = listOf(WearHomeViewModel.SurahRow(TestQuran.surah(1), SurahDownloadState.NotDownloaded)),
        )
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = empty,
                    onSurahClick = {},
                    onNowPlayingClick = {},
                    onContinueClick = {},
                    onCycleMode = {},
                )
            }
        }

        composeRule.onNodeWithText("1. Al-Faatiha").assertExists()
        composeRule.onNodeWithText("7 ayahs").assertExists()
        composeRule.onNodeWithText("Offline", substring = true).assertDoesNotExist()
    }

    @Test
    fun `row clicks invoke the callbacks`() {
        var surahClicked = 0
        var nowPlayingClicked = false
        var modeCycles = 0
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = uiState,
                    onSurahClick = { surahClicked = it },
                    onNowPlayingClick = { nowPlayingClicked = true },
                    onContinueClick = {},
                    onCycleMode = { modeCycles++ },
                )
            }
        }

        composeRule.onNodeWithText("2. Al-Baqara").performClick()
        assertEquals(2, surahClicked)

        composeRule.onNodeWithText("Now playing — Al-Baqara 2:255").performClick()
        assertTrue(nowPlayingClicked)

        composeRule.onNodeWithText("Arabic + Bangla").performClick()
        assertEquals(1, modeCycles)
    }

    @Test
    fun `the continue chip appears when nothing is queued and invokes the callback`() {
        var continueClicked = false
        val continueState = uiState.copy(
            nowPlayingChip = null,
            continueChip = WearHomeViewModel.ContinueChip("Al-Ikhlaas", "112:3"),
        )
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = continueState,
                    onSurahClick = {},
                    onNowPlayingClick = {},
                    onContinueClick = { continueClicked = true },
                    onCycleMode = {},
                )
            }
        }

        composeRule.onNodeWithText("Continue 112:3").performClick()
        assertTrue(continueClicked)
    }

    @Test
    fun `failed downloads are labelled on the row`() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = uiState,
                    onSurahClick = {},
                    onNowPlayingClick = {},
                    onContinueClick = {},
                    onCycleMode = {},
                )
            }
        }

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        composeRule.onNodeWithText("6 ayahs · Failed").assertExists()
    }
}

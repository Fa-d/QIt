package dev.sadakat.qit.wear.presentation.surah

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.TestQuran
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class SurahScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val surah = TestQuran.surah(2)

    @Test
    fun `shows the surah title, ayah count and mode`() {
        composeRule.setContent {
            MaterialTheme {
                SurahScreen(
                    uiState = WearSurahViewModel.UiState(surah = surah, mode = RecitationMode.ARABIC_BANGLA),
                    onPlay = {},
                    onDownload = {},
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("2. Al-Baqara").assertExists()
        composeRule.onNodeWithText("286 ayahs").assertExists()
        composeRule.onNodeWithText("Mode: Arabic + Bangla").assertExists()
        composeRule.onNodeWithText("Play").assertExists()
        composeRule.onNodeWithText("Download").assertExists()
    }

    @Test
    fun `an unknown surah shows a not-found message instead of actions`() {
        composeRule.setContent {
            MaterialTheme {
                SurahScreen(
                    uiState = WearSurahViewModel.UiState(),
                    onPlay = {},
                    onDownload = {},
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("Surah not found").assertExists()
    }

    @Test
    fun `play and download clicks invoke the callbacks`() {
        var playClicked = false
        var downloadClicked = false
        composeRule.setContent {
            MaterialTheme {
                SurahScreen(
                    uiState = WearSurahViewModel.UiState(surah = surah),
                    onPlay = { playClicked = true },
                    onDownload = { downloadClicked = true },
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("Play").performClick()
        assertTrue(playClicked)

        composeRule.onNodeWithText("Download").performClick()
        assertTrue(downloadClicked)
    }

    @Test
    fun `a download in progress shows the percentage`() {
        composeRule.setContent {
            MaterialTheme {
                SurahScreen(
                    uiState = WearSurahViewModel.UiState(
                        surah = surah,
                        download = SurahDownloadState.Downloading(3, 286),
                    ),
                    onPlay = {},
                    onDownload = {},
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("Downloading 1%").assertExists()
    }

    @Test
    fun `a downloaded surah offers remove instead of download`() {
        var removeClicked = false
        var downloadClicked = false
        composeRule.setContent {
            MaterialTheme {
                SurahScreen(
                    uiState = WearSurahViewModel.UiState(surah = surah, download = SurahDownloadState.Downloaded),
                    onPlay = {},
                    onDownload = { downloadClicked = true },
                    onRemove = { removeClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Downloaded").assertExists()
        composeRule.onNodeWithText("Remove").performClick()
        assertTrue(removeClicked)
        assertTrue(!downloadClicked)
    }

    @Test
    fun `a failed download offers a retry`() {
        var downloadClicked = false
        composeRule.setContent {
            MaterialTheme {
                SurahScreen(
                    uiState = WearSurahViewModel.UiState(
                        surah = surah,
                        download = SurahDownloadState.Failed(1, 286),
                    ),
                    onPlay = {},
                    onDownload = { downloadClicked = true },
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("Retry download").performClick()
        assertTrue(downloadClicked)
    }
}

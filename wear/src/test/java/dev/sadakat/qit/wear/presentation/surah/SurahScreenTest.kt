package dev.sadakat.qit.wear.presentation.surah

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.AppScaffold
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
@Config(application = Application::class)
class SurahScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val surah = TestQuran.surah(2)

    @Test
    fun `shows the surah names, ayah count, download and recitation rows`() {
        composeRule.setContent { ScreenContent() }

        composeRule.onNodeWithText("البقرة").assertExists()
        composeRule.onNodeWithText("2 · Al-Baqara").assertExists()
        composeRule.onNodeWithText("286 ayahs").assertExists()
        composeRule.onNodeWithText("Download").assertExists()
        composeRule.onNodeWithText("Recitation · Arabic + Bangla").assertExists()
        composeRule.onNodeWithText("Play").assertExists()
    }

    @Test
    fun `an unknown surah shows a not-found message instead of actions`() {
        composeRule.setContent {
            AppScaffold {
                SurahScreen(uiState = WearSurahUiState(), onPlay = {}, onDownload = {}, onRemove = {}, onModeClick = {})
            }
        }

        composeRule.onNodeWithText("Surah not found").assertExists()
        composeRule.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test
    fun `play, download and mode clicks invoke the callbacks`() {
        var playClicked = false
        var downloadClicked = false
        var modeClicked = false
        composeRule.setContent {
            AppScaffold {
                SurahScreen(
                    uiState = state(),
                    onPlay = { playClicked = true },
                    onDownload = { downloadClicked = true },
                    onRemove = {},
                    onModeClick = { modeClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Play").performClick()
        assertTrue(playClicked)

        composeRule.onNodeWithText("Download").performClick()
        assertTrue(downloadClicked)

        composeRule.onNodeWithText("Recitation · Arabic + Bangla").performClick()
        assertTrue(modeClicked)
    }

    @Test
    fun `a download in progress shows the percentage and is disabled`() {
        composeRule.setContent { ScreenContent(SurahDownloadState.Downloading(3, 286)) }

        composeRule.onNodeWithText("Downloading 1%").assertExists()
    }

    @Test
    fun `a failed download offers a retry`() {
        var downloadClicked = false
        composeRule.setContent {
            AppScaffold {
                SurahScreen(
                    uiState = state(SurahDownloadState.Failed(1, 286)),
                    onPlay = {},
                    onDownload = { downloadClicked = true },
                    onRemove = {},
                    onModeClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Retry download").performClick()
        assertTrue(downloadClicked)
    }

    @Test
    fun `tapping Downloaded asks for confirmation before removing`() {
        var removeClicked = false
        var downloadClicked = false
        composeRule.setContent {
            AppScaffold {
                SurahScreen(
                    uiState = state(SurahDownloadState.Downloaded),
                    onPlay = {},
                    onDownload = { downloadClicked = true },
                    onRemove = { removeClicked = true },
                    onModeClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Downloaded").performClick()
        composeRule.onNodeWithText("Remove download?").assertExists()
        composeRule.onNodeWithText("Delete Al-Baqara's audio files from the watch.").assertExists()
        assertTrue(!removeClicked)
        assertTrue(!downloadClicked)

        composeRule.onNodeWithText("Remove").performClick()
        assertTrue(removeClicked)
        composeRule.onNodeWithText("Remove download?").assertDoesNotExist()
    }

    @Test
    fun `dismissing the remove confirmation removes nothing`() {
        var removeClicked = false
        composeRule.setContent {
            AppScaffold {
                SurahScreen(
                    uiState = state(SurahDownloadState.Downloaded),
                    onPlay = {},
                    onDownload = {},
                    onRemove = { removeClicked = true },
                    onModeClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Downloaded").performClick()
        composeRule.onNodeWithContentDescription("Dismiss").performClick()
        composeRule.onNodeWithText("Remove download?").assertDoesNotExist()
        assertTrue(!removeClicked)
    }

    private fun state(download: SurahDownloadState = SurahDownloadState.NotDownloaded) =
        WearSurahUiState(surah = surah, mode = RecitationMode.ARABIC_BANGLA, download = download)

    @Composable
    private fun ScreenContent(download: SurahDownloadState = SurahDownloadState.NotDownloaded) {
        AppScaffold {
            SurahScreen(
                uiState = state(download),
                onPlay = {},
                onDownload = {},
                onRemove = {},
                onModeClick = {},
            )
        }
    }
}

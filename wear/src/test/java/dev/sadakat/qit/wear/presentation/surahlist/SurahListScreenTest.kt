package dev.sadakat.qit.wear.presentation.surahlist

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.AppScaffold
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.TestQuran
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class SurahListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a downloading row shows the surah and its progress`() {
        composeRule.setContent { ListContent(single(SurahDownloadState.Downloading(123, 286))) }

        composeRule.onNodeWithText("2 · Al-Baqara").assertExists()
        composeRule.onNodeWithText("286 ayahs · 43%").assertExists()
    }

    @Test
    fun `a downloaded row says so`() {
        composeRule.setContent { ListContent(single(SurahDownloadState.Downloaded, number = 112)) }

        composeRule.onNodeWithText("112 · Al-Ikhlaas").assertExists()
        composeRule.onNodeWithText("4 ayahs · Downloaded").assertExists()
    }

    @Test
    fun `a failed row says so`() {
        composeRule.setContent { ListContent(single(SurahDownloadState.Failed(1, 6), number = 114)) }

        composeRule.onNodeWithText("114 · An-Naas").assertExists()
        composeRule.onNodeWithText("6 ayahs · Failed").assertExists()
    }

    @Test
    fun `a surah without downloads shows only its ayah count`() {
        // Found on a Galaxy Watch: not-downloaded surahs were labelled "Offline", which reads as
        // "available offline" — the opposite of the truth.
        composeRule.setContent { ListContent(single(SurahDownloadState.NotDownloaded, number = 1)) }

        composeRule.onNodeWithText("1 · Al-Faatiha").assertExists()
        composeRule.onNodeWithText("7 ayahs").assertExists()
        composeRule.onNodeWithText("Offline", substring = true).assertDoesNotExist()
    }

    @Test
    fun `row clicks report the surah number`() {
        val clicked = mutableListOf<Int>()
        composeRule.setContent {
            AppScaffold {
                SurahListScreen(uiState = single(SurahDownloadState.NotDownloaded), onSurahClick = { clicked += it })
            }
        }

        composeRule.onNodeWithText("2 · Al-Baqara").performClick()
        assertEquals(listOf(2), clicked)
    }

    @Test
    fun `the downloaded-only variant explains itself when empty`() {
        composeRule.setContent { ListContent(WearSurahListUiState(downloadedOnly = true)) }

        composeRule.onNodeWithText("Nothing downloaded yet. Open a surah to download its audio.").assertExists()
    }

    private fun single(download: SurahDownloadState, number: Int = 2) = WearSurahListUiState(
        rows = listOf(SurahRowUiModel(TestQuran.surah(number), download)),
        mode = RecitationMode.ARABIC_BANGLA,
    )

    @Composable
    private fun ListContent(uiState: WearSurahListUiState) {
        AppScaffold { SurahListScreen(uiState = uiState, onSurahClick = {}) }
    }
}

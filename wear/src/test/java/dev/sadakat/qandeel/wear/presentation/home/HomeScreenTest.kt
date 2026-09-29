package dev.sadakat.qandeel.wear.presentation.home

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.AppScaffold
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders the hub rows`() {
        composeRule.setContent { HomeContent(state()) }

        composeRule.onNodeWithText("Surahs").assertExists()
        composeRule.onNodeWithText("By juz").assertExists()
        composeRule.onNodeWithText("Downloaded (3)").assertExists()
        composeRule.onNodeWithText("Recitation · Arabic + Bangla").assertExists()
    }

    @Test
    fun `the downloaded row hides at zero`() {
        composeRule.setContent { HomeContent(state(downloadedCount = 0)) }

        composeRule.onNodeWithText("Surahs").assertExists()
        composeRule.onNodeWithText("Downloaded (0)", substring = true).assertDoesNotExist()
    }

    @Test
    fun `row clicks invoke the callbacks`() {
        val clicks = mutableSetOf<String>()
        composeRule.setContent {
            HomeContent(
                state(),
                onSurahsClick = { clicks += "surahs" },
                onJuzClick = { clicks += "juz" },
                onDownloadedClick = { clicks += "downloaded" },
                onModeClick = { clicks += "mode" },
            )
        }

        composeRule.onNodeWithText("Surahs").performClick()
        composeRule.onNodeWithText("By juz").performClick()
        composeRule.onNodeWithText("Downloaded (3)").performClick()
        composeRule.onNodeWithText("Recitation · Arabic + Bangla").performClick()
        assertEquals(setOf("surahs", "juz", "downloaded", "mode"), clicks)
    }

    @Test
    fun `an unloaded hub shows the loading row only`() {
        composeRule.setContent { HomeContent(state(loaded = false, downloadedCount = 0)) }

        composeRule.onNodeWithText("Loading…").assertExists()
        composeRule.onNodeWithText("Surahs").assertDoesNotExist()
    }

    @Test
    fun `the edge button offers Now playing while something is queued`() {
        var nowPlaying = false
        composeRule.setContent {
            HomeContent(state(isQueued = true, continuePosition = null), onNowPlayingClick = { nowPlaying = true })
        }

        composeRule.onNodeWithText("Now playing").performClick()
        assertTrue(nowPlaying)
        composeRule.onNodeWithText("Continue 18:23", substring = true).assertDoesNotExist()
    }

    @Test
    fun `the edge button offers Continue with the last position when nothing is queued`() {
        var continued = false
        composeRule.setContent {
            HomeContent(state(isQueued = false, continuePosition = "18:23"), onContinueClick = { continued = true })
        }

        composeRule.onNodeWithText("Continue 18:23").performClick()
        assertTrue(continued)
    }

    @Test
    fun `no queue and no saved position means no edge button`() {
        composeRule.setContent { HomeContent(state(isQueued = false, continuePosition = null)) }

        composeRule.onNodeWithText("Now playing").assertDoesNotExist()
        composeRule.onNodeWithText("Continue", substring = true).assertDoesNotExist()
    }

    private fun state(
        loaded: Boolean = true,
        downloadedCount: Int = 3,
        isQueued: Boolean = true,
        continuePosition: String? = null,
    ) = WearHomeUiState(
        loaded = loaded,
        mode = RecitationMode.ARABIC_BANGLA,
        downloadedCount = downloadedCount,
        isQueued = isQueued,
        continuePosition = continuePosition,
    )

    @Composable
    private fun HomeContent(
        state: WearHomeUiState,
        onSurahsClick: () -> Unit = {},
        onJuzClick: () -> Unit = {},
        onDownloadedClick: () -> Unit = {},
        onModeClick: () -> Unit = {},
        onNowPlayingClick: () -> Unit = {},
        onContinueClick: () -> Unit = {},
    ) {
        AppScaffold {
            HomeScreen(
                uiState = state,
                onSurahsClick = onSurahsClick,
                onJuzClick = onJuzClick,
                onDownloadedClick = onDownloadedClick,
                onModeClick = onModeClick,
                onNowPlayingClick = onNowPlayingClick,
                onContinueClick = onContinueClick,
            )
        }
    }
}

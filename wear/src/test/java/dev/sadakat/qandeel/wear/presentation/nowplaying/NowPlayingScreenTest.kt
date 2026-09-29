package dev.sadakat.qandeel.wear.presentation.nowplaying

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.AppScaffold
import dev.sadakat.qandeel.core.domain.player.PlaybackError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class NowPlayingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the controls page shows the surah and the position`() {
        composeRule.setContent { PlayerContent(playing()) }

        composeRule.onNodeWithText("Al-Baqara").assertExists()
        composeRule.onNodeWithText("2:255").assertExists()
    }

    @Test
    fun `the basmala position is labelled Bismillah`() {
        composeRule.setContent { PlayerContent(playing(ayah = 0)) }

        composeRule.onNodeWithText("Bismillah").assertExists()
        composeRule.onNodeWithText("2:0", substring = true).assertDoesNotExist()
    }

    @Test
    fun `nothing queued shows a placeholder`() {
        composeRule.setContent { PlayerContent(WearNowPlayingUiState()) }

        composeRule.onNodeWithText("Nothing playing").assertExists()
    }

    @Test
    fun `the playback error is shown`() {
        composeRule.setContent { PlayerContent(playing(error = PlaybackError.NETWORK)) }

        composeRule.onNodeWithText("No connection").assertExists()
    }

    @Test
    fun `a failed recitation gets its own short line`() {
        composeRule.setContent { PlayerContent(playing(error = PlaybackError.FAILED)) }

        composeRule.onNodeWithText("Couldn't play").assertExists()
    }

    @Test
    fun `the controls invoke their callbacks`() {
        var previous = 0
        var toggles = 0
        var next = 0
        var options = 0
        composeRule.setContent {
            AppScaffold {
                NowPlayingScreen(
                    uiState = playing(),
                    onPrevious = { previous++ },
                    onTogglePlayPause = { toggles++ },
                    onNext = { next++ },
                    onVolumeSteps = {},
                    onOpenOptions = { options++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Previous ayah").performClick()
        assertEquals(1, previous)

        composeRule.onNodeWithContentDescription("Pause").performClick()
        assertEquals(1, toggles)

        composeRule.onNodeWithContentDescription("Next ayah").performClick()
        assertEquals(1, next)

        composeRule.onNodeWithContentDescription("Playback options").performClick()
        assertEquals(1, options)
    }

    @Test
    fun `a paused player shows play instead of pause`() {
        composeRule.setContent { PlayerContent(playing(isPlaying = false)) }

        composeRule.onNodeWithContentDescription("Play or pause").assertExists()
        composeRule.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }

    @Test
    fun `swiping to the text page shows the whole ayah and its translation`() {
        composeRule.setContent { PlayerContent(playing()) }

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ").assertExists()
        composeRule.onNodeWithText("Bangla 2:255").assertExists()
    }

    @Test
    fun `the text page shows the basmala itself at position zero`() {
        composeRule.setContent { PlayerContent(playing(ayah = 0)) }

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ").assertExists()
    }

    private fun playing(ayah: Int = 255, isPlaying: Boolean = true, error: PlaybackError? = null) =
        WearNowPlayingUiState(
            surahNumber = 2,
            surahName = "Al-Baqara",
            ayah = ayah,
            ayahText = if (ayah >= 1) "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ" else null,
            translation = if (ayah >= 1) "Bangla 2:255" else null,
            isPlaying = isPlaying,
            error = error,
            progress = 0.5f,
            volume = 0.5f,
        )

    @Composable
    private fun PlayerContent(state: WearNowPlayingUiState) {
        AppScaffold {
            NowPlayingScreen(
                uiState = state,
                onPrevious = {},
                onTogglePlayPause = {},
                onNext = {},
                onVolumeSteps = {},
                onOpenOptions = {},
            )
        }
    }
}

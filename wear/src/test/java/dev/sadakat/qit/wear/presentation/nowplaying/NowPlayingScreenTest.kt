package dev.sadakat.qit.wear.presentation.nowplaying

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class NowPlayingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows the surah, the position and the Arabic ayah text`() {
        composeRule.setContent {
            MaterialTheme {
                NowPlayingScreen(
                    uiState = NowPlayingViewModel.UiState(
                        surahNumber = 2,
                        surahName = "Al-Baqara",
                        ayah = 255,
                        ayahText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
                        isPlaying = true,
                    ),
                    onPrevious = {},
                    onTogglePlayPause = {},
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithText("Al-Baqara").assertExists()
        composeRule.onNodeWithText("2:255").assertExists()
        composeRule.onNodeWithText("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ").assertExists()
    }

    @Test
    fun `the basmala position is labelled Bismillah and shows the basmala text`() {
        composeRule.setContent {
            MaterialTheme {
                NowPlayingScreen(
                    uiState = NowPlayingViewModel.UiState(
                        surahNumber = 2,
                        surahName = "Al-Baqara",
                        ayah = 0,
                        ayahText = null,
                    ),
                    onPrevious = {},
                    onTogglePlayPause = {},
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithText("Bismillah").assertExists()
        composeRule.onNodeWithText("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ").assertExists()
    }

    @Test
    fun `nothing queued shows a placeholder`() {
        composeRule.setContent {
            MaterialTheme {
                NowPlayingScreen(
                    uiState = NowPlayingViewModel.UiState(),
                    onPrevious = {},
                    onTogglePlayPause = {},
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithText("Nothing playing").assertExists()
    }

    @Test
    fun `the playback error is shown`() {
        composeRule.setContent {
            MaterialTheme {
                NowPlayingScreen(
                    uiState = NowPlayingViewModel.UiState(
                        surahNumber = 2,
                        surahName = "Al-Baqara",
                        ayah = 255,
                        ayahText = "آية 2:255",
                        error = "No network",
                    ),
                    onPrevious = {},
                    onTogglePlayPause = {},
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithText("No network").assertExists()
    }

    @Test
    fun `the controls invoke their callbacks and reflect the play state`() {
        var previous = 0
        var toggles = 0
        var next = 0
        composeRule.setContent {
            MaterialTheme {
                NowPlayingScreen(
                    uiState = NowPlayingViewModel.UiState(
                        surahNumber = 2,
                        surahName = "Al-Baqara",
                        ayah = 255,
                        ayahText = "آية 2:255",
                        isPlaying = true,
                    ),
                    onPrevious = { previous++ },
                    onTogglePlayPause = { toggles++ },
                    onNext = { next++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Previous ayah").performClick()
        assertEquals(1, previous)

        composeRule.onNodeWithContentDescription("Pause").performClick()
        assertEquals(1, toggles)

        composeRule.onNodeWithContentDescription("Next ayah").performClick()
        assertEquals(1, next)
    }

    @Test
    fun `a paused player shows play instead of pause`() {
        composeRule.setContent {
            MaterialTheme {
                NowPlayingScreen(
                    uiState = NowPlayingViewModel.UiState(
                        surahNumber = 2,
                        surahName = "Al-Baqara",
                        ayah = 255,
                        ayahText = "آية 2:255",
                        isPlaying = false,
                    ),
                    onPrevious = {},
                    onTogglePlayPause = {},
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Play or pause").assertExists()
        composeRule.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }
}

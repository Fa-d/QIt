package dev.sadakat.qit.presentation.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MiniPlayerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var expands = 0
    private var toggles = 0
    private var nexts = 0

    private fun setContent(nowPlaying: NowPlaying, sleepTimer: SleepTimerStatus = SleepTimerStatus.Off) {
        composeRule.setContent {
            QItAppTheme {
                MiniPlayer(
                    state = PlayerUiState(nowPlaying = nowPlaying, surahName = "Al-Kahf", sleepTimer = sleepTimer),
                    onExpand = { expands++ },
                    onTogglePlayPause = { toggles++ },
                    onNext = { nexts++ },
                )
            }
        }
    }

    private fun playing(ayah: Int = 10, isPlaying: Boolean = true, isBuffering: Boolean = false) =
        NowPlaying(18, ayah, Track.ARABIC, RecitationMode.ARABIC_BANGLA, isPlaying, isBuffering)

    @Test
    fun `shows what plays and where in the surah`() {
        setContent(playing(), SleepTimerStatus.Counting(754_000L))

        composeRule.onNodeWithText("Al-Kahf 18:10").assertIsDisplayed()
        composeRule.onNodeWithText("10/110 · Arabic + Bangla · 12:34").assertIsDisplayed()
    }

    @Test
    fun `the basmala is titled as such`() {
        setContent(playing(ayah = 0))

        composeRule.onNodeWithText("Al-Kahf · Bismillah").assertIsDisplayed()
    }

    @Test
    fun `play-pause and next dispatch, and a tap or a swipe up opens the full player`() {
        setContent(playing())

        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Next ayah").performClick()
        composeRule.onNodeWithTag("mini_player").performClick()
        composeRule.onNodeWithTag("mini_player").performTouchInput { swipeUp() }

        assertEquals(1, toggles)
        assertEquals(1, nexts)
        assertEquals(2, expands)
    }

    @Test
    fun `offers play while paused and shows buffering`() {
        setContent(playing(isPlaying = false, isBuffering = true))

        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithTag("player_buffering").assertIsDisplayed()
    }
}

package dev.sadakat.qit.presentation.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var toggled = false
    private var nexted = false
    private var previoused = false
    private var stopped = false
    private var opened: Pair<Int, Int>? = null

    private fun setContent(state: PlayerBarUiState) {
        composeRule.setContent {
            QItAppTheme {
                PlayerBar(
                    state = state,
                    onOpenReader = { surah, ayah -> opened = surah to ayah },
                    onPrevious = { previoused = true },
                    onTogglePlayPause = { toggled = true },
                    onNext = { nexted = true },
                    onStop = { stopped = true },
                )
            }
        }
    }

    private fun nowPlaying(ayah: Int, isPlaying: Boolean = true, isBuffering: Boolean = false) =
        NowPlaying(2, ayah, Track.ARABIC, RecitationMode.ARABIC_ENGLISH, isPlaying, isBuffering)

    private fun cd(label: Int): String =
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(label)

    @Test
    fun `shows the surah, ayah and mode of what is playing`() {
        setContent(PlayerBarUiState(nowPlaying = nowPlaying(255), surahName = "Al-Baqara"))
        composeRule.onNodeWithText("Al-Baqara 2:255").assertIsDisplayed()
        composeRule.onNodeWithText("Arabic + English").assertIsDisplayed()
        // Playing, so the pause action is offered.
        composeRule.onNodeWithContentDescription(cd(R.string.cd_pause)).assertIsDisplayed()
        assertFalse(toggled)
    }

    @Test
    fun `titles the basmala when ayah zero plays`() {
        setContent(PlayerBarUiState(nowPlaying = nowPlaying(0), surahName = "Al-Baqara"))
        composeRule.onNodeWithText("Al-Baqara · Bismillah").assertIsDisplayed()
    }

    @Test
    fun `offers play while paused`() {
        setContent(PlayerBarUiState(nowPlaying = nowPlaying(255, isPlaying = false), surahName = "Al-Baqara"))
        composeRule.onNodeWithContentDescription(cd(R.string.cd_play)).assertIsDisplayed()
    }

    @Test
    fun `shows a buffering indicator while buffering`() {
        setContent(PlayerBarUiState(nowPlaying = nowPlaying(255, isBuffering = true), surahName = "Al-Baqara"))
        composeRule.onNodeWithTag("player_buffering").assertIsDisplayed()
    }

    @Test
    fun `the buttons dispatch their actions`() {
        setContent(PlayerBarUiState(nowPlaying = nowPlaying(255), surahName = "Al-Baqara"))
        composeRule.onNodeWithContentDescription(cd(R.string.cd_previous_ayah)).performClick()
        composeRule.onNodeWithContentDescription(cd(R.string.cd_pause)).performClick()
        composeRule.onNodeWithContentDescription(cd(R.string.cd_next_ayah)).performClick()
        composeRule.onNodeWithContentDescription(cd(R.string.cd_close_player)).performClick()
        assertTrue(previoused)
        assertTrue(toggled)
        assertTrue(nexted)
        assertTrue(stopped)
    }

    @Test
    fun `tapping the bar opens the reader at the playing ayah`() {
        setContent(PlayerBarUiState(nowPlaying = nowPlaying(255), surahName = "Al-Baqara"))
        composeRule.onNodeWithText("Al-Baqara 2:255").performClick()
        org.junit.Assert.assertEquals(2 to 255, opened)
    }
}

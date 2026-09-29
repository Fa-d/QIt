package dev.sadakat.qandeel.presentation.player

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.SleepTimerStatus
import dev.sadakat.qandeel.ui.theme.QandeelAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The mode chip's menu: the modes, and the Bangla voices listed under them while Bangla plays. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<Any>()

    private fun setContent(mode: RecitationMode, voice: BanglaVoice) {
        composeRule.setContent {
            QandeelAppTheme {
                ModeAndSleepRow(
                    mode = mode,
                    sleepTimer = SleepTimerStatus.Off,
                    voice = voice,
                    actions = NowPlayingActions(
                        onTogglePlayPause = {},
                        onPrevious = {},
                        onNext = {},
                        onSeek = {},
                        onModeChange = { calls += it },
                        onVoiceChange = { calls += it },
                        onRepeatChange = {},
                        onSpeedChange = {},
                        onSleepTimerChange = {},
                        onOpenReader = { _, _ -> },
                        onStop = {},
                    ),
                )
            }
        }
    }

    @Test
    fun `the mode menu lists the voices under the modes while bangla plays`() {
        setContent(RecitationMode.ARABIC_BANGLA, voice = BanglaVoice.DEFAULT)

        composeRule.onNodeWithTag("player_mode").performClick()

        composeRule.onNodeWithText("Bangla voice").assertExists()
        composeRule.onNodeWithText("Islamic Foundation translation").assertExists()
        composeRule.onNodeWithText("Sayed Ismat Toha").assertExists()
        composeRule.onNodeWithText("Shareef Baezeed Mahmood").assertExists()
    }

    @Test
    fun `picking a voice changes it, picking the current one does not`() {
        setContent(RecitationMode.ARABIC_BANGLA, voice = BanglaVoice.DEFAULT)

        composeRule.onNodeWithTag("player_mode").performClick()
        composeRule.onNodeWithText("Sayed Ismat Toha").performClick()
        composeRule.onNodeWithTag("player_mode").performClick()
        // Already the current voice: nothing to do.
        composeRule.onNodeWithText("Islamic Foundation translation").performClick()

        assertEquals(listOf<Any>(BanglaVoice.SAYED_ISMAT_TOHA), calls)
    }

    @Test
    fun `the voices stay out of the menu in the other modes`() {
        setContent(RecitationMode.ARABIC_ENGLISH, voice = BanglaVoice.DEFAULT)

        composeRule.onNodeWithTag("player_mode").performClick()

        composeRule.onNodeWithText("Bangla voice").assertDoesNotExist()
        composeRule.onNodeWithText("Sayed Ismat Toha").assertDoesNotExist()
    }
}

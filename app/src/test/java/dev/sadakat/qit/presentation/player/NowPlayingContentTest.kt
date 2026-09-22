package dev.sadakat.qit.presentation.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackProgress
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NowPlayingContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<Any>()

    private val actions = NowPlayingActions(
        onTogglePlayPause = { calls += "toggle" },
        onPrevious = { calls += "previous" },
        onNext = { calls += "next" },
        onSeek = { calls += "seek ${it}ms" },
        onModeChange = { calls += it },
        onVoiceChange = { calls += it },
        onRepeatChange = { calls += it },
        onSpeedChange = { calls += it },
        onSleepTimerChange = { calls += it ?: "cancel sleep" },
        onOpenReader = { surah, ayah -> calls += "reader $surah:$ayah" },
        onStop = { calls += "stop" },
    )

    private fun setContent(
        sleepTimer: SleepTimerStatus = SleepTimerStatus.Off,
        translation: String? = "Our Lord, grant us mercy from Yourself.",
        progress: PlaybackProgress =
            PlaybackProgress(itemPositionMs = 2_000, surahPositionMs = 192_000, surahDurationMs = 1_037_000),
        pointer: WordPointer = WordPointer.Off,
        nowPlaying: NowPlaying = playing(),
    ) {
        composeRule.setContent {
            QItAppTheme {
                NowPlayingContent(
                    progress = { progress },
                    pointer = pointer,
                    onCollapse = { calls += "collapse" },
                    state = PlayerUiState(
                        nowPlaying = nowPlaying,
                        surahName = "Al-Kahf",
                        ayahArabic = "رَبَّنَآ ءَاتِنَا مِن لَّدُنكَ رَحْمَةً",
                        ayahTranslation = translation,
                        sleepTimer = sleepTimer,
                        // Ayah n starts at 20 s × n: the middle of the bar (8:38) is ayah 25.
                        ayahStartsMs = List(110) { it * 20_000L },
                    ),
                    actions = actions,
                )
            }
        }
    }

    private fun playing(
        repeat: RepeatSetting = RepeatSetting.Off,
        mode: RecitationMode = RecitationMode.ARABIC_ENGLISH,
    ) = NowPlaying(18, 10, Track.ARABIC, mode, isPlaying = true, isBuffering = false, repeat = repeat)

    @Test
    fun `shows the ayah, its translation and where it is`() {
        setContent()

        composeRule.onNodeWithText("Al-Kahf").assertIsDisplayed()
        composeRule.onNodeWithText("Ayah 10 of 110").assertIsDisplayed()
        composeRule.onNodeWithText("Our Lord, grant us mercy from Yourself.").assertIsDisplayed()
    }

    @Test
    fun `transport buttons dispatch`() {
        setContent()

        composeRule.onNodeWithContentDescription("Previous ayah").performClick()
        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Next ayah").performClick()

        assertEquals(listOf<Any>("previous", "toggle", "next"), calls)
    }

    @Test
    fun `the mode menu switches the recitation`() {
        setContent()

        composeRule.onNodeWithContentDescription("Recitation: Arabic + English").assertIsDisplayed()
        composeRule.onNodeWithTag("player_mode").performClick()
        composeRule.onNodeWithText("Arabic + Bangla").performClick()
        composeRule.onNodeWithTag("player_mode").performClick()
        // Already playing: nothing to do.
        composeRule.onNode(hasText("Arabic + English") and hasAnyAncestor(isPopup())).performClick()

        assertEquals(listOf<Any>(RecitationMode.ARABIC_BANGLA), calls)
    }

    @Test
    fun `the speed button offers the speeds`() {
        setContent()

        composeRule.onNodeWithContentDescription("Speed 1×").performClick()
        composeRule.onNodeWithText("1.25×").performClick()

        assertEquals(listOf<Any>(PlaybackSpeed.X1_25), calls)
    }

    @Test
    fun `the sleep chip starts a timer, and turns a running one off`() {
        setContent(sleepTimer = SleepTimerStatus.Counting(600_000L))

        composeRule.onNodeWithText("10:00").assertIsDisplayed()
        composeRule.onNodeWithTag("player_sleep").performClick()
        composeRule.onNodeWithText("30 min").performClick()
        composeRule.onNodeWithTag("player_sleep").performClick()
        composeRule.onNodeWithText("End of surah").performClick()
        composeRule.onNodeWithTag("player_sleep").performClick()
        composeRule.onNodeWithText("Turn off timer").performClick()

        assertEquals(listOf<Any>(SleepOption.Minutes(30), SleepOption.EndOfSurah, "cancel sleep"), calls)
    }

    @Test
    fun `the repeat dialog sets a range around the current ayah`() {
        setContent()

        composeRule.onNodeWithTag("player_repeat").performClick()
        composeRule.onNodeWithText("Range").performClick()
        composeRule.onNodeWithText("Ayahs 10–14").assertIsDisplayed()
        composeRule.onNodeWithText("∞").performClick()
        composeRule.onNode(hasText("Repeat") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()

        assertEquals(listOf<Any>(RepeatSetting.Range(10, 14, null)), calls)
    }

    @Test
    fun `an active repeat shows on its button`() {
        setContent(nowPlaying = playing(repeat = RepeatSetting.Ayah(3)))

        composeRule.onNodeWithContentDescription("Repeat: Ayah ×3").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun `the time bar shows the surah's elapsed and remaining time`() {
        setContent()

        composeRule.onNodeWithText("3:12").assertIsDisplayed()
        composeRule.onNodeWithText("−14:05").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Position in the surah").assert(hasStateDescription("3:12 of 17:17"))
    }

    @Test
    fun `dragging the time bar names the ayah and seeks the surah`() {
        setContent()

        composeRule.onNodeWithTag("player_time_bar").performTouchInput {
            down(centerLeft)
            repeat(DRAG_STEPS) { moveBy(Offset(width / 2f / DRAG_STEPS, 0f)) }
        }
        composeRule.onNodeWithTag("player_seek_ayah").assertIsDisplayed()
        composeRule.onNodeWithTag("player_time_bar").performTouchInput { up() }

        val seek = calls.single() as String
        assertTrue(seek, seek.startsWith("seek ") && seek.endsWith("ms"))
    }

    @Test
    fun `before the surah's length is known the bar only shows the ayahs behind`() {
        setContent(progress = PlaybackProgress.START)

        composeRule.onNodeWithTag("player_time_bar").assertDoesNotExist()
        composeRule.onNodeWithText("3:12").assertDoesNotExist()
    }

    @Test
    fun `the chevron closes the player`() {
        setContent()

        composeRule.onNodeWithContentDescription("Close player").performClick()

        assertEquals(listOf<Any>("collapse"), calls)
    }

    @Test
    fun `the recited ayah keeps its text while the pointer moves`() {
        setContent(pointer = WordPointer.Reciting(2))

        composeRule.onNodeWithText("رَبَّنَآ ءَاتِنَا مِن لَّدُنكَ رَحْمَةً").assertIsDisplayed()
    }

    @Test
    fun `the menu opens the reader and stops playback`() {
        setContent()

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Open in reader").performClick()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Stop playback").performClick()

        assertEquals(listOf<Any>("reader 18:10", "stop"), calls)
    }

    private companion object {
        const val DRAG_STEPS = 10
    }
}

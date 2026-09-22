package dev.sadakat.qit.presentation.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertEquals
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
        onSeekToAyah = { calls += "seek $it" },
        onModeChange = { calls += it },
        onRepeatChange = { calls += it },
        onSpeedChange = { calls += it },
        onSleepTimerChange = { calls += it ?: "cancel sleep" },
        onOpenReader = { surah, ayah -> calls += "reader $surah:$ayah" },
        onStop = { calls += "stop" },
    )

    private fun setContent(
        repeat: RepeatSetting = RepeatSetting.Off,
        sleepTimer: SleepTimerStatus = SleepTimerStatus.Off,
        translation: String? = "Our Lord, grant us mercy from Yourself.",
    ) {
        composeRule.setContent {
            QItAppTheme {
                NowPlayingContent(
                    state = PlayerUiState(
                        nowPlaying = NowPlaying(
                            18,
                            10,
                            Track.ARABIC,
                            RecitationMode.ARABIC_ENGLISH,
                            isPlaying = true,
                            isBuffering = false,
                            repeat = repeat,
                        ),
                        surahName = "Al-Kahf",
                        ayahArabic = "رَبَّنَآ ءَاتِنَا مِن لَّدُنكَ رَحْمَةً",
                        ayahTranslation = translation,
                        sleepTimer = sleepTimer,
                    ),
                    actions = actions,
                )
            }
        }
    }

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
    fun `choosing another mode switches the recitation`() {
        setContent()

        composeRule.onNodeWithText("+ Bangla").performClick()
        composeRule.onNodeWithText("+ English").performClick()

        assertEquals(listOf<Any>(RecitationMode.ARABIC_BANGLA), calls)
    }

    @Test
    fun `the speed chip offers the speeds`() {
        setContent()

        composeRule.onNodeWithTag("chip_speed").performClick()
        composeRule.onNodeWithText("1.25×").performClick()

        assertEquals(listOf<Any>(PlaybackSpeed.X1_25), calls)
    }

    @Test
    fun `the sleep chip starts a timer, and turns a running one off`() {
        setContent(sleepTimer = SleepTimerStatus.Counting(600_000L))

        composeRule.onNodeWithText("10:00").assertIsDisplayed()
        composeRule.onNodeWithTag("chip_sleep").performClick()
        composeRule.onNodeWithText("30 min").performClick()
        composeRule.onNodeWithTag("chip_sleep").performClick()
        composeRule.onNodeWithText("End of surah").performClick()
        composeRule.onNodeWithTag("chip_sleep").performClick()
        composeRule.onNodeWithText("Turn off timer").performClick()

        assertEquals(listOf<Any>(SleepOption.Minutes(30), SleepOption.EndOfSurah, "cancel sleep"), calls)
    }

    @Test
    fun `the repeat dialog sets a range around the current ayah`() {
        setContent()

        composeRule.onNodeWithTag("chip_repeat").performClick()
        composeRule.onNodeWithText("Range").performClick()
        composeRule.onNodeWithText("Ayahs 10–14").assertIsDisplayed()
        composeRule.onNodeWithText("∞").performClick()
        composeRule.onNode(hasText("Repeat") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()

        assertEquals(listOf<Any>(RepeatSetting.Range(10, 14, null)), calls)
    }

    @Test
    fun `an active repeat shows on its chip`() {
        setContent(repeat = RepeatSetting.Ayah(3))

        composeRule.onNodeWithText("Ayah ×3").assertIsDisplayed()
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
}

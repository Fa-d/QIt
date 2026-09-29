package dev.sadakat.qandeel.presentation.reader

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.WordPointer
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState
import dev.sadakat.qandeel.core.testing.TestQuran
import dev.sadakat.qandeel.ui.theme.QandeelAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SurahReaderScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun readerState(
        surahNumber: Int = 2,
        playingAyah: Int? = null,
        downloadState: SurahDownloadState = SurahDownloadState.NotDownloaded,
        mode: RecitationMode = RecitationMode.ARABIC_BANGLA,
        followAlong: Boolean = true,
    ) = SurahReaderUiState(
        surah = TestQuran.surah(surahNumber),
        ayahs = TestQuran.ayahs(surahNumber),
        playingAyah = playingAyah,
        downloadState = downloadState,
        mode = mode,
        followAlong = followAlong,
    )

    private var tappedAyah = 0
    private var played = false
    private var openedReadingSettings = false
    private var downloaded = false
    private var removed = false
    private var sentToWatch = false
    private var pickedMode: RecitationMode? = null
    private var consumedMessage = false
    private var longPressed = 0
    private var wordClicked: Pair<Int, Int>? = null
    private var repeated = 0
    private var retries = 0

    private fun setContent(state: SurahReaderUiState) {
        composeRule.setContent {
            QandeelAppTheme {
                SurahReaderScreen(
                    state = state,
                    pointer = WordPointer.Off,
                    onBack = {},
                    onOpenReadingSettings = { openedReadingSettings = true },
                    onAyahClick = { tappedAyah = it },
                    onPlaySurah = { played = true },
                    onDownload = { downloaded = true },
                    onRemove = { removed = true },
                    onSendToWatch = { sentToWatch = true },
                    onModeChange = { pickedMode = it },
                    onConsumeMessage = { consumedMessage = true },
                    ayahCallbacks = AyahCallbacks(
                        onLongPress = { longPressed = it },
                        onRepeat = { repeated = it },
                        onWordClick = { ayah, word -> wordClicked = ayah to word },
                    ),
                    onRetry = { retries++ },
                )
            }
        }
    }

    @Test
    fun `marks the playing ayah and reports taps on other ayahs`() {
        setContent(readerState(playingAyah = 2))
        composeRule.onNodeWithTag("ayah_2").assert(isPlayingAyah())
        composeRule.onNodeWithTag("ayah_3").assert(isNotPlayingAyah())
        composeRule.onNodeWithTag("ayah_3").performClick()
        assertEquals(3, tappedAyah)
    }

    @Test
    fun `shows the basmala header only for surahs that have one`() {
        setContent(readerState(surahNumber = 2))
        composeRule.onNodeWithText(context.getString(R.string.basmala)).assertIsDisplayed()
    }

    @Test
    fun `shows no basmala header for surahs without one`() {
        setContent(readerState(surahNumber = 9))
        composeRule.onNodeWithText(context.getString(R.string.basmala)).assertDoesNotExist()
    }

    @Test
    fun `the play button plays the surah`() {
        setContent(readerState())
        composeRule.onNodeWithText(context.getString(R.string.play_surah)).performClick()
        assertTrue(played)
    }

    @Test
    fun `the Aa action opens the reading settings`() {
        setContent(readerState())
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_reading_settings)).performClick()
        assertTrue(openedReadingSettings)
    }

    @Test
    fun `the overflow menu offers a download for a not-downloaded surah`() {
        setContent(readerState(downloadState = SurahDownloadState.NotDownloaded))
        openOverflow()
        composeRule.onNodeWithText(context.getString(R.string.download_for_offline)).performClick()
        assertTrue(downloaded)
    }

    @Test
    fun `the overflow menu shows disabled progress while downloading`() {
        setContent(readerState(downloadState = SurahDownloadState.Downloading(completedFiles = 3, totalFiles = 7)))
        openOverflow()
        composeRule.onNodeWithText(context.getString(R.string.downloading_percent, 43))
            .assertIsNotEnabled()
    }

    @Test
    fun `a downloaded surah offers removal, confirmed by the dialog`() {
        setContent(readerState(downloadState = SurahDownloadState.Downloaded))
        openOverflow()
        composeRule.onNodeWithText(context.getString(R.string.remove_download)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.remove_download_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.remove)).performClick()
        assertTrue(removed)
    }

    @Test
    fun `a failed download offers a retry`() {
        setContent(readerState(downloadState = SurahDownloadState.Failed(completedFiles = 2, totalFiles = 7)))
        openOverflow()
        composeRule.onNodeWithText(context.getString(R.string.retry_download)).performClick()
        assertTrue(downloaded)
    }

    @Test
    fun `send to watch is a labelled overflow item`() {
        setContent(readerState())
        openOverflow()
        composeRule.onNodeWithText(context.getString(R.string.send_to_watch)).performClick()
        assertTrue(sentToWatch)
    }

    @Test
    fun `the translation follows the mode and the reading pref`() {
        // One content per test; the reader state is snapshot state the content reads live.
        val state = mutableStateOf(readerState())
        composeRule.setContent {
            QandeelAppTheme {
                SurahReaderScreen(
                    state = state.value,
                    pointer = WordPointer.Off,
                    onBack = {},
                    onOpenReadingSettings = {},
                    onAyahClick = {},
                    onPlaySurah = {},
                    onDownload = {},
                    onRemove = {},
                    onSendToWatch = {},
                    onModeChange = {},
                    onConsumeMessage = {},
                )
            }
        }
        composeRule.onNodeWithText("বাংলা 2:1").assertIsDisplayed()

        composeRule.runOnIdle { state.value = state.value.copy(mode = RecitationMode.ARABIC_ENGLISH) }
        composeRule.onNodeWithText("English 2:1").assertIsDisplayed()

        composeRule.runOnIdle { state.value = state.value.copy(showTranslation = false) }
        composeRule.onNodeWithText("English 2:1").assertDoesNotExist()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                mode = RecitationMode.ARABIC_ONLY,
                showTranslation = true,
            )
        }
        composeRule.onNodeWithText("বাংলা 2:1").assertDoesNotExist()
    }

    @Test
    fun `selecting a mode segment reports it`() {
        setContent(readerState())
        composeRule.onNodeWithText(context.getString(R.string.mode_english)).performClick()
        assertEquals(RecitationMode.ARABIC_ENGLISH, pickedMode)
    }

    @Test
    fun `a deep link lands on its ayah`() {
        setContent(readerState().copy(initialAyah = 255))
        composeRule.onNodeWithTag("ayah_255").assertIsDisplayed()
    }

    @Test
    fun `follow-along keeps the reciting ayah in view`() {
        setContent(readerState(playingAyah = 200))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ayah_200").assertIsDisplayed()
    }

    @Test
    fun `a jump chip brings back the reciting ayah after the reader scrolls away`() {
        setContent(readerState(playingAyah = 1, followAlong = false))
        composeRule.onNodeWithTag("ayah_1").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.jump_to_reciting_ayah)).assertDoesNotExist()

        composeRule.onNodeWithTag("ayah_list").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.jump_to_reciting_ayah)).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.jump_to_reciting_ayah)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ayah_1").assertIsDisplayed()
    }

    @Test
    fun `a user drag pauses follow-along until the reciting ayah jumps back`() {
        // Snapshot state, so recomposition follows the test's edits like it would the ViewModel's.
        val state = mutableStateOf(readerState(playingAyah = 5))
        composeRule.setContent {
            QandeelAppTheme {
                SurahReaderScreen(
                    state = state.value,
                    pointer = WordPointer.Off,
                    onBack = {},
                    onOpenReadingSettings = {},
                    onAyahClick = {},
                    onPlaySurah = {},
                    onDownload = {},
                    onRemove = {},
                    onSendToWatch = {},
                    onModeChange = {},
                    onConsumeMessage = {},
                )
            }
        }
        composeRule.onNodeWithTag("ayah_list").performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        // The recitation advances; a paused list must not chase it.
        state.value = state.value.copy(playingAyah = 60)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.jump_to_reciting_ayah)).assertIsDisplayed()
        composeRule.onNodeWithTag("ayah_60").assertDoesNotExist()

        composeRule.onNodeWithText(context.getString(R.string.jump_to_reciting_ayah)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ayah_60").assertIsDisplayed()
    }

    @Test
    fun `a failing load shows the error message instead of the surah`() {
        setContent(readerState(surahNumber = 2).copy(surah = null, ayahs = emptyList(), loadFailed = true))
        composeRule.onNodeWithText(context.getString(R.string.load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.play_surah)).assertDoesNotExist()
    }

    @Test
    fun `a load in progress shows a progress bar`() {
        setContent(readerState().copy(surah = null, ayahs = emptyList()))
        composeRule.onNode(progressBar()).assertExists()
    }

    @Test
    fun `watch results surface as a snackbar and are consumed`() {
        setContent(readerState().copy(message = ReaderMessage.SentToWatch(watches = 2)))
        composeRule.onNodeWithText("Sent to 2 watches").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(5_000)
        composeRule.waitForIdle()
        assertTrue(consumedMessage)
    }

    private fun openOverflow() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_more_options)).performClick()
    }

    private fun progressBar() = SemanticsMatcher("progress bar") {
        androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo in it.config
    }

    @Test
    fun `a long press on an ayah opens its options`() {
        setContent(readerState())
        composeRule.onNodeWithTag("ayah_list").performScrollToKey(3)
        composeRule.onNodeWithTag("ayah_3").performTouchInput { longClick() }
        assertEquals(3, longPressed)
        assertEquals(0, tappedAyah)
    }

    @Test
    fun `an ayah says what a tap and a long press do, and reads its number`() {
        setContent(readerState())
        composeRule.onNodeWithTag("ayah_list").performScrollToKey(2)
        composeRule.onNodeWithTag("ayah_2").assert(
            SemanticsMatcher("labelled actions") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label ==
                    context.getString(R.string.ayah_cd_play_from_here) &&
                    it.config.getOrNull(SemanticsActions.OnLongClick)?.label ==
                    context.getString(R.string.ayah_cd_options)
            },
        )
        composeRule.onNodeWithContentDescription("Ayah 2", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a screen reader can repeat an ayah without a long press`() {
        setContent(readerState())
        composeRule.onNodeWithTag("ayah_list").performScrollToKey(3)
        val actions = composeRule.onNodeWithTag("ayah_3").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnIdle { actions.first { it.label == context.getString(R.string.ayah_action_repeat) }.action() }
        assertEquals(3, repeated)
    }

    @Test
    fun `in word by word a tap on a word plays from that word`() {
        val words = TestQuran.ayahs(2)[0].arabic.split(" ").size
        setContent(readerState().copy(wordMeanings = mapOf(1 to List(words) { "m$it" })))
        composeRule.onNodeWithTag("ayah_list").performScrollToKey(1)
        composeRule.onNodeWithText("m0", useUnmergedTree = true).performClick()
        assertEquals(1 to 0, wordClicked)
        assertEquals(0, tappedAyah)
    }

    @Test
    fun `a failed load can be retried`() {
        setContent(SurahReaderUiState(loadFailed = true))
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `the options sheet shows the words with their meanings and does each action`() {
        var done = ""
        composeRule.setContent {
            QandeelAppTheme {
                AyahActionsContent(
                    ayah = AyahActionsUi(
                        ayah = 1,
                        arabic = "بِسْمِ ٱللَّهِ",
                        translation = "In the name of Allah",
                        words = listOf(GlossaryWord("بِسْمِ", "In (the) name"), GlossaryWord("ٱللَّهِ", "(of) Allah")),
                    ),
                    title = "Al-Fatihah · 1:1",
                    actions = AyahSheetActions(
                        onPlay = { done += "play " },
                        onRepeat = { done += "repeat " },
                        onCopy = { done += "copy " },
                        onShare = { done += "share" },
                    ),
                )
            }
        }
        composeRule.onNodeWithText("(of) Allah").assertIsDisplayed()
        listOf(
            R.string.ayah_action_play,
            R.string.ayah_action_repeat,
            R.string.ayah_action_copy,
            R.string.ayah_action_share,
        )
            .forEach { composeRule.onNodeWithText(context.getString(it)).performScrollTo().performClick() }
        assertEquals("play repeat copy share", done)
    }
}

/** Matches the ayah node that carries the playing semantics flag. */
private fun isPlayingAyah() = SemanticsMatcher("ayah is playing") { it.config.getOrNull(AyahIsPlaying) == true }

private fun isNotPlayingAyah() = SemanticsMatcher("ayah is not playing") {
    it.config.getOrNull(AyahIsPlaying) != true
}

package dev.sadakat.qit.presentation.reader

import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.testing.TestQuran
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertEquals
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

    private fun readerState(surahNumber: Int = 2, playingAyah: Int? = null) = SurahReaderUiState(
        surah = TestQuran.surah(surahNumber),
        ayahs = TestQuran.ayahs(surahNumber),
        playingAyah = playingAyah,
    )

    private var tappedAyah = 0
    private var played = false

    private fun setContent(state: SurahReaderUiState) {
        composeRule.setContent {
            QItAppTheme {
                SurahReaderScreen(
                    state = state,
                    onBack = {},
                    onAyahClick = { tappedAyah = it },
                    onPlaySurah = { played = true },
                    onDownload = {},
                    onRemove = {},
                    onSendToWatch = {},
                    onModeChange = {},
                    onConsumeMessage = {},
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
        val basmala = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(dev.sadakat.qit.R.string.basmala)
        composeRule.onNodeWithText(basmala).assertIsDisplayed()
    }

    @Test
    fun `shows no basmala header for surahs without one`() {
        setContent(readerState(surahNumber = 9))
        val basmala = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(dev.sadakat.qit.R.string.basmala)
        composeRule.onNodeWithText(basmala).assertDoesNotExist()
    }

    @Test
    fun `the play button plays the surah`() {
        setContent(readerState())
        composeRule.onNodeWithText(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .targetContext.getString(dev.sadakat.qit.R.string.play_surah),
        ).performClick()
        assertTrue(played)
    }
}

/** Matches the ayah node that carries the playing semantics flag. */
private fun isPlayingAyah() = SemanticsMatcher("ayah is playing") { it.config.getOrNull(AyahIsPlaying) == true }

private fun isNotPlayingAyah() = SemanticsMatcher("ayah is not playing") { it.config.getOrNull(AyahIsPlaying) != true }

package dev.sadakat.qit.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.testing.snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Ayahs laid out word by word: at rest, and reciting with the pointer on a word that carries a pause mark. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class WordByWordScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun banglaLight() = composeRule.snapshot("reader_word_by_word_bangla") { Ayahs(BANGLA, Track.BANGLA) }

    @Test
    fun englishDark() = composeRule.snapshot("reader_word_by_word_english_dark", tone = QItTone.DARK) {
        Ayahs(ENGLISH, Track.ENGLISH)
    }
}

@Composable
private fun Ayahs(meanings: List<String>, track: Track) {
    Column(
        modifier = Modifier.padding(QItTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(QItTheme.spacing.sm),
    ) {
        AyahItem(
            ayah = AYAH,
            translationTrack = track,
            isPlaying = false,
            pulsed = false,
            onClick = {},
            wordMeanings = meanings,
        )
        AyahItem(
            ayah = AYAH,
            translationTrack = track,
            isPlaying = true,
            pulsed = false,
            onClick = {},
            // رَيْبَ ۛ — the pause mark is part of the word the pointer is on.
            pointer = WordPointer.Reciting(3),
            wordMeanings = meanings,
        )
    }
}

private val AYAH = Ayah(
    surah = 2,
    number = 2,
    globalNumber = 9,
    arabic = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًۭى لِّلْمُتَّقِينَ",
    english = "This is the Book about which there is no doubt, a guidance for those conscious of Allah -",
    bangla = "এ সেই কিতাব যাতে কোনই সন্দেহ নেই। পথ প্রদর্শনকারী পরহেযগারদের জন্য,",
)

private val ENGLISH = listOf("That", "(is) the book", "no", "doubt", "in it", "a Guidance", "for the God-conscious")

private val BANGLA = listOf(
    "(এটা) সেই",
    "মহাগ্রন্থ (আল্লাহর)",
    "নেই",
    "কোনো সন্দেহ",
    "তাঁরমধ্যে",
    "সৎপথ নির্দেশ (হেদায়াত)",
    "মুত্তাকীদের জন্য",
)

package dev.sadakat.qit.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.SurahListening
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.core.testing.TestQuran
import dev.sadakat.qit.testing.snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ReaderScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = composeRule.snapshot("reader_light") {
        Reader(readerState())
    }

    @Test
    fun dark() = composeRule.snapshot("reader_dark", darkTheme = true) {
        Reader(readerState())
    }

    @Test
    fun arabicOnly() = composeRule.snapshot("reader_arabic_only") {
        Reader(readerState(mode = RecitationMode.ARABIC_ONLY))
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, fontScale = 1.3f)
    fun stress() = composeRule.snapshot("reader_stress", arabicScale = 1.75f) {
        Reader(readerState())
    }

    private fun readerState(mode: RecitationMode = RecitationMode.ARABIC_BANGLA) = SurahReaderUiState(
        surah = TestQuran.surah(2),
        ayahs = TestQuran.ayahs(2),
        mode = mode,
        playingAyah = 3,
        // Keep the golden at the top of the surah: ayah 3 is in view without the auto-scroll.
        followAlong = false,
        heard = listOf(3, 3, 2) + List(283) { 0 },
        listening = SurahListening(
            surah = 2,
            ayahCount = 286,
            rounds = 0,
            ayahsIntoNextRound = 3,
            ayahsHeard = 3,
            totalListens = 8,
            lastHeardAt = 1L,
            listenedMs = 60_000L,
        ),
    )
}

@Composable
private fun Reader(state: SurahReaderUiState) {
    SurahReaderScreen(
        state = state,
        pointer = WordPointer.Reciting(0),
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

package dev.sadakat.qit.presentation.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.domain.model.QuranMeta
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
class ProgressScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = composeRule.snapshot("progress_light") { Progress(progress(rounds = 2)) }

    @Test
    fun dark() = composeRule.snapshot("progress_dark", darkTheme = true) { Progress(progress()) }

    @Test
    fun empty() = composeRule.snapshot("progress_empty") { Progress(ProgressUiState(isLoading = false)) }

    @Test
    @Config(fontScale = 1.3f)
    fun largeText() = composeRule.snapshot("progress_stress", arabicScale = 1.75f) { Progress(progress()) }

    @Composable
    private fun Progress(state: ProgressUiState) {
        ProgressScreen(
            state = state,
            onBack = {},
            onOrderChange = {},
            onReset = {},
            onOpenReader = { _, _ -> },
        )
    }

    private fun progress(rounds: Int = 0) = ProgressUiState(
        isLoading = false,
        ayahsHeard = 1_204,
        coverage = 1_204f / QuranMeta.TOTAL_AYAHS,
        rounds = rounds,
        listenedMs = (14 * 60 + 5) * 60_000L + 30_000L,
        rows = listOf(
            row(1, rounds = 1, intoNext = 3, ayahsHeard = 7, listens = 10),
            row(18, rounds = 0, intoNext = 64, ayahsHeard = 64, listens = 64),
            row(112, rounds = 2, intoNext = 0, ayahsHeard = 4, listens = 8),
        ),
    )

    private fun row(surah: Int, rounds: Int, intoNext: Int, ayahsHeard: Int, listens: Int): ProgressRowUi {
        val meta = TestQuran.surah(surah)
        return ProgressRowUi(
            surah = surah,
            nameEnglish = meta.nameEnglish,
            nameArabicShort = meta.nameArabicShort,
            rounds = rounds,
            ayahsHeard = ayahsHeard,
            ayahCount = meta.ayahCount,
            ayahsIntoNextRound = intoNext,
            nextRoundProgress = intoNext.toFloat() / meta.ayahCount,
            totalListens = listens,
        )
    }
}

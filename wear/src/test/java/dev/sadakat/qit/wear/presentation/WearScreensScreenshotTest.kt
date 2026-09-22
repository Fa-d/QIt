package dev.sadakat.qit.wear.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.testing.TestQuran
import dev.sadakat.qit.wear.presentation.home.HomeScreen
import dev.sadakat.qit.wear.presentation.home.WearHomeUiState
import dev.sadakat.qit.wear.presentation.juz.JuzRowUiModel
import dev.sadakat.qit.wear.presentation.juz.JuzScreen
import dev.sadakat.qit.wear.presentation.juz.WearJuzUiState
import dev.sadakat.qit.wear.presentation.mode.ModeScreen
import dev.sadakat.qit.wear.presentation.mode.WearModeUiState
import dev.sadakat.qit.wear.presentation.nowplaying.NowPlayingScreen
import dev.sadakat.qit.wear.presentation.nowplaying.TextPage
import dev.sadakat.qit.wear.presentation.nowplaying.WearNowPlayingUiState
import dev.sadakat.qit.wear.presentation.options.OptionsScreen
import dev.sadakat.qit.wear.presentation.options.RepeatSelection
import dev.sadakat.qit.wear.presentation.options.WearOptionsUiState
import dev.sadakat.qit.wear.presentation.surah.SurahScreen
import dev.sadakat.qit.wear.presentation.surah.WearSurahUiState
import dev.sadakat.qit.wear.presentation.surahlist.SurahListScreen
import dev.sadakat.qit.wear.presentation.surahlist.SurahRowUiModel
import dev.sadakat.qit.wear.presentation.surahlist.WearSurahListUiState
import dev.sadakat.qit.wear.testing.wearSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Every screen of the watch, on the large round display. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.WearOSLargeRound)
class WearScreensLargeRoundScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home() = composeRule.wearSnapshot("wear_home_large") { HomeGolden() }

    @Test
    fun surahs() = composeRule.wearSnapshot("wear_surahs_large") { SurahsGolden() }

    @Test
    fun juz() = composeRule.wearSnapshot("wear_juz_large") { JuzGolden() }

    @Test
    fun surah() = composeRule.wearSnapshot("wear_surah_large") { SurahGolden() }

    @Test
    fun nowPlayingControls() = composeRule.wearSnapshot("wear_nowplaying_controls_large") { NowPlayingGolden() }

    @Test
    fun nowPlayingText() = composeRule.wearSnapshot("wear_nowplaying_text_large") { NowPlayingTextGolden() }

    @Test
    fun options() = composeRule.wearSnapshot("wear_options_large") { OptionsGolden() }

    @Test
    fun mode() = composeRule.wearSnapshot("wear_mode_large") { ModeGolden() }
}

/** The same screens on the small round display, where the edges clip hardest. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.WearOSSmallRound)
class WearScreensSmallRoundScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home() = composeRule.wearSnapshot("wear_home_small") { HomeGolden() }

    @Test
    fun surahs() = composeRule.wearSnapshot("wear_surahs_small") { SurahsGolden() }

    @Test
    fun juz() = composeRule.wearSnapshot("wear_juz_small") { JuzGolden() }

    @Test
    fun surah() = composeRule.wearSnapshot("wear_surah_small") { SurahGolden() }

    @Test
    fun nowPlayingControls() = composeRule.wearSnapshot("wear_nowplaying_controls_small") { NowPlayingGolden() }

    @Test
    fun nowPlayingText() = composeRule.wearSnapshot("wear_nowplaying_text_small") { NowPlayingTextGolden() }

    @Test
    fun options() = composeRule.wearSnapshot("wear_options_small") { OptionsGolden() }

    @Test
    fun mode() = composeRule.wearSnapshot("wear_mode_small") { ModeGolden() }
}

@Composable
private fun HomeGolden() {
    // Centered on the last row so the edge button stands at full size — the state the golden
    // shows and the accessibility checks measure.
    val listState = rememberTransformingLazyColumnState(initialAnchorItemIndex = 3)
    AppScaffold {
        HomeScreen(
            uiState = WearHomeUiState(loaded = true, downloadedCount = 4, isQueued = true),
            onSurahsClick = {},
            onJuzClick = {},
            onDownloadedClick = {},
            onModeClick = {},
            onNowPlayingClick = {},
            onContinueClick = {},
            listState = listState,
        )
    }
}

@Composable
private fun SurahsGolden() {
    AppScaffold {
        SurahListScreen(
            uiState = WearSurahListUiState(
                rows = listOf(
                    SurahRowUiModel(TestQuran.surah(2), SurahDownloadState.Downloading(123, 286)),
                    SurahRowUiModel(TestQuran.surah(112), SurahDownloadState.Downloaded),
                ),
            ),
            onSurahClick = {},
        )
    }
}

@Composable
private fun JuzGolden() {
    AppScaffold {
        JuzScreen(
            uiState = WearJuzUiState(
                rows = listOf(
                    JuzRowUiModel(juz = 14, start = AyahRef(53, 1), surahName = "An-Najm"),
                    JuzRowUiModel(juz = 15, start = AyahRef(17, 1), surahName = "Al-Israa"),
                ),
            ),
            onJuzClick = {},
        )
    }
}

@Composable
private fun SurahGolden() {
    // Centered on the last row so the Play edge button stands at full size.
    val listState = rememberTransformingLazyColumnState(initialAnchorItemIndex = 2)
    AppScaffold {
        SurahScreen(
            uiState = WearSurahUiState(surah = TestQuran.surah(2), mode = RecitationMode.ARABIC_BANGLA),
            onPlay = {},
            onDownload = {},
            onRemove = {},
            onModeClick = {},
            listState = listState,
        )
    }
}

@Composable
private fun NowPlayingGolden() {
    AppScaffold {
        NowPlayingScreen(
            uiState = WearNowPlayingUiState(
                surahNumber = 18,
                surahName = "Al-Kahf",
                ayah = 10,
                isPlaying = true,
                progress = 0.45f,
                volume = 0.6f,
            ),
            onPrevious = {},
            onTogglePlayPause = {},
            onNext = {},
            onVolumeSteps = {},
            onOpenOptions = {},
        )
    }
}

@Composable
private fun NowPlayingTextGolden() {
    AppScaffold {
        TextPage(
            uiState = WearNowPlayingUiState(
                surahNumber = 18,
                surahName = "Al-Kahf",
                ayah = 10,
                ayahText = "رَبَّنَآ ءَاتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا",
                translation = "Our Lord, bestow on us mercy from Yourself",
                isPlaying = true,
            ),
        )
    }
}

@Composable
private fun OptionsGolden() {
    AppScaffold {
        OptionsScreen(
            uiState = WearOptionsUiState(
                speed = PlaybackSpeed.X1_25,
                repeat = RepeatSelection.THREE_TIMES,
                sleepRemainingMs = 14 * 60_000L,
            ),
            onSelectSpeed = {},
            onSelectRepeat = {},
            onSelectSleep = {},
        )
    }
}

@Composable
private fun ModeGolden() {
    AppScaffold {
        ModeScreen(uiState = WearModeUiState(RecitationMode.ARABIC_BANGLA), onSelect = {})
    }
}

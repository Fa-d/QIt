package dev.sadakat.qit.presentation.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import dev.sadakat.qit.core.designsystem.skin.QItStyle
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.Revelation
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.testing.snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class HomeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = composeRule.snapshot("home_light") { Home(home()) }

    @Test
    fun dark() = composeRule.snapshot("home_dark", tone = QItTone.DARK) { Home(home()) }

    @Test
    fun searchWithJump() = composeRule.snapshot("home_search_jump") {
        Home(
            home().copy(
                query = "2:255",
                jumpTarget = AyahJumpUi(AyahRef(2, 255), "Al-Baqarah"),
                surahs = emptyList(),
            ),
        )
    }

    @Test
    fun juz() = composeRule.snapshot("home_juz") { Home(home().copy(browse = BrowseMode.JUZ)) }

    @Test
    fun emptySearch() = composeRule.snapshot("home_search_empty") {
        Home(home().copy(query = "zzz", surahs = emptyList(), jumpTarget = null))
    }

    @Test
    fun loadError() = composeRule.snapshot("home_load_error") {
        Home(HomeUiState(isLoading = false, loadFailed = true))
    }

    @Test
    fun glass() = composeRule.snapshot("home_glass", style = QItStyle.GLASS) { Home(home()) }

    @Test
    fun expressiveDark() =
        composeRule.snapshot("home_expressive_dark", style = QItStyle.EXPRESSIVE, tone = QItTone.DARK) {
            Home(home())
        }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel5, fontScale = 1.3f)
    fun largeText() = composeRule.snapshot("home_large_text") { Home(home()) }

    @androidx.compose.runtime.Composable
    private fun Home(state: HomeUiState) {
        HomeScreen(
            state = state,
            onQueryChange = {},
            onBrowseChange = {},
            onOpenReader = { _, _ -> },
            onOpenProgress = {},
            onContinuePlayPause = {},
            onOpenReadingSettings = {},
            onOpenAppearance = {},
            onRetry = {},
        )
    }

    private fun home() = HomeUiState(
        isLoading = false,
        continueListening = ContinueListeningUi(18, "Al-Kahf", "الكهف", 23, 110, isCurrent = true, isPlaying = true),
        surahs = SURAHS.mapIndexed { index, surah ->
            SurahRowUi(
                surah = surah,
                download = when (surah.number) {
                    1 -> SurahDownloadState.Downloaded
                    2 -> SurahDownloadState.Downloading(completedFiles = 120, totalFiles = 287)
                    else -> SurahDownloadState.NotDownloaded
                },
                isPlaying = index == SURAHS.lastIndex,
            )
        },
        juz = (1..QuranMeta.JUZ_COUNT).map {
            JuzRowUi(it, QuranMeta.juzStart(it), JUZ_SURAHS.getOrElse(it - 1) { "" })
        },
    )

    private companion object {
        val SURAHS = listOf(
            Surah(1, "الفاتحة", "Al-Fatihah", "The Opening", 7, Revelation.MECCAN),
            Surah(2, "البقرة", "Al-Baqarah", "The Cow", 286, Revelation.MEDINAN),
            Surah(3, "آل عمران", "Ali 'Imran", "Family of Imran", 200, Revelation.MEDINAN),
            Surah(4, "النساء", "An-Nisa", "The Women", 176, Revelation.MEDINAN),
            Surah(5, "المائدة", "Al-Ma'idah", "The Table Spread", 120, Revelation.MEDINAN),
            Surah(18, "الكهف", "Al-Kahf", "The Cave", 110, Revelation.MECCAN),
        )
        val JUZ_SURAHS =
            listOf("Al-Fatihah", "Al-Baqarah", "Al-Baqarah", "Ali 'Imran", "An-Nisa", "An-Nisa", "Al-Ma'idah")
    }
}

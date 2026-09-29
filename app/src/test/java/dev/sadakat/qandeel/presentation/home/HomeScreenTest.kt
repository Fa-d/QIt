package dev.sadakat.qandeel.presentation.home

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.domain.model.AyahRef
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState
import dev.sadakat.qandeel.core.testing.TestQuran
import dev.sadakat.qandeel.ui.theme.QandeelAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val rows = listOf(1, 2, 112).map {
        SurahRowUi(TestQuran.surah(it), SurahDownloadState.NotDownloaded, isPlaying = false)
    }
    private val juz = (1..QuranMeta.JUZ_COUNT).map { JuzRowUi(it, QuranMeta.juzStart(it), "Surah") }
    private val card = ContinueListeningUi(2, "Al-Baqara", "البقرة", 255, 286, isCurrent = false, isPlaying = false)

    private val opened = mutableListOf<Pair<Int, Int>>()
    private val queries = mutableListOf<String>()
    private var browse: BrowseMode? = null
    private var playPauses = 0
    private var openedProgress = false
    private var openedAppearance = false
    private var retries = 0

    private fun setContent(state: HomeUiState) {
        composeRule.setContent {
            QandeelAppTheme {
                HomeScreen(
                    state = state,
                    onQueryChange = { queries += it },
                    onBrowseChange = { browse = it },
                    onOpenReader = { surah, ayah -> opened += surah to ayah },
                    onOpenProgress = { openedProgress = true },
                    onContinuePlayPause = { playPauses++ },
                    onOpenReadingSettings = {},
                    onOpenAppearance = { openedAppearance = true },
                    onRetry = { retries++ },
                )
            }
        }
    }

    private fun loaded(query: String = "", browse: BrowseMode = BrowseMode.SURAH, jump: AyahJumpUi? = null) =
        HomeUiState(
            isLoading = false,
            continueListening = card,
            query = query,
            jumpTarget = jump,
            browse = browse,
            surahs = rows,
            juz = juz,
        )

    @Test
    fun `shows the continue card and the surahs, and a tap opens a surah`() {
        setContent(loaded())

        composeRule.onNodeWithTag("continue_listening").assertIsDisplayed()
        composeRule.onNodeWithText("Al-Faatiha").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("surah_112"))
        composeRule.onNodeWithTag("surah_112").performClick()

        assertEquals(listOf(112 to 0), opened)
    }

    @Test
    fun `the card opens the reader at its ayah and its button plays`() {
        setContent(loaded())

        composeRule.onNodeWithContentDescription("Play").performClick()
        composeRule.onNodeWithTag("continue_listening").performClick()

        assertEquals(1, playPauses)
        assertEquals(listOf(2 to 255), opened)
    }

    @Test
    fun `the insights action opens the listening progress`() {
        setContent(loaded())

        composeRule.onNodeWithContentDescription("Your listening").performClick()

        assertEquals(true, openedProgress)
    }

    @Test
    fun `typing reports the query`() {
        setContent(loaded())

        composeRule.onNodeWithTag("home_search").performTextInput("kahf")

        assertEquals("kahf", queries.last())
    }

    @Test
    fun `a verse reference jumps straight to the ayah`() {
        setContent(loaded(query = "2:255", jump = AyahJumpUi(AyahRef(2, 255), "Al-Baqara")))

        composeRule.onNodeWithText("Go to 2:255").performClick()

        assertEquals(listOf(2 to 255), opened)
    }

    @Test
    fun `the juz list opens a juz where it starts`() {
        setContent(loaded(browse = BrowseMode.JUZ))

        composeRule.onNodeWithText("Surahs").performClick()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("juz_2"))
        composeRule.onNodeWithTag("juz_2").performClick()

        assertEquals(BrowseMode.SURAH, browse)
        assertEquals(listOf(2 to 142), opened)
    }

    @Test
    fun `a search with no match says so`() {
        setContent(loaded(query = "zzz").copy(surahs = emptyList()))

        composeRule.onNodeWithText("No surah matches \"zzz\"").assertIsDisplayed()
    }

    @Test
    fun `a text failure shows the error`() {
        setContent(HomeUiState(isLoading = false, loadFailed = true))

        composeRule.onNodeWithText("Couldn't load the Quran text.").assertIsDisplayed()
    }

    @Test
    fun `a failed load can be retried`() {
        setContent(HomeUiState(isLoading = false, loadFailed = true))

        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `an empty search teaches the syntax, and its examples fill the field`() {
        setContent(loaded(query = "zzz").copy(surahs = emptyList()))

        composeRule.onNodeWithText("Try a name, a number, or a verse like 2:255").assertIsDisplayed()
        composeRule.onNodeWithText("2:255").performClick()
        assertEquals("2:255", queries.last())
    }

    @Test
    fun `the palette action opens Appearance`() {
        setContent(loaded())

        composeRule.onNodeWithContentDescription("Appearance").performClick()
        assertEquals(true, openedAppearance)
    }

    @Test
    fun `rows say what a tap does and read their number with its kind`() {
        setContent(loaded())

        composeRule.onNodeWithTag("surah_1").assert(
            SemanticsMatcher("opens the surah") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open surah"
            },
        )
        composeRule.onNodeWithContentDescription("Surah 1", useUnmergedTree = true).assertExists()
    }
}

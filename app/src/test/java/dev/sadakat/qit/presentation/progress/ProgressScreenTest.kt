package dev.sadakat.qit.presentation.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.ListeningOrder
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.testing.TestQuran
import dev.sadakat.qit.ui.theme.QItAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProgressScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val opened = mutableListOf<Pair<Int, Int>>()
    private var resets = 0
    private var pickedOrder: ListeningOrder? = null

    private fun setContent(state: ProgressUiState) {
        composeRule.setContent {
            QItAppTheme {
                ProgressScreen(
                    state = state,
                    onBack = {},
                    onOrderChange = { pickedOrder = it },
                    onReset = { resets++ },
                    onOpenReader = { surah, ayah -> opened += surah to ayah },
                )
            }
        }
    }

    private fun loaded(order: ListeningOrder = ListeningOrder.RECENT) = ProgressUiState(
        isLoading = false,
        ayahsHeard = 1_204,
        coverage = 1_204f / QuranMeta.TOTAL_AYAHS,
        rounds = 0,
        listenedMs = (14 * 60 + 5) * 60_000L,
        order = order,
        rows = listOf(
            row(1, rounds = 1, intoNext = 3, ayahsHeard = 7, listens = 10),
            row(112, rounds = 0, intoNext = 3, ayahsHeard = 3, listens = 3),
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

    @Test
    fun `shows the summary and the rows, and a tap opens the reader`() {
        setContent(loaded())

        val number = String.format(Locale.getDefault(), "%,d", 1_204)
        val total = String.format(Locale.getDefault(), "%,d", QuranMeta.TOTAL_AYAHS)
        composeRule.onNodeWithText(number).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.progress_of_ayahs_heard, total)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.progress_coverage_line, 19, "14 h 5 min"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag("progress_surah_1").assertIsDisplayed()
        composeRule.onNodeWithText("1 full round · 10 listens").assertIsDisplayed()

        composeRule.onNodeWithTag("progress_surah_112").performClick()

        assertEquals(listOf(112 to 0), opened)
    }

    @Test
    fun `the order toggle reports the pick`() {
        setContent(loaded())

        composeRule.onNodeWithText("Most heard").performClick()

        assertEquals(ListeningOrder.MOST_HEARD, pickedOrder)
    }

    @Test
    fun `reset asks first and only resets when confirmed`() {
        setContent(loaded())

        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_more_options)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.progress_reset)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.progress_reset_title)).assertIsDisplayed()
        assertEquals(0, resets)

        composeRule.onNodeWithText(context.getString(R.string.progress_reset_confirm)).performClick()

        assertEquals(1, resets)
    }

    @Test
    fun `cancel closes the reset dialog without resetting`() {
        setContent(loaded())

        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_more_options)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.progress_reset)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

        assertEquals(0, resets)
    }

    @Test
    fun `nothing heard shows the empty state and no reset menu`() {
        setContent(ProgressUiState(isLoading = false))

        composeRule.onNodeWithText("Nothing heard yet").assertIsDisplayed()
        composeRule.onNodeWithText("Al-Faatiha").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_more_options)).assertDoesNotExist()
    }
}

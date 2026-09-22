package dev.sadakat.qit.presentation.progress

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import dev.sadakat.qit.core.domain.model.ListeningOrder
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.repository.ListeningCounts
import dev.sadakat.qit.core.testing.FakeListeningHistory
import dev.sadakat.qit.core.testing.FakeQuranText
import dev.sadakat.qit.core.testing.MainDispatcherRule
import dev.sadakat.qit.presentation.awaitWhere
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val history = FakeListeningHistory()
    private val quranText = FakeQuranText()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = ProgressViewModel(history, quranText, handle)

    @Test
    fun `sums the whole quran and lists the heard surahs, most recent first`() = runTest {
        history.counts.value = counts(
            heard = allAyahsOf(1, times = 1) + firstAyahsOf(114, ayahs = 3),
            lastHeardAt = mapOf(1 to 1_000L, 114 to 2_000L),
            listenedMs = mapOf(1 to 3_600_000L, 114 to 300_000L),
        )
        viewModel().uiState.test {
            val state = awaitWhere { !it.isLoading }
            assertEquals(10, state.ayahsHeard)
            assertEquals(10f / QuranMeta.TOTAL_AYAHS, state.coverage, 0.0001f)
            assertEquals(0, state.rounds)
            assertEquals(3_900_000L, state.listenedMs)
            assertFalse(state.isEmpty)
            assertEquals(ListeningOrder.RECENT, state.order)
            assertEquals(listOf(114, 1), state.rows.map { it.surah })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a row carries the surah's names and the round math`() = runTest {
        // Al-Faatiha (7 ayahs): every ayah once and the first three once more.
        history.counts.value = counts(
            heard = allAyahsOf(1, times = 1) + firstAyahsOf(1, ayahs = 3, times = 2),
        )
        viewModel().uiState.test {
            val row = awaitWhere { !it.isLoading }.rows.single()
            assertEquals(1, row.surah)
            assertEquals("Al-Faatiha", row.nameEnglish)
            assertEquals("الفاتحة", row.nameArabicShort)
            assertEquals(1, row.rounds)
            assertEquals(7, row.ayahsHeard)
            assertEquals(7, row.ayahCount)
            assertEquals(3, row.ayahsIntoNextRound)
            assertEquals(3f / 7f, row.nextRoundProgress, 0.0001f)
            assertEquals(10, row.totalListens)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nothing heard is the empty state`() = runTest {
        viewModel().uiState.test {
            val state = awaitWhere { !it.isLoading }
            assertTrue(state.isEmpty)
            assertTrue(state.rows.isEmpty())
            assertEquals(0, state.ayahsHeard)
            assertEquals(0f, state.coverage, 0f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `each order sorts the rows its way`() = runTest {
        history.counts.value = counts(
            heard = allAyahsOf(1, times = 1) + firstAyahsOf(1, ayahs = 3, times = 2) +
                allAyahsOf(112, times = 3) + allAyahsOf(114, times = 1),
            lastHeardAt = mapOf(1 to 3_000L, 112 to 1_000L, 114 to 2_000L),
        )
        val viewModel = viewModel()
        viewModel.uiState.test {
            // Last heard first: Al-Faatiha, then An-Naas, then Al-Ikhlaas.
            assertEquals(listOf(1, 114, 112), awaitWhere { it.rows.size == 3 }.rows.map { it.surah })

            // Most listens first: 112 heard 12 times, 1 heard 10, 114 heard 6.
            viewModel.setOrder(ListeningOrder.MOST_HEARD)
            assertEquals(
                listOf(112, 1, 114),
                awaitWhere {
                    it.order == ListeningOrder.MOST_HEARD
                }.rows.map { it.surah },
            )

            viewModel.setOrder(ListeningOrder.BY_NUMBER)
            assertEquals(listOf(1, 112, 114), awaitWhere { it.order == ListeningOrder.BY_NUMBER }.rows.map { it.surah })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the order survives through the saved state handle`() = runTest {
        val handle = SavedStateHandle(mapOf("order" to ListeningOrder.BY_NUMBER))
        val viewModel = ProgressViewModel(history, quranText, handle)
        viewModel.uiState.test {
            assertEquals(ListeningOrder.BY_NUMBER, awaitWhere { !it.isLoading }.order)

            viewModel.setOrder(ListeningOrder.MOST_HEARD)
            assertEquals(ListeningOrder.MOST_HEARD, handle.get<ListeningOrder>("order"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset asks the history to forget everything`() = runTest {
        history.counts.value = counts(heard = allAyahsOf(1, times = 1))
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitWhere { !it.isEmpty }

            viewModel.reset()
            assertTrue(history.lastResetAt.value > 0)
            assertTrue(awaitWhere { it.isEmpty }.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failing text source still shows the rows, without names`() = runTest {
        quranText.failure = IllegalStateException("disk on fire")
        history.counts.value = counts(heard = allAyahsOf(1, times = 1))
        viewModel().uiState.test {
            val state = awaitWhere { !it.isLoading }
            assertFalse(state.isEmpty)
            assertNull(state.rows.single().nameEnglish)
            assertNull(state.rows.single().nameArabicShort)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** [ListeningCounts] from per-ayah counts keyed by global ayah number. */
    private fun counts(
        heard: Map<Int, Int> = emptyMap(),
        lastHeardAt: Map<Int, Long> = emptyMap(),
        listenedMs: Map<Int, Long> = emptyMap(),
    ) = ListeningCounts(
        ayahCounts = IntArray(QuranMeta.TOTAL_AYAHS).also { array ->
            heard.forEach { (globalAyah, times) -> array[globalAyah - 1] = times }
        },
        lastHeardAt = lastHeardAt,
        listenedMs = listenedMs,
    )

    /** Every ayah of [surah] heard [times] times. */
    private fun allAyahsOf(surah: Int, times: Int): Map<Int, Int> =
        (1..QuranMeta.ayahCount(surah)).associate { QuranMeta.globalAyah(surah, it) to times }

    /** The first [ayahs] of [surah] heard [times] times. */
    private fun firstAyahsOf(surah: Int, ayahs: Int, times: Int = 1): Map<Int, Int> =
        (1..ayahs).associate { QuranMeta.globalAyah(surah, it) to times }
}

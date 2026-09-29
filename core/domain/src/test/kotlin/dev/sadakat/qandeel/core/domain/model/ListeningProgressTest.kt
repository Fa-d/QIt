package dev.sadakat.qandeel.core.domain.model

import dev.sadakat.qandeel.core.domain.repository.ListeningCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningProgressTest {

    /** Counts with [heard] (global ayah to times), Al-Fatiha last heard at 100 and Al-Ikhlaas at 200. */
    private fun counts(vararg heard: Pair<Int, Int>) = ListeningCounts(
        ayahCounts = IntArray(QuranMeta.TOTAL_AYAHS).also { array -> heard.forEach { (g, n) -> array[g - 1] = n } },
        lastHeardAt = mapOf(1 to 100L, 112 to 200L),
        listenedMs = mapOf(1 to 60_000L, 112 to 30_000L),
    )

    // Al-Fatiha is global 1..7; Al-Ikhlaas 6222..6225.
    private val fatihaTwiceAndThreeMore = counts(
        1 to 3,
        2 to 3,
        3 to 3,
        4 to 2,
        5 to 2,
        6 to 2,
        7 to 2,
        6222 to 1,
    )

    @Test
    fun `rounds are the times every ayah was heard`() {
        val fatiha = ListeningProgress.surah(fatihaTwiceAndThreeMore, 1)
        assertEquals(2, fatiha.rounds)
        assertEquals(3, fatiha.ayahsIntoNextRound)
        assertEquals(3f / 7, fatiha.nextRoundProgress, 0.0001f)
        assertEquals(7, fatiha.ayahsHeard)
        assertEquals(17, fatiha.totalListens)
        assertEquals(100L, fatiha.lastHeardAt)
        assertEquals(60_000L, fatiha.listenedMs)
        assertTrue(fatiha.isHeard)
    }

    @Test
    fun `a surah partly heard has no round yet`() {
        val ikhlaas = ListeningProgress.surah(fatihaTwiceAndThreeMore, 112)
        assertEquals(0, ikhlaas.rounds)
        assertEquals(1, ikhlaas.ayahsIntoNextRound)
        assertEquals(1, ikhlaas.ayahsHeard)
    }

    @Test
    fun `a surah never heard`() {
        val baqarah = ListeningProgress.surah(fatihaTwiceAndThreeMore, 2)
        assertFalse(baqarah.isHeard)
        assertEquals(null, baqarah.lastHeardAt)
        assertEquals(0L, baqarah.listenedMs)
        assertEquals(0f, baqarah.nextRoundProgress)
    }

    @Test
    fun `the whole quran lists heard surahs only`() {
        val quran = ListeningProgress.quran(fatihaTwiceAndThreeMore)
        assertEquals(8, quran.ayahsHeard)
        assertEquals(0, quran.rounds)
        assertEquals(90_000L, quran.listenedMs)
        assertEquals(listOf(1, 112), quran.surahs.map { it.surah })
        assertEquals(8f / 6236, quran.coverage, 0.00001f)
    }

    @Test
    fun `the quran is a round once every ayah is heard`() {
        val everything = ListeningCounts(IntArray(QuranMeta.TOTAL_AYAHS) { 1 })
        assertEquals(1, ListeningProgress.quran(everything).rounds)
        assertEquals(1f, ListeningProgress.quran(everything).coverage)
    }

    @Test
    fun `orders by recency, listens or number`() {
        val surahs = ListeningProgress.quran(fatihaTwiceAndThreeMore).surahs
        assertEquals(listOf(112, 1), ListeningProgress.sorted(surahs, ListeningOrder.RECENT).map { it.surah })
        assertEquals(listOf(1, 112), ListeningProgress.sorted(surahs, ListeningOrder.MOST_HEARD).map { it.surah })
        assertEquals(
            listOf(1, 112),
            ListeningProgress.sorted(surahs.reversed(), ListeningOrder.BY_NUMBER).map {
                it.surah
            },
        )
    }

    @Test
    fun `ties fall back to the surah number`() {
        // 2:1 (global 8) and 112:1 (global 6222), heard once each at no recorded time.
        val heard = IntArray(QuranMeta.TOTAL_AYAHS).also {
            it[7] = 1
            it[6221] = 1
        }
        val surahs = ListeningProgress.quran(ListeningCounts(heard)).surahs.reversed()
        assertEquals(listOf(2, 112), ListeningProgress.sorted(surahs, ListeningOrder.MOST_HEARD).map { it.surah })
        assertEquals(listOf(2, 112), ListeningProgress.sorted(surahs, ListeningOrder.RECENT).map { it.surah })
    }

    @Test
    fun `counts compare by content`() {
        assertEquals(counts(5 to 1), counts(5 to 1))
        assertEquals(counts(5 to 1).hashCode(), counts(5 to 1).hashCode())
        assertNotEquals(counts(5 to 1), counts(5 to 2))
        assertNotEquals(counts(5 to 1), ListeningCounts(IntArray(QuranMeta.TOTAL_AYAHS).also { it[4] = 1 }))
        assertNotEquals(ListeningCounts.EMPTY, "empty")
        assertEquals(0, ListeningCounts.EMPTY.count(6236))
        assertEquals(1, counts(5 to 1).count(1, 5))
        assertEquals("ListeningCounts(heard=1 ayahs, surahs=[1, 112])", counts(5 to 1).toString())
    }
}

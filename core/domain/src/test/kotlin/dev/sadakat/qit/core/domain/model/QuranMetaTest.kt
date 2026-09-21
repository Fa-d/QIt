package dev.sadakat.qit.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranMetaTest {

    @Test
    fun `surah ayah counts sum to the total number of ayahs`() {
        val total = (1..QuranMeta.SURAH_COUNT).sumOf { QuranMeta.ayahCount(it) }
        assertEquals(QuranMeta.TOTAL_AYAHS, total)
    }

    @Test
    fun `global ayah numbering maps surah and ayah onto 1 to 6236`() {
        assertEquals(1, QuranMeta.globalAyah(1, 1))
        assertEquals(8, QuranMeta.globalAyah(2, 1))
        assertEquals(262, QuranMeta.globalAyah(2, 255))
        assertEquals(6236, QuranMeta.globalAyah(114, 6))
    }

    @Test
    fun `every surah starts right after the previous one ends`() {
        var expectedFirst = 1
        for (surah in 1..QuranMeta.SURAH_COUNT) {
            assertEquals(expectedFirst, QuranMeta.globalAyah(surah, 1))
            expectedFirst += QuranMeta.ayahCount(surah)
        }
        assertEquals(QuranMeta.TOTAL_AYAHS + 1, expectedFirst)
    }

    @Test
    fun `invalid surah or ayah throws`() {
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.ayahCount(0) }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.ayahCount(QuranMeta.SURAH_COUNT + 1) }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.globalAyah(0, 1) }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.globalAyah(QuranMeta.SURAH_COUNT + 1, 1) }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.globalAyah(1, 0) }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.globalAyah(1, 8) }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.globalAyah(2, 287) }
    }

    @Test
    fun `only Al-Fatiha and At-Tawbah lack a basmala prefix`() {
        assertFalse(QuranMeta.hasBasmalaPrefix(1))
        assertFalse(QuranMeta.hasBasmalaPrefix(9))
        assertTrue(QuranMeta.hasBasmalaPrefix(2))
        assertEquals(
            QuranMeta.SURAH_COUNT - 2,
            (1..QuranMeta.SURAH_COUNT).count { QuranMeta.hasBasmalaPrefix(it) },
        )
    }
}

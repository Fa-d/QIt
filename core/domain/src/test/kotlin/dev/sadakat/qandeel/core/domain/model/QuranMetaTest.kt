package dev.sadakat.qandeel.core.domain.model

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

    @Test
    fun `the thirty juz start where printed mushafs start them`() {
        assertEquals(AyahRef(1, 1), QuranMeta.juzStart(1))
        assertEquals(AyahRef(2, 142), QuranMeta.juzStart(2))
        assertEquals(AyahRef(15, 1), QuranMeta.juzStart(14))
        assertEquals(AyahRef(67, 1), QuranMeta.juzStart(29))
        assertEquals(AyahRef(78, 1), QuranMeta.juzStart(30))
    }

    @Test
    fun `juz starts run strictly forward through the Quran`() {
        val globals = (1..QuranMeta.JUZ_COUNT).map {
            QuranMeta.juzStart(it).let { ref -> QuranMeta.globalAyah(ref.surah, ref.ayah) }
        }
        assertEquals(globals.sorted().distinct(), globals)
    }

    @Test
    fun `an ayah belongs to the last juz starting at or before it`() {
        assertEquals(1, QuranMeta.juzOf(1, 1))
        assertEquals(1, QuranMeta.juzOf(2, 141))
        assertEquals(2, QuranMeta.juzOf(2, 142))
        assertEquals(3, QuranMeta.juzOf(2, 255))
        assertEquals(30, QuranMeta.juzOf(114, 6))
        assertEquals(30, QuranMeta.juzOf(78, 1))
        assertEquals(29, QuranMeta.juzOf(77, 50))
    }

    @Test
    fun `a surah's basmala belongs to the juz of its first ayah`() {
        assertEquals(QuranMeta.juzOf(18, 1), QuranMeta.juzOf(18, 0))
    }

    @Test
    fun `an invalid juz throws`() {
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.juzStart(0) }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.juzStart(31) }
    }

    @Test
    fun `ayahRef is the inverse of globalAyah`() {
        assertEquals(AyahRef(1, 1), QuranMeta.ayahRef(1))
        assertEquals(AyahRef(1, 7), QuranMeta.ayahRef(7))
        assertEquals(AyahRef(2, 1), QuranMeta.ayahRef(8))
        assertEquals(AyahRef(2, 255), QuranMeta.ayahRef(262))
        assertEquals(AyahRef(114, 6), QuranMeta.ayahRef(QuranMeta.TOTAL_AYAHS))
        for (surah in 1..QuranMeta.SURAH_COUNT) {
            for (ayah in 1..QuranMeta.ayahCount(surah)) {
                assertEquals(AyahRef(surah, ayah), QuranMeta.ayahRef(QuranMeta.globalAyah(surah, ayah)))
            }
        }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.ayahRef(0) }
        assertThrows(IllegalArgumentException::class.java) { QuranMeta.ayahRef(QuranMeta.TOTAL_AYAHS + 1) }
    }
}

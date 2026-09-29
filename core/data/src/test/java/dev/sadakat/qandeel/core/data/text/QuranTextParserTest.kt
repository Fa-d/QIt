package dev.sadakat.qandeel.core.data.text

import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.model.Revelation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * Verifies the parser against the real generated assets (Gradle runs unit tests with the module
 * directory as the working dir, so the files are read straight from `src/main/assets`).
 */
class QuranTextParserTest {

    private fun asset(path: String): String = File("src/main/assets/quran", path).readText()

    private fun fileName(surah: Int): String = String.format(Locale.ROOT, "%03d", surah)

    @Test
    fun `surahs parses all 114 surahs in order with the ayah counts of QuranMeta`() {
        val surahs = QuranTextParser.parseSurahs(asset("surahs.json"))

        assertEquals(QuranMeta.SURAH_COUNT, surahs.size)
        surahs.forEachIndexed { index, surah ->
            assertEquals("surah at index $index", index + 1, surah.number)
            assertEquals("ayahCount of surah ${surah.number}", QuranMeta.ayahCount(surah.number), surah.ayahCount)
        }
    }

    @Test
    fun `revelation of surah 1 is MECCAN and of surahs 2 and 9 is MEDINAN`() {
        val surahs = QuranTextParser.parseSurahs(asset("surahs.json"))

        assertEquals(Revelation.MECCAN, surahs[0].revelation)
        assertEquals(Revelation.MEDINAN, surahs[1].revelation)
        assertEquals(Revelation.MEDINAN, surahs[8].revelation)
    }

    @Test
    fun `parsing all 114 text files yields 6236 ayahs numbered as QuranMeta`() {
        var total = 0
        for (surah in 1..QuranMeta.SURAH_COUNT) {
            val ayahs = QuranTextParser.parseAyahs(surah, asset("text/${fileName(surah)}.json"))

            assertEquals("ayah count of surah $surah", QuranMeta.ayahCount(surah), ayahs.size)
            ayahs.forEachIndexed { index, ayah ->
                assertEquals("surah of $surah:${index + 1}", surah, ayah.surah)
                assertEquals("number at index $index of surah $surah", index + 1, ayah.number)
                assertEquals(
                    "global number of $surah:${ayah.number}",
                    QuranMeta.globalAyah(surah, ayah.number),
                    ayah.globalNumber,
                )
            }
            total += ayahs.size
        }
        assertEquals(QuranMeta.TOTAL_AYAHS, total)
    }

    @Test
    fun `surah 1 verse 1 starts with the basmala but surah 2 verse 1 does not`() {
        val faatiha = QuranTextParser.parseAyahs(1, asset("text/001.json"))
        val baqara = QuranTextParser.parseAyahs(2, asset("text/002.json"))

        assertTrue(faatiha[0].arabic.startsWith("بِسْمِ"))
        assertFalse(baqara[0].arabic.startsWith("بِسْمِ"))
    }

    @Test
    fun `no ayah has blank text in any language`() {
        for (surah in 1..QuranMeta.SURAH_COUNT) {
            QuranTextParser.parseAyahs(surah, asset("text/${fileName(surah)}.json")).forEach { ayah ->
                assertTrue("blank Arabic at $surah:${ayah.number}", ayah.arabic.isNotBlank())
                assertTrue("blank English at $surah:${ayah.number}", ayah.english.isNotBlank())
                assertTrue("blank Bangla at $surah:${ayah.number}", ayah.bangla.isNotBlank())
            }
        }
    }

    @Test
    fun `malformed JSON is rejected with IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            QuranTextParser.parseSurahs("not json")
        }
        assertThrows(IllegalArgumentException::class.java) {
            QuranTextParser.parseAyahs(2, "[{\"n\":1")
        }
    }
}

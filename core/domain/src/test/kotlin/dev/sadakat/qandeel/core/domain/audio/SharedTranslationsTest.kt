package dev.sadakat.qandeel.core.domain.audio

import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedTranslationsTest {

    @Test
    fun `ranges parse to the verses they cover`() {
        assertEquals(setOf(3, 4, 5, 9), SharedTranslations.parse("3-5,9"))
        assertEquals(setOf(7), SharedTranslations.parse(" 7 "))
        assertEquals(emptySet<Int>(), SharedTranslations.parse(""))
        assertThrows(IllegalArgumentException::class.java) { SharedTranslations.parse("5-3") }
    }

    @Test
    fun `tracks that read every verse apart have every file`() {
        for (track in listOf(
            Track.ARABIC,
            Track.ENGLISH,
            Track.BANGLA,
            Track.ARABIC_BASIT_MUJAWWAD,
            Track.ARABIC_SUDAIS,
        )) {
            assertTrue((1..QuranMeta.TOTAL_AYAHS).all { QuranAudioUrls.hasOwnFile(track, it) })
        }
    }

    @Test
    fun `the generated table only names ayahs of voices with shared files`() {
        for ((track, ranges) in SHARED_TRANSLATIONS) {
            assertTrue(track in setOf(Track.BANGLA_TOHA, Track.BANGLA_BAEZEED))
            assertTrue(SharedTranslations.parse(ranges).all { it in 1 until QuranMeta.TOTAL_AYAHS })
        }
    }

    @Test
    fun `a shared verse has no file or queue entry, and a surah's last verse is never shared`() {
        for (track in SHARED_TRANSLATIONS.keys) {
            for (surah in 1..QuranMeta.SURAH_COUNT) {
                val last = QuranMeta.globalAyah(surah, QuranMeta.ayahCount(surah))
                assertTrue(QuranAudioUrls.hasOwnFile(track, last))
                val files = QuranAudioUrls.surahFiles(surah, track).map { it.id }.toSet()
                for (ayah in 1..QuranMeta.ayahCount(surah)) {
                    val global = QuranMeta.globalAyah(surah, ayah)
                    assertEquals(QuranAudioUrls.hasOwnFile(track, global), "${track.code}/$global" in files)
                }
            }
        }
    }
}

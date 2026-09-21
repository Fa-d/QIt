package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.Track
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale
import dev.sadakat.qit.core.domain.audio.QuranAudioUrls.AudioFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class QuranAudioUrlsTest {

    private companion object {
        const val BANGLA = "${QuranAudioUrls.HF_DATASET}/bangla/bangla-translation-verses"
    }

    @Test
    fun `arabic and english verses come from the islamic network cdn`() {
        assertEquals(
            AudioFile("ar/255", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/255.mp3"),
            QuranAudioUrls.verse(Track.ARABIC, 255),
        )
        assertEquals(
            AudioFile("en/255", "https://cdn.islamic.network/quran/audio/192/en.walk/255.mp3"),
            QuranAudioUrls.verse(Track.ENGLISH, 255),
        )
    }

    @Test
    fun `bangla verses and intros come from the hugging face dataset mirroring quran_audio`() {
        val base = "https://huggingface.co/datasets/faddy001/quran_audio/resolve/main/bangla/bangla-translation-verses"
        assertEquals(AudioFile("bn/1", "$base/00001.mp3"), QuranAudioUrls.verse(Track.BANGLA, 1))
        assertEquals("$base/06236.mp3", QuranAudioUrls.verse(Track.BANGLA, QuranMeta.TOTAL_AYAHS).url)
        assertEquals(AudioFile("bn/intro/114", "$base/intro/114.mp3"), QuranAudioUrls.basmala(Track.BANGLA, 114))
    }

    @Test
    fun `file formatting ignores the device locale`() {
        // A Bangla-locale device would otherwise format Bengali digits into the URL.
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("bn", "BD"))
            assertEquals(
                "$BANGLA/01234.mp3",
                QuranAudioUrls.verse(Track.BANGLA, 1234).url,
            )
            assertEquals(
                "$BANGLA/intro/002.mp3",
                QuranAudioUrls.basmala(Track.BANGLA, 2)!!.url,
            )
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `an invalid global ayah throws`() {
        assertThrows(IllegalArgumentException::class.java) { QuranAudioUrls.verse(Track.ARABIC, 0) }
        assertThrows(IllegalArgumentException::class.java) {
            QuranAudioUrls.verse(Track.ARABIC, QuranMeta.TOTAL_AYAHS + 1)
        }
    }

    @Test
    fun `surahs 1 and 9 have no basmala prefix`() {
        for (track in Track.entries) {
            assertNull(QuranAudioUrls.basmala(track, 1))
            assertNull(QuranAudioUrls.basmala(track, 9))
        }
    }

    @Test
    fun `arabic and english reuse verse 1 as the basmala, bangla has one intro per surah`() {
        assertEquals(QuranAudioUrls.verse(Track.ARABIC, 1), QuranAudioUrls.basmala(Track.ARABIC, 2))
        assertEquals(QuranAudioUrls.verse(Track.ENGLISH, 1), QuranAudioUrls.basmala(Track.ENGLISH, 114))
        assertEquals(
            AudioFile("bn/intro/2", "$BANGLA/intro/002.mp3"),
            QuranAudioUrls.basmala(Track.BANGLA, 2),
        )
    }

    @Test
    fun `surah files are the basmala plus one file per ayah`() {
        assertEquals(7, QuranAudioUrls.surahFiles(1, Track.ARABIC).size)
        // 286 ayahs + the shared basmala.
        assertEquals(287, QuranAudioUrls.surahFiles(2, Track.ARABIC).size)
        assertEquals(129, QuranAudioUrls.surahFiles(9, Track.BANGLA).size)
        // Surah 1 is its own basmala, so nothing is added.
        assertEquals(
            QuranAudioUrls.verse(Track.ARABIC, 1),
            QuranAudioUrls.surahFiles(1, Track.ARABIC).first(),
        )
        assertEquals(
            QuranAudioUrls.basmala(Track.ARABIC, 2),
            QuranAudioUrls.surahFiles(2, Track.ARABIC).first(),
        )
        assertEquals(
            QuranAudioUrls.verse(Track.ARABIC, QuranMeta.globalAyah(2, 286)),
            QuranAudioUrls.surahFiles(2, Track.ARABIC).last(),
        )
    }

    @Test
    fun `file ids are unique`() {
        for (track in Track.entries) {
            val verseIds = (1..QuranMeta.TOTAL_AYAHS).map { QuranAudioUrls.verse(track, it).id }
            assertEquals(QuranMeta.TOTAL_AYAHS, verseIds.distinct().size)
        }
        // Arabic/English reuse verse 1 as every basmala, so a track has exactly one id per verse;
        // Bangla has a distinct intro file per surah besides the 6236 verse files.
        for (track in listOf(Track.ARABIC, Track.ENGLISH)) {
            val ids = (1..QuranMeta.SURAH_COUNT)
                .flatMap { QuranAudioUrls.surahFiles(it, track) }
                .map { it.id }
            assertEquals(QuranMeta.TOTAL_AYAHS, ids.distinct().size)
        }
        val banglaIds = (1..QuranMeta.SURAH_COUNT)
            .flatMap { QuranAudioUrls.surahFiles(it, Track.BANGLA) }
            .map { it.id }
        assertEquals(QuranMeta.TOTAL_AYAHS + QuranMeta.SURAH_COUNT - 2, banglaIds.distinct().size)
    }
}

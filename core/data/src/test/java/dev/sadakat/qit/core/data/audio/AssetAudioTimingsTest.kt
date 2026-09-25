package dev.sadakat.qit.core.data.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.data.text.AssetQuranText
import dev.sadakat.qit.core.domain.audio.QueuePlan
import dev.sadakat.qit.core.domain.audio.WordTimings
import dev.sadakat.qit.core.domain.model.ArabicWords
import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The bundled timing assets against the bundled text and the queues the player builds. */
@RunWith(AndroidJUnit4::class)
class AssetAudioTimingsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val timings = AssetAudioTimings(context)
    private val text = AssetQuranText(context)

    @Test
    fun `every ayah has one timing per word of its text`() = runTest {
        for (surah in 1..QuranMeta.SURAH_COUNT) {
            val words = timings.wordTimings(surah, Track.ARABIC)
            for (ayah in text.ayahs(surah)) {
                assertEquals(
                    "${ayah.surah}:${ayah.number}",
                    ArabicWords.count(ayah.arabic),
                    words[ayah.number]?.wordCount,
                )
            }
        }
    }

    @Test
    fun `the basmala before verse 1 has 1_1's timings`() = runTest {
        val fatiha = timings.wordTimings(1, Track.ARABIC)
        assertEquals(fatiha[1], timings.wordTimings(2, Track.ARABIC)[0])
        assertNull(fatiha[0]) // Al-Fatiha opens with verse 1 itself
        assertNull(timings.wordTimings(9, Track.ARABIC)[0]) // At-Tawbah has no basmala
        assertEquals(4, fatiha.getValue(1).wordCount)
    }

    @Test
    fun `ayat al-kursi's words run in order within its file`() = runTest {
        val kursi = timings.wordTimings(2, Track.ARABIC).getValue(255)
        assertEquals(50, kursi.wordCount)
        assertEquals(0, kursi.wordAt(40))
        assertEquals(49, kursi.wordAt(51_000))
        assertTrue((1 until kursi.wordCount).all { kursi.startMs(it) >= kursi.startMs(it - 1) })
        assertTrue(kursi.endMs(49) <= timings.durationMs("ar/262")!!)
    }

    @Test
    fun `every reciter a voice plays has word timings for every ayah, a translation has none`() = runTest {
        for (reciter in BanglaVoice.entries.map { it.arabic }) {
            val words = timings.wordTimings(2, reciter)
            assertEquals("$reciter", (0..QuranMeta.ayahCount(2)).toSet(), words.keys)
        }
        assertEquals(emptyMap<Int, WordTimings>(), timings.wordTimings(2, Track.BANGLA_TOHA))
    }

    @Test
    fun `each reciter has timings of its own`() = runTest {
        // 2:255 lasts a different time in each recording: the timings can't be one reciter's copied.
        val ends = BanglaVoice.entries.map { voice ->
            val words = timings.wordTimings(2, voice.arabic).getValue(255)
            words.startMs(words.wordCount - 1)
        }
        assertEquals(ends.size, ends.distinct().size)
    }

    @Test
    fun `every file of every voice's queues has a length`() = runTest {
        for (voice in BanglaVoice.entries) {
            for (surah in 1..QuranMeta.SURAH_COUNT) {
                for (entry in QueuePlan.plan(surah, RecitationMode.ARABIC_BANGLA, voice)) {
                    assertNotNull("${entry.file.id} of $surah read by $voice", timings.durationMs(entry.file.id))
                }
            }
        }
    }

    @Test
    fun `every file of the default voice's queues has a length`() = runTest {
        for (surah in 1..QuranMeta.SURAH_COUNT) {
            for (mode in RecitationMode.entries) {
                for (entry in QueuePlan.plan(surah, mode, BanglaVoice.DEFAULT)) {
                    assertNotNull("${entry.file.id} of $surah in $mode", timings.durationMs(entry.file.id))
                }
            }
        }
    }

    @Test
    fun `lengths are those of the files`() = runTest {
        assertEquals(51_918L, timings.durationMs("ar/262"))
        assertEquals(40_934L, timings.durationMs("en/262"))
        assertEquals(38_456L, timings.durationMs("bn/262"))
        assertEquals(10_107L, timings.durationMs("bn/intro/2"))
    }

    @Test
    fun `unknown files have no length`() = runTest {
        assertNull(timings.durationMs("bn/intro/1")) // Al-Fatiha has no intro
        assertNull(timings.durationMs("ar/0"))
        assertNull(timings.durationMs("ar/6237"))
        assertNull(timings.durationMs("xx/1"))
        assertNull(timings.durationMs("ar/one"))
        assertNull(timings.durationMs("ar"))
    }
}

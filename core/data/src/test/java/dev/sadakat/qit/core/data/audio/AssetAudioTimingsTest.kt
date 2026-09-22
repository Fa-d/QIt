package dev.sadakat.qit.core.data.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.data.text.AssetQuranText
import dev.sadakat.qit.core.domain.audio.QueuePlan
import dev.sadakat.qit.core.domain.model.ArabicWords
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
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
            val words = timings.wordTimings(surah)
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
        val fatiha = timings.wordTimings(1)
        assertEquals(fatiha[1], timings.wordTimings(2)[0])
        assertNull(fatiha[0]) // Al-Fatiha opens with verse 1 itself
        assertNull(timings.wordTimings(9)[0]) // At-Tawbah has no basmala
        assertEquals(4, fatiha.getValue(1).wordCount)
    }

    @Test
    fun `ayat al-kursi's words run in order within its file`() = runTest {
        val kursi = timings.wordTimings(2).getValue(255)
        assertEquals(50, kursi.wordCount)
        assertEquals(0, kursi.wordAt(40))
        assertEquals(49, kursi.wordAt(51_000))
        assertTrue((1 until kursi.wordCount).all { kursi.startMs(it) >= kursi.startMs(it - 1) })
        assertTrue(kursi.endMs(49) <= timings.durationMs("ar/262")!!)
    }

    @Test
    fun `every file of every queue has a length`() = runTest {
        for (surah in 1..QuranMeta.SURAH_COUNT) {
            for (mode in RecitationMode.entries) {
                for (entry in QueuePlan.plan(surah, mode)) {
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

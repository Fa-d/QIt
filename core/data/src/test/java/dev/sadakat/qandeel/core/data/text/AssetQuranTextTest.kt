package dev.sadakat.qandeel.core.data.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetQuranTextTest {

    private val quranText = AssetQuranText(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `surahs returns all 114 surahs`() = runTest {
        assertEquals(QuranMeta.SURAH_COUNT, quranText.surahs().size)
    }

    @Test
    fun `surah 112 is Al-Ikhlaas`() = runTest {
        assertEquals("Al-Ikhlaas", quranText.surah(112).nameEnglish)
    }

    @Test
    fun `ayahs of surah 2 has 286 ayahs and Ayat al-Kursi has global number 262`() = runTest {
        val ayahs = quranText.ayahs(2)

        assertEquals(QuranMeta.ayahCount(2), ayahs.size)
        assertEquals(262, ayahs[254].globalNumber)
    }

    @Test
    fun `repeated calls return equal results`() = runTest {
        assertEquals(quranText.surahs(), quranText.surahs())
        assertEquals(quranText.ayahs(2), quranText.ayahs(2))
    }

    @Test
    fun `surah numbers outside 1 to 114 are rejected`() = runTest {
        assertRejected { quranText.surah(0) }
        assertRejected { quranText.surah(115) }
    }

    /** assertThrows takes a non-suspend lambda, so the failure is asserted by hand. */
    private suspend fun assertRejected(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }
}

package dev.sadakat.qit.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {

    private val ayah = Ayah(surah = 2, number = 255, globalNumber = 262, arabic = "ar", english = "en", bangla = "bn")

    @Test
    fun `an ayah's translation follows the track`() {
        assertEquals("en", ayah.translation(Track.ENGLISH))
        assertEquals("bn", ayah.translation(Track.BANGLA))
        assertNull(ayah.translation(Track.ARABIC))
    }

    @Test
    fun `a surah numbers its ayahs globally`() {
        val surah = Surah(2, "البقرة", "Al-Baqara", "The Cow", 286, Revelation.MEDINAN)
        assertEquals(262, surah.globalAyah(255))
    }

    @Test
    fun `tracks round-trip through their codes`() {
        Track.entries.forEach { assertEquals(it, Track.fromCode(it.code)) }
        assertNull(Track.fromCode("xx"))
    }

    @Test
    fun `a mode's translation is its non-Arabic track`() {
        assertNull(RecitationMode.ARABIC_ONLY.translation)
        assertEquals(Track.ENGLISH, RecitationMode.ARABIC_ENGLISH.translation)
        assertEquals(Track.BANGLA, RecitationMode.ARABIC_BANGLA.translation)
    }

    @Test
    fun `reading prefs default to the comfortable middle`() {
        val prefs = ReadingPrefs()
        assertEquals(ArabicTextSize.MEDIUM, prefs.arabicTextSize)
        assertEquals(1f, prefs.arabicTextSize.scale)
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
    }
}

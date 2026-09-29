package dev.sadakat.qandeel.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `the short Arabic name drops the leading word surah`() {
        val fatiha = Surah(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", 7, Revelation.MECCAN)
        assertEquals("ٱلْفَاتِحَةِ", fatiha.nameArabicShort)
        assertEquals("البقرة", Surah(2, "البقرة", "Al-Baqara", "The Cow", 286, Revelation.MEDINAN).nameArabicShort)
    }

    @Test
    fun `tracks round-trip through their codes`() {
        Track.entries.forEach { assertEquals(it, Track.fromCode(it.code)) }
        assertNull(Track.fromCode("xx"))
    }

    @Test
    fun `bangla voices round-trip through their codes`() {
        BanglaVoice.entries.forEach { assertEquals(it, BanglaVoice.fromCode(it.code)) }
        assertNull(BanglaVoice.fromCode("xx"))
    }

    @Test
    fun `a mode's translation is the language shown, whatever the voice`() {
        assertNull(RecitationMode.ARABIC_ONLY.translation)
        assertEquals(Track.ENGLISH, RecitationMode.ARABIC_ENGLISH.translation)
        assertEquals(Track.BANGLA, RecitationMode.ARABIC_BANGLA.translation)
    }

    @Test
    fun `arabic and bangla plays the voice's own arabic, then the voice`() {
        for (voice in BanglaVoice.entries) {
            assertEquals(listOf(voice.arabic, voice.bangla), RecitationMode.ARABIC_BANGLA.tracks(voice))
            assertTrue(voice.arabic.isArabic)
            assertEquals(Language.BANGLA, voice.bangla.language)
            assertEquals(listOf(Track.ARABIC), RecitationMode.ARABIC_ONLY.tracks(voice))
            assertEquals(listOf(Track.ARABIC, Track.ENGLISH), RecitationMode.ARABIC_ENGLISH.tracks(voice))
        }
        assertEquals(listOf(Track.ARABIC, Track.BANGLA), RecitationMode.ARABIC_BANGLA.tracks(BanglaVoice.DEFAULT))
    }

    @Test
    fun `every bangla voice shows the bangla text`() {
        val ayah = Ayah(1, 1, 1, "ar", "en", "bn")
        for (track in Track.entries) {
            val expected = when (track.language) {
                Language.ARABIC -> null
                Language.ENGLISH -> "en"
                Language.BANGLA -> "bn"
            }
            assertEquals(expected, ayah.translation(track))
        }
    }

    @Test
    fun `reading prefs default to the comfortable middle`() {
        val prefs = ReadingPrefs()
        assertEquals(ArabicTextSize.MEDIUM, prefs.arabicTextSize)
        assertEquals(1f, prefs.arabicTextSize.scale)
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertEquals(UiStyle.MUSHAF, prefs.uiStyle)
    }
}

package dev.sadakat.qit.presentation.home

import dev.sadakat.qit.core.domain.model.Revelation
import dev.sadakat.qit.core.domain.model.Surah
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses the real names from the bundled text (transliterations and fully vowelled Arabic). */
class SurahSearchTest {

    private fun surah(number: Int, english: String, meaning: String, arabic: String) =
        Surah(number, arabic, english, meaning, ayahCount = 7, revelation = Revelation.MECCAN)

    private val fatiha = surah(1, "Al-Faatiha", "The Opening", "سُورَةُ ٱلْفَاتِحَةِ")
    private val baqara = surah(2, "Al-Baqara", "The Cow", "سُورَةُ البَقَرَةِ")
    private val yaseen = surah(36, "Yaseen", "Yaseen", "سُورَةُ يسٓ")
    private val rahman = surah(55, "Ar-Rahmaan", "The Beneficent", "سُورَةُ الرَّحۡمَٰنِ")
    private val ikhlaas = surah(112, "Al-Ikhlaas", "Sincerity", "سُورَةُ الإِخۡلَاصِ")
    private val naas = surah(114, "An-Naas", "Mankind", "سُورَةُ النَّاسِ")

    @Test
    fun `an empty query matches everything`() {
        assertTrue(SurahSearch.matches(fatiha, "  "))
    }

    @Test
    fun `a number matches only that surah`() {
        assertTrue(SurahSearch.matches(ikhlaas, "112"))
        assertFalse(SurahSearch.matches(naas, "112"))
    }

    @Test
    fun `common transliteration spellings find the surah`() {
        assertTrue(SurahSearch.matches(ikhlaas, "Ikhlas"))
        assertTrue(SurahSearch.matches(yaseen, "yasin"))
        assertTrue(SurahSearch.matches(rahman, "rahman"))
        assertTrue(SurahSearch.matches(fatiha, "Fatihah"))
        assertTrue(SurahSearch.matches(baqara, "al baqarah"))
        assertTrue(SurahSearch.matches(naas, "nas"))
    }

    @Test
    fun `the meaning matches case-insensitively`() {
        assertTrue(SurahSearch.matches(baqara, "cow"))
        assertTrue(SurahSearch.matches(naas, "MANKIND"))
    }

    @Test
    fun `arabic typed without diacritics or alef variants finds the vowelled name`() {
        assertTrue(SurahSearch.matches(fatiha, "الفاتحة"))
        assertTrue(SurahSearch.matches(ikhlaas, "الاخلاص"))
        assertTrue(SurahSearch.matches(baqara, "البقره"))
    }

    @Test
    fun `unrelated text matches nothing`() {
        assertFalse(SurahSearch.matches(fatiha, "zzz"))
        assertFalse(SurahSearch.matches(baqara, "الفاتحة"))
    }
}

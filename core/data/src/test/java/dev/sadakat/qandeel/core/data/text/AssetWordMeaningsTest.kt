package dev.sadakat.qandeel.core.data.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.domain.model.ArabicWords
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.model.WordByWord
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The bundled word meaning assets against the bundled text, in both languages. */
@RunWith(AndroidJUnit4::class)
class AssetWordMeaningsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val meanings = AssetWordMeanings(context)
    private val text = AssetQuranText(context)

    @Test
    fun `every ayah has one meaning per word of its text in both languages`() = runTest {
        for (surah in 1..QuranMeta.SURAH_COUNT) {
            for (language in listOf(WordByWord.ENGLISH, WordByWord.BANGLA)) {
                val words = meanings.meanings(surah, language)
                for (ayah in text.ayahs(surah)) {
                    assertEquals(
                        "${ayah.surah}:${ayah.number} in $language",
                        ArabicWords.count(ayah.arabic),
                        words[ayah.number]?.size,
                    )
                    words[ayah.number]?.forEach { meaning ->
                        assertTrue(
                            "${ayah.surah}:${ayah.number} in $language: '$meaning' is blank",
                            meaning.isNotBlank(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `the basmala before verse 1 has 1_1's meanings`() = runTest {
        for (language in listOf(WordByWord.ENGLISH, WordByWord.BANGLA)) {
            val fatiha = meanings.meanings(1, language)
            assertEquals(fatiha[1], meanings.meanings(2, language)[0])
            assertEquals(4, fatiha.getValue(1).size)
        }
        assertNull(meanings.meanings(1, WordByWord.ENGLISH)[0]) // Al-Fatiha opens with verse 1 itself
        assertNull(meanings.meanings(9, WordByWord.ENGLISH)[0]) // At-Tawbah has no basmala
    }

    @Test
    fun `off has no meanings`() = runTest {
        assertTrue(meanings.meanings(2, WordByWord.OFF).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an invalid surah is rejected`() = runTest {
        meanings.meanings(0, WordByWord.ENGLISH)
    }

    @Test
    fun `ayat al-kursi starts with Allah in bangla`() = runTest {
        assertEquals("আল্লাহ", meanings.meanings(2, WordByWord.BANGLA).getValue(255).first())
    }

    @Test
    fun `the basmala has four english meanings`() = runTest {
        assertEquals(
            listOf("In (the) name", "(of) Allah", "the Most Gracious", "the Most Merciful"),
            meanings.meanings(1, WordByWord.ENGLISH).getValue(1),
        )
    }
}

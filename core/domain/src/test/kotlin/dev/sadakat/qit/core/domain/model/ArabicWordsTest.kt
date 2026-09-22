package dev.sadakat.qit.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicWordsTest {

    private fun words(text: String) = ArabicWords.ranges(text).map { text.substring(it) }

    @Test
    fun `splits on whitespace`() {
        assertEquals(listOf("قُلْ", "هُوَ", "ٱللَّهُ", "أَحَدٌ"), words("قُلْ هُوَ ٱللَّهُ أَحَدٌ"))
    }

    @Test
    fun `a standalone pause mark belongs to the word before it`() {
        // 2:2: "لَا رَيْبَ ۛ فِيهِ ۛ هُدًى"
        val text = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ"
        assertEquals(7, ArabicWords.count(text))
        assertEquals("رَيْبَ ۛ", words(text)[3])
        assertEquals("فِيهِ ۛ", words(text)[4])
    }

    @Test
    fun `every pause mark is recognised`() {
        for (mark in listOf("ۖ", "ۗ", "ۘ", "ۙ", "ۚ", "ۛ", "ۜ", "۞", "۩")) {
            assertEquals(mark, 1, ArabicWords.count("كَلِمَةٌ $mark"))
        }
    }

    @Test
    fun `a pause mark opening the ayah belongs to the first word`() {
        // Many ayahs open with the rub el hizb (۞), e.g. 2:75.
        assertEquals(listOf("۞ أَفَتَطْمَعُونَ", "أَن"), words("۞ أَفَتَطْمَعُونَ أَن"))
    }

    @Test
    fun `repeated whitespace and edges add no words`() {
        assertEquals(listOf("قُلْ", "هُوَ"), words("  قُلْ   هُوَ \n"))
        assertEquals(0, ArabicWords.count(" "))
        assertEquals(0, ArabicWords.count(""))
    }

    @Test
    fun `a mark alone is no word`() {
        assertEquals(0, ArabicWords.count("۞"))
    }
}

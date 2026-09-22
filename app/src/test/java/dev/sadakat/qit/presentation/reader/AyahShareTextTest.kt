package dev.sadakat.qit.presentation.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class AyahShareTextTest {

    @Test
    fun `the Arabic, its translation and the reference, a paragraph each`() {
        assertEquals(
            "ٱللَّهُ لَآ إِلَـٰهَ إِلَّا هُوَ\n\nAllah - there is no deity except Him\n\n— Al-Baqarah 2:255",
            ayahShareText(
                " ٱللَّهُ لَآ إِلَـٰهَ إِلَّا هُوَ ",
                "Allah - there is no deity except Him ",
                "— Al-Baqarah 2:255",
            ),
        )
    }

    @Test
    fun `no translation leaves no empty paragraph`() {
        assertEquals("بِسْمِ\n\n— Al-Fatihah 1:1", ayahShareText("بِسْمِ", null, "— Al-Fatihah 1:1"))
        assertEquals("بِسْمِ\n\n— Al-Fatihah 1:1", ayahShareText("بِسْمِ", "  ", "— Al-Fatihah 1:1"))
    }
}

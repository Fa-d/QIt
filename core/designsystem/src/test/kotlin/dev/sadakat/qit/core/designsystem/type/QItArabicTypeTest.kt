package dev.sadakat.qit.core.designsystem.type

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class QItArabicTypeTest {

    private val type = QItArabicType()

    @Test
    fun `scale 1 keeps the same styles`() {
        assertSame(type, type.scaled(1f))
    }

    @Test
    fun `scaling grows the reading styles and their line heights together`() {
        val scaled = type.scaled(1.5f)

        assertEquals(39.sp, scaled.body.fontSize)
        assertEquals(75.sp, scaled.body.lineHeight)
        assertEquals(51.sp, scaled.display.fontSize)
        assertEquals(36.sp, scaled.title.fontSize)
    }

    @Test
    fun `list and watch styles never scale`() {
        val scaled = type.scaled(1.75f)

        assertEquals(type.label, scaled.label)
        assertEquals(type.watchBody, scaled.watchBody)
        assertEquals(type.watchTitle, scaled.watchTitle)
    }

    @Test
    fun `every Arabic style uses Amiri Quran`() {
        listOf(type.display, type.body, type.title, type.label, type.watchBody, type.watchTitle).forEach {
            assertEquals(QItFonts.AmiriQuran, it.fontFamily)
        }
    }
}

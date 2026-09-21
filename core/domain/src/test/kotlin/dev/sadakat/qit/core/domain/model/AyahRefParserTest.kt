package dev.sadakat.qit.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AyahRefParserTest {

    @Test
    fun `reads the usual way of quoting a verse`() {
        assertEquals(AyahRef(2, 255), AyahRefParser.parse("2:255"))
    }

    @Test
    fun `accepts other separators and surrounding spaces`() {
        listOf("2.255", "2/255", "2 255", " 2 : 255 ").forEach {
            assertEquals(it, AyahRef(2, 255), AyahRefParser.parse(it))
        }
    }

    @Test
    fun `reads Arabic-Indic and Bengali digits`() {
        assertEquals(AyahRef(2, 255), AyahRefParser.parse("٢:٢٥٥"))
        assertEquals(AyahRef(2, 255), AyahRefParser.parse("۲:۲۵۵"))
        assertEquals(AyahRef(2, 255), AyahRefParser.parse("২:২৫৫"))
    }

    @Test
    fun `rejects ayahs and surahs that don't exist`() {
        listOf("0:1", "115:1", "1:0", "1:8", "2:287").forEach {
            assertNull(it, AyahRefParser.parse(it))
        }
    }

    @Test
    fun `rejects anything that isn't a reference`() {
        listOf("", "2", "baqarah", "2:", ":255", "2:255:1", "1234:1", "2-255").forEach {
            assertNull(it, AyahRefParser.parse(it))
        }
    }
}

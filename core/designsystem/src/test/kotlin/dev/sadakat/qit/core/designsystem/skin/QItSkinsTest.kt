package dev.sadakat.qit.core.designsystem.skin

import dev.sadakat.qit.core.designsystem.color.darkQItColors
import dev.sadakat.qit.core.designsystem.color.lightQItColors
import dev.sadakat.qit.core.designsystem.type.QItUiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QItSkinsTest {

    @Test
    fun `the mushaf skin is the brand's look, unchanged`() {
        val light = QItSkins.of(QItStyle.MUSHAF, QItTone.LIGHT)
        val dark = QItSkins.of(QItStyle.MUSHAF, QItTone.DARK)

        assertEquals(lightQItColors(), light.colors)
        assertEquals(darkQItColors(), dark.colors)
        assertEquals(QItUiType(), light.type)
        assertFalse(light.surfaces.isTranslucent)
    }

    @Test
    fun `there is a skin for every style on every tone`() {
        val all = QItSkins.all

        assertEquals(QItStyle.entries.size * QItTone.entries.size, all.size)
        QItStyle.entries.forEach { style ->
            QItTone.entries.forEach { tone ->
                val skin = QItSkins.of(style, tone)
                assertEquals(style, skin.style)
                assertEquals(tone, skin.tone)
                assertEquals(tone == QItTone.DARK, skin.colors.isDark)
            }
        }
    }

    @Test
    fun `the mushaf type scale is the default one`() {
        assertEquals(QItUiType(), QItUiType.mushaf())
    }
}

package dev.sadakat.qit.core.designsystem.skin

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sadakat.qit.core.designsystem.color.QItColors
import dev.sadakat.qit.core.designsystem.color.darkQItColors
import dev.sadakat.qit.core.designsystem.color.lightQItColors
import dev.sadakat.qit.core.designsystem.scale.QItMotion
import dev.sadakat.qit.core.designsystem.scale.QItRadius
import dev.sadakat.qit.core.designsystem.scale.QItSprings
import dev.sadakat.qit.core.designsystem.scale.QItSurfaces
import dev.sadakat.qit.core.designsystem.shape.QItBadge
import dev.sadakat.qit.core.designsystem.shape.QItBadgeShape
import dev.sadakat.qit.core.designsystem.type.QItUiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QItSkinsTest {

    @Test
    fun `the mushaf skins on light and dark are the brand's look, unchanged`() {
        assertEquals(
            QItSkin(style = QItStyle.MUSHAF, tone = QItTone.LIGHT, colors = lightQItColors()),
            QItSkins.of(QItStyle.MUSHAF, QItTone.LIGHT),
        )
        assertEquals(
            QItSkin(style = QItStyle.MUSHAF, tone = QItTone.DARK, colors = darkQItColors()),
            QItSkins.of(QItStyle.MUSHAF, QItTone.DARK),
        )
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
    fun `each style wears its own badge, type and radius`() {
        val expected = mapOf(
            QItStyle.MUSHAF to Triple(QItBadge(QItBadgeShape.OCTAGRAM), QItUiType(), QItRadius()),
            QItStyle.MATERIAL to Triple(
                QItBadge(QItBadgeShape.CIRCLE, filled = true),
                QItUiType.sans(),
                QItRadius(xs = 4.dp, sm = 8.dp, md = 12.dp, lg = 16.dp, xl = 28.dp),
            ),
            QItStyle.EXPRESSIVE to Triple(
                QItBadge(QItBadgeShape.COOKIE, filled = true),
                QItUiType.emphasized(),
                QItRadius(xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 20.dp, xl = 32.dp),
            ),
            QItStyle.GLASS to Triple(
                QItBadge(QItBadgeShape.OCTAGRAM),
                QItUiType.sans(headingWeight = FontWeight.Medium),
                QItRadius(xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 24.dp, xl = 32.dp),
            ),
        )
        QItSkins.all.forEach { skin ->
            val (badge, type, radius) = expected.getValue(skin.style)
            assertEquals("${skin.style} ${skin.tone} badge", badge, skin.badge)
            assertEquals("${skin.style} ${skin.tone} type", type, skin.type)
            assertEquals("${skin.style} ${skin.tone} radius", radius, skin.radius)
        }
    }

    @Test
    fun `expressive moves on its springs, floats its chrome, glass does not shadow`() {
        val expressive = QItSkins.of(QItStyle.EXPRESSIVE, QItTone.LIGHT)
        assertEquals(QItSprings.Expressive, expressive.motion.springs)
        assertEquals(12.dp, expressive.surfaces.floatingInset)
        assertEquals(6.dp, expressive.surfaces.shadow)

        val glass = QItSkins.of(QItStyle.GLASS, QItTone.LIGHT)
        assertEquals(QItMotion(), glass.motion)
        assertEquals(0.dp, glass.surfaces.shadow)
    }

    @Test
    fun `sepia is a light page that keeps its style's accents`() {
        QItStyle.entries.forEach { style ->
            val sepia = QItSkins.of(style, QItTone.SEPIA).colors
            val light = QItSkins.of(style, QItTone.LIGHT).colors

            assertFalse(sepia.isDark)
            assertNotEquals(light.surface, sepia.surface)
            listOf(
                QItColors::primary, QItColors::onPrimary, QItColors::primaryContainer, QItColors::onPrimaryContainer,
                QItColors::inversePrimary, QItColors::secondary, QItColors::onSecondary, QItColors::secondaryContainer,
                QItColors::onSecondaryContainer, QItColors::tertiary, QItColors::onTertiary,
                QItColors::tertiaryContainer, QItColors::onTertiaryContainer, QItColors::error, QItColors::onError,
                QItColors::errorContainer, QItColors::onErrorContainer, QItColors::playingAyahHighlight,
                QItColors::onPlayingAyahHighlight, QItColors::currentWordHighlight, QItColors::onCurrentWordHighlight,
                QItColors::currentWord, QItColors::currentWordOnHighlight, QItColors::upcomingWordOnHighlight,
                QItColors::ornament, QItColors::progressTrack,
            ).forEach { role ->
                assertEquals("$style keeps ${role.name} in sepia", role.get(light), role.get(sepia))
            }
        }
    }

    @Test
    fun `only glass lets the page show through`() {
        QItSkins.all.forEach { skin ->
            assertEquals(skin.style == QItStyle.GLASS, skin.surfaces.isTranslucent)
        }
    }

    @Test
    fun `the mushaf type scale is the default one`() {
        assertEquals(QItUiType(), QItUiType.mushaf())
    }
}

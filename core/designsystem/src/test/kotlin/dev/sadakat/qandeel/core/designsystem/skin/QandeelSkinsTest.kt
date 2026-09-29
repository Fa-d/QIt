package dev.sadakat.qandeel.core.designsystem.skin

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sadakat.qandeel.core.designsystem.color.QandeelColors
import dev.sadakat.qandeel.core.designsystem.color.darkQandeelColors
import dev.sadakat.qandeel.core.designsystem.color.lightQandeelColors
import dev.sadakat.qandeel.core.designsystem.scale.QandeelMotion
import dev.sadakat.qandeel.core.designsystem.scale.QandeelRadius
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSprings
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSurfaces
import dev.sadakat.qandeel.core.designsystem.shape.QandeelBadge
import dev.sadakat.qandeel.core.designsystem.shape.QandeelBadgeShape
import dev.sadakat.qandeel.core.designsystem.type.QandeelUiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QandeelSkinsTest {

    @Test
    fun `the mushaf skins on light and dark are the brand's look, unchanged`() {
        assertEquals(
            QandeelSkin(style = QandeelStyle.MUSHAF, tone = QandeelTone.LIGHT, colors = lightQandeelColors()),
            QandeelSkins.of(QandeelStyle.MUSHAF, QandeelTone.LIGHT),
        )
        assertEquals(
            QandeelSkin(style = QandeelStyle.MUSHAF, tone = QandeelTone.DARK, colors = darkQandeelColors()),
            QandeelSkins.of(QandeelStyle.MUSHAF, QandeelTone.DARK),
        )
    }

    @Test
    fun `there is a skin for every style on every tone`() {
        val all = QandeelSkins.all

        assertEquals(QandeelStyle.entries.size * QandeelTone.entries.size, all.size)
        QandeelStyle.entries.forEach { style ->
            QandeelTone.entries.forEach { tone ->
                val skin = QandeelSkins.of(style, tone)
                assertEquals(style, skin.style)
                assertEquals(tone, skin.tone)
                assertEquals(tone == QandeelTone.DARK, skin.colors.isDark)
            }
        }
    }

    @Test
    fun `each style wears its own badge, type and radius`() {
        val expected = mapOf(
            QandeelStyle.MUSHAF to Triple(QandeelBadge(QandeelBadgeShape.OCTAGRAM), QandeelUiType(), QandeelRadius()),
            QandeelStyle.MATERIAL to Triple(
                QandeelBadge(QandeelBadgeShape.CIRCLE, filled = true),
                QandeelUiType.sans(),
                QandeelRadius(xs = 4.dp, sm = 8.dp, md = 12.dp, lg = 16.dp, xl = 28.dp),
            ),
            QandeelStyle.EXPRESSIVE to Triple(
                QandeelBadge(QandeelBadgeShape.COOKIE, filled = true),
                QandeelUiType.emphasized(),
                QandeelRadius(xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 20.dp, xl = 32.dp),
            ),
            QandeelStyle.GLASS to Triple(
                QandeelBadge(QandeelBadgeShape.OCTAGRAM),
                QandeelUiType.sans(headingWeight = FontWeight.Medium),
                QandeelRadius(xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 24.dp, xl = 32.dp),
            ),
        )
        QandeelSkins.all.forEach { skin ->
            val (badge, type, radius) = expected.getValue(skin.style)
            assertEquals("${skin.style} ${skin.tone} badge", badge, skin.badge)
            assertEquals("${skin.style} ${skin.tone} type", type, skin.type)
            assertEquals("${skin.style} ${skin.tone} radius", radius, skin.radius)
        }
    }

    @Test
    fun `expressive moves on its springs, floats its chrome, glass does not shadow`() {
        val expressive = QandeelSkins.of(QandeelStyle.EXPRESSIVE, QandeelTone.LIGHT)
        assertEquals(QandeelSprings.Expressive, expressive.motion.springs)
        assertEquals(12.dp, expressive.surfaces.floatingInset)
        assertEquals(6.dp, expressive.surfaces.shadow)

        val glass = QandeelSkins.of(QandeelStyle.GLASS, QandeelTone.LIGHT)
        assertEquals(QandeelMotion(), glass.motion)
        assertEquals(0.dp, glass.surfaces.shadow)
    }

    @Test
    fun `sepia is a light page that keeps its style's accents`() {
        QandeelStyle.entries.forEach { style ->
            val sepia = QandeelSkins.of(style, QandeelTone.SEPIA).colors
            val light = QandeelSkins.of(style, QandeelTone.LIGHT).colors

            assertFalse(sepia.isDark)
            assertNotEquals(light.surface, sepia.surface)
            listOf(
                QandeelColors::primary, QandeelColors::onPrimary, QandeelColors::primaryContainer,
                QandeelColors::onPrimaryContainer, QandeelColors::inversePrimary, QandeelColors::secondary,
                QandeelColors::onSecondary, QandeelColors::secondaryContainer, QandeelColors::onSecondaryContainer,
                QandeelColors::tertiary, QandeelColors::onTertiary, QandeelColors::tertiaryContainer,
                QandeelColors::onTertiaryContainer, QandeelColors::error, QandeelColors::onError,
                QandeelColors::errorContainer, QandeelColors::onErrorContainer, QandeelColors::playingAyahHighlight,
                QandeelColors::onPlayingAyahHighlight, QandeelColors::currentWordHighlight,
                QandeelColors::onCurrentWordHighlight, QandeelColors::currentWord,
                QandeelColors::currentWordOnHighlight, QandeelColors::upcomingWordOnHighlight, QandeelColors::ornament,
                QandeelColors::progressTrack,
            ).forEach { role ->
                assertEquals("$style keeps ${role.name} in sepia", role.get(light), role.get(sepia))
            }
        }
    }

    @Test
    fun `only glass lets the page show through`() {
        QandeelSkins.all.forEach { skin ->
            assertEquals(skin.style == QandeelStyle.GLASS, skin.surfaces.isTranslucent)
        }
    }

    @Test
    fun `the mushaf type scale is the default one`() {
        assertEquals(QandeelUiType(), QandeelUiType.mushaf())
    }
}

package dev.sadakat.qit.core.designsystem.skin

import androidx.compose.runtime.Immutable
import dev.sadakat.qit.core.designsystem.color.QItColors
import dev.sadakat.qit.core.designsystem.color.darkQItColors
import dev.sadakat.qit.core.designsystem.color.lightQItColors
import dev.sadakat.qit.core.designsystem.scale.QItMotion
import dev.sadakat.qit.core.designsystem.scale.QItRadius
import dev.sadakat.qit.core.designsystem.scale.QItSurfaces
import dev.sadakat.qit.core.designsystem.shape.QItBadge
import dev.sadakat.qit.core.designsystem.type.QItUiType

/**
 * Everything that makes a look: one [QItStyle] on one [QItTone]. Components read these tokens and
 * never ask which style is on, so a new look is a new value here, with no component code.
 *
 * Spacing, sizes, elevation and the Arabic styles are the same in every look: the page's rhythm and
 * the Quran's text don't change with the chrome.
 */
@Immutable
data class QItSkin(
    val style: QItStyle,
    val tone: QItTone,
    val colors: QItColors,
    val type: QItUiType = QItUiType(),
    val radius: QItRadius = QItRadius(),
    val surfaces: QItSurfaces = QItSurfaces(),
    val motion: QItMotion = QItMotion(),
    val badge: QItBadge = QItBadge(),
)

/** The skins the apps can show. */
object QItSkins {

    /** The skin of [style] on [tone]. */
    fun of(style: QItStyle, tone: QItTone): QItSkin = mushaf(tone).copy(style = style)

    /** Every style on every tone, for tests that must hold for all of them. */
    val all: List<QItSkin>
        get() = QItStyle.entries.flatMap { style -> QItTone.entries.map { tone -> of(style, tone) } }

    private fun mushaf(tone: QItTone) = QItSkin(
        style = QItStyle.MUSHAF,
        tone = tone,
        colors = when (tone) {
            QItTone.LIGHT, QItTone.SEPIA -> lightQItColors()
            QItTone.DARK -> darkQItColors()
        },
    )
}

package dev.sadakat.qit.core.designsystem.skin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sadakat.qit.core.designsystem.color.QItColors
import dev.sadakat.qit.core.designsystem.color.darkQItColors
import dev.sadakat.qit.core.designsystem.color.lightQItColors
import dev.sadakat.qit.core.designsystem.color.withExpressiveContainers
import dev.sadakat.qit.core.designsystem.color.withSepiaPage
import dev.sadakat.qit.core.designsystem.ref.QItPaletteSet
import dev.sadakat.qit.core.designsystem.scale.QItMotion
import dev.sadakat.qit.core.designsystem.scale.QItRadius
import dev.sadakat.qit.core.designsystem.scale.QItSprings
import dev.sadakat.qit.core.designsystem.scale.QItSurfaces
import dev.sadakat.qit.core.designsystem.shape.QItBadge
import dev.sadakat.qit.core.designsystem.shape.QItBadgeShape
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
    fun of(style: QItStyle, tone: QItTone): QItSkin = when (style) {
        QItStyle.MUSHAF -> mushaf(tone)
        QItStyle.MATERIAL -> material(tone)
        QItStyle.EXPRESSIVE -> expressive(tone)
        QItStyle.GLASS -> glass(tone)
    }

    /** Every style on every tone, for tests that must hold for all of them. */
    val all: List<QItSkin>
        get() = QItStyle.entries.flatMap { style -> QItTone.entries.map { tone -> of(style, tone) } }

    /** The brand: serif headings, the rub el hizb, opaque paper. */
    private fun mushaf(tone: QItTone) = QItSkin(
        style = QItStyle.MUSHAF,
        tone = tone,
        colors = colors(QItStyle.MUSHAF, tone),
    )

    /** Stock Material 3: the baseline palette, the platform sans, round filled badges. */
    private fun material(tone: QItTone) = QItSkin(
        style = QItStyle.MATERIAL,
        tone = tone,
        colors = colors(QItStyle.MATERIAL, tone),
        type = QItUiType.sans(),
        badge = QItBadge(QItBadgeShape.CIRCLE, filled = true),
    )

    /** Material 3 Expressive: bolder shapes, springy motion, emphasized type, cookie badges. */
    private fun expressive(tone: QItTone) = QItSkin(
        style = QItStyle.EXPRESSIVE,
        tone = tone,
        colors = colors(QItStyle.EXPRESSIVE, tone),
        type = QItUiType.emphasized(),
        radius = QItRadius(xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 20.dp, xl = 32.dp),
        surfaces = QItSurfaces(floatingInset = 12.dp, shadow = 6.dp),
        motion = QItMotion(springs = QItSprings.Expressive),
        badge = QItBadge(QItBadgeShape.COOKIE, filled = true),
    )

    /** Frosted glass: translucent blurred chrome, sans headings at medium weight. */
    private fun glass(tone: QItTone) = QItSkin(
        style = QItStyle.GLASS,
        tone = tone,
        colors = colors(QItStyle.GLASS, tone),
        type = QItUiType.sans(headingWeight = FontWeight.Medium),
        radius = QItRadius(xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 24.dp, xl = 32.dp),
        surfaces = glassSurfaces(tone),
    )

    /** One skin's colors: its palette set's scheme on the page tone, Expressive's ink on top. */
    private fun colors(style: QItStyle, tone: QItTone): QItColors {
        val palettes = when (style) {
            QItStyle.MUSHAF, QItStyle.GLASS -> QItPaletteSet.Mushaf
            QItStyle.MATERIAL, QItStyle.EXPRESSIVE -> QItPaletteSet.Baseline
        }
        val scheme = when (tone) {
            QItTone.LIGHT, QItTone.SEPIA -> lightQItColors(palettes)
            QItTone.DARK -> darkQItColors(palettes)
        }
        val page = if (tone == QItTone.SEPIA) scheme.withSepiaPage() else scheme
        return if (style == QItStyle.EXPRESSIVE) page.withExpressiveContainers(palettes) else page
    }

    /**
     * Frosted glass: translucent, blurred chrome with a hairline edge instead of a shadow. The
     * alphas keep `onSurface` and `onSurfaceVariant` readable over every backdrop the page can
     * show under the chrome (GlassContrastTest holds them to 4.5:1).
     */
    private fun glassSurfaces(tone: QItTone) = QItSurfaces(
        chromeAlpha = if (tone == QItTone.DARK) 0.8f else 0.82f,
        chromeFallbackAlpha = 0.9f,
        sheetAlpha = 0.9f,
        cardAlpha = 0.85f,
        blurRadius = 24.dp,
        noise = 0.05f,
        hairline = 1.dp,
        hairlineAlpha = if (tone == QItTone.DARK) 0.18f else 0.35f,
        floatingInset = 12.dp,
        shadow = 0.dp,
        backdropWash = 0.35f,
    )
}

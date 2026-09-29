package dev.sadakat.qandeel.core.designsystem.skin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sadakat.qandeel.core.designsystem.color.QandeelColors
import dev.sadakat.qandeel.core.designsystem.color.darkQandeelColors
import dev.sadakat.qandeel.core.designsystem.color.lightQandeelColors
import dev.sadakat.qandeel.core.designsystem.color.withExpressiveContainers
import dev.sadakat.qandeel.core.designsystem.color.withSepiaPage
import dev.sadakat.qandeel.core.designsystem.ref.QandeelPaletteSet
import dev.sadakat.qandeel.core.designsystem.scale.QandeelMotion
import dev.sadakat.qandeel.core.designsystem.scale.QandeelRadius
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSprings
import dev.sadakat.qandeel.core.designsystem.scale.QandeelSurfaces
import dev.sadakat.qandeel.core.designsystem.shape.QandeelBadge
import dev.sadakat.qandeel.core.designsystem.shape.QandeelBadgeShape
import dev.sadakat.qandeel.core.designsystem.type.QandeelUiType

/**
 * Everything that makes a look: one [QandeelStyle] on one [QandeelTone]. Components read these tokens and
 * never ask which style is on, so a new look is a new value here, with no component code.
 *
 * Spacing, sizes, elevation and the Arabic styles are the same in every look: the page's rhythm and
 * the Quran's text don't change with the chrome.
 */
@Immutable
data class QandeelSkin(
    val style: QandeelStyle,
    val tone: QandeelTone,
    val colors: QandeelColors,
    val type: QandeelUiType = QandeelUiType(),
    val radius: QandeelRadius = QandeelRadius(),
    val surfaces: QandeelSurfaces = QandeelSurfaces(),
    val motion: QandeelMotion = QandeelMotion(),
    val badge: QandeelBadge = QandeelBadge(),
)

/** The skins the apps can show. */
object QandeelSkins {

    /** The skin of [style] on [tone]. */
    fun of(style: QandeelStyle, tone: QandeelTone): QandeelSkin = when (style) {
        QandeelStyle.MUSHAF -> mushaf(tone)
        QandeelStyle.MATERIAL -> material(tone)
        QandeelStyle.EXPRESSIVE -> expressive(tone)
        QandeelStyle.GLASS -> glass(tone)
    }

    /** Every style on every tone, for tests that must hold for all of them. */
    val all: List<QandeelSkin>
        get() = QandeelStyle.entries.flatMap { style -> QandeelTone.entries.map { tone -> of(style, tone) } }

    /** The brand: serif headings, the rub el hizb, opaque paper. */
    private fun mushaf(tone: QandeelTone) = QandeelSkin(
        style = QandeelStyle.MUSHAF,
        tone = tone,
        colors = colors(QandeelStyle.MUSHAF, tone),
    )

    /** Stock Material 3: the baseline palette, the platform sans, round filled badges. */
    private fun material(tone: QandeelTone) = QandeelSkin(
        style = QandeelStyle.MATERIAL,
        tone = tone,
        colors = colors(QandeelStyle.MATERIAL, tone),
        type = QandeelUiType.sans(),
        badge = QandeelBadge(QandeelBadgeShape.CIRCLE, filled = true),
    )

    /** Material 3 Expressive: bolder shapes, springy motion, emphasized type, cookie badges. */
    private fun expressive(tone: QandeelTone) = QandeelSkin(
        style = QandeelStyle.EXPRESSIVE,
        tone = tone,
        colors = colors(QandeelStyle.EXPRESSIVE, tone),
        type = QandeelUiType.emphasized(),
        radius = QandeelRadius(xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 20.dp, xl = 32.dp),
        surfaces = QandeelSurfaces(floatingInset = 12.dp, shadow = 6.dp),
        motion = QandeelMotion(springs = QandeelSprings.Expressive),
        badge = QandeelBadge(QandeelBadgeShape.COOKIE, filled = true),
    )

    /** Frosted glass: translucent blurred chrome, sans headings at medium weight. */
    private fun glass(tone: QandeelTone) = QandeelSkin(
        style = QandeelStyle.GLASS,
        tone = tone,
        colors = colors(QandeelStyle.GLASS, tone),
        type = QandeelUiType.sans(headingWeight = FontWeight.Medium),
        radius = QandeelRadius(xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 24.dp, xl = 32.dp),
        surfaces = glassSurfaces(tone),
    )

    /** One skin's colors: its palette set's scheme on the page tone, Expressive's ink on top. */
    private fun colors(style: QandeelStyle, tone: QandeelTone): QandeelColors {
        val palettes = when (style) {
            QandeelStyle.MUSHAF, QandeelStyle.GLASS -> QandeelPaletteSet.Mushaf
            QandeelStyle.MATERIAL, QandeelStyle.EXPRESSIVE -> QandeelPaletteSet.Baseline
        }
        val scheme = when (tone) {
            QandeelTone.LIGHT, QandeelTone.SEPIA -> lightQandeelColors(palettes)
            QandeelTone.DARK -> darkQandeelColors(palettes)
        }
        val page = if (tone == QandeelTone.SEPIA) scheme.withSepiaPage() else scheme
        return if (style == QandeelStyle.EXPRESSIVE) page.withExpressiveContainers(palettes) else page
    }

    /**
     * Frosted glass: translucent, blurred chrome with a hairline edge instead of a shadow. The
     * alphas keep `onSurface` and `onSurfaceVariant` readable over every backdrop the page can
     * show under the chrome (GlassContrastTest holds them to 4.5:1).
     */
    private fun glassSurfaces(tone: QandeelTone) = QandeelSurfaces(
        chromeAlpha = if (tone == QandeelTone.DARK) 0.8f else 0.82f,
        chromeFallbackAlpha = 0.9f,
        sheetAlpha = 0.9f,
        cardAlpha = 0.85f,
        blurRadius = 24.dp,
        noise = 0.05f,
        hairline = 1.dp,
        hairlineAlpha = if (tone == QandeelTone.DARK) 0.18f else 0.35f,
        floatingInset = 12.dp,
        shadow = 0.dp,
        backdropWash = 0.35f,
    )
}

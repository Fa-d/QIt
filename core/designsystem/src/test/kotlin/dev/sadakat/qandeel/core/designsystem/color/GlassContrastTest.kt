package dev.sadakat.qandeel.core.designsystem.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkins
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2 contrast of text on frosted chrome: a translucent surface is readable only over what can
 * be under it, so the chrome and sheet colors are composited over every backdrop the page can show
 * — its own roles, and pure black and white — by blending in sRGB
 * (src * a + dst * (1 - a)), the way a translucent layer draws without blur.
 */
class GlassContrastTest {

    private val glass = QandeelSkins.all.filter { it.surfaces.isTranslucent }

    @Test
    fun `text on frosted chrome is readable over every backdrop`() {
        val failures = glass.flatMap { skin ->
            val colors = skin.colors
            val chrome = listOf(
                "chrome" to skin.surfaces.chromeAlpha,
                "chromeFallback" to skin.surfaces.chromeFallbackAlpha,
            )
            val backdrops = listOf(
                "background" to colors.background,
                "surface" to colors.surface,
                "arabicText" to colors.arabicText,
                "translationText" to colors.translationText,
                "playingAyahHighlight" to colors.playingAyahHighlight,
                "currentWordHighlight" to colors.currentWordHighlight,
                "primaryContainer" to colors.primaryContainer,
                "black" to Color.Black,
                "white" to Color.White,
            )
            chrome.flatMap { (kind, alpha) ->
                backdrops.flatMap { (name, backdrop) ->
                    readable(
                        "${skin.style} ${skin.tone} $kind over $name",
                        colors.surfaceContainer,
                        alpha,
                        backdrop,
                        colors,
                    )
                }
            }
        }
        assertTrue("Below 4.5:1 on frosted chrome —\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `text on frosted sheets is readable over black and white`() {
        val failures = glass.flatMap { skin ->
            listOf(Color.Black, Color.White).flatMap { backdrop ->
                readable(
                    "${skin.style} ${skin.tone} sheet",
                    skin.colors.surfaceContainerLow,
                    skin.surfaces.sheetAlpha,
                    backdrop,
                    skin.colors,
                )
            }
        }
        assertTrue("Below 4.5:1 on a frosted sheet —\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** The failures (empty when readable) of both body inks on [surface] at [alpha] over [backdrop]. */
    private fun readable(
        where: String,
        surface: Color,
        alpha: Float,
        backdrop: Color,
        colors: QandeelColors,
    ): List<String> {
        val composite = surface.compositeOver(backdrop, alpha)
        return listOf(colors.onSurface to "onSurface", colors.onSurfaceVariant to "onSurfaceVariant")
            .mapNotNull { (ink, role) ->
                val ratio = contrast(ink, composite)
                "$where: $role on $composite is ${"%.2f".format(ratio)}:1".takeIf { ratio < TEXT }
            }
    }

    /** [surface] as a translucent layer at [alpha] over [backdrop], blended per sRGB channel. */
    private fun Color.compositeOver(backdrop: Color, alpha: Float): Color = Color(
        red = red * alpha + backdrop.red * (1 - alpha),
        green = green * alpha + backdrop.green * (1 - alpha),
        blue = blue * alpha + backdrop.blue * (1 - alpha),
    )

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private companion object {
        const val TEXT = 4.5
    }
}

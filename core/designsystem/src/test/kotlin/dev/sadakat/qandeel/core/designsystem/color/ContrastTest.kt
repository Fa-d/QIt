package dev.sadakat.qandeel.core.designsystem.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.sadakat.qandeel.core.designsystem.skin.QandeelSkins
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2 contrast of every pair of roles that is drawn on top of each other, over every skin. Text
 * needs 4.5:1, the Quran's Arabic 7:1 (AAA: it is read for long stretches, often at night), and UI
 * shapes 3:1.
 */
class ContrastTest {

    private val schemes: Map<String, QandeelColors> =
        QandeelSkins.all.associate { skin ->
            "${skin.style.name.lowercase()} ${skin.tone.name.lowercase()}" to skin.colors
        } +
            ("watch" to watchQandeelColors())

    @Test
    fun `body text is readable on every surface`() = assertEach(TEXT) { c ->
        listOf(c.surface, c.surfaceContainerLow, c.surfaceContainer, c.surfaceContainerHigh)
            .flatMap { surface -> listOf(c.onSurface to surface, c.onSurfaceVariant to surface) }
    }

    @Test
    fun `the Quran text reaches AAA on the page and on the playing highlight`() = assertEach(ARABIC) { c ->
        listOf(
            c.arabicText to c.surface,
            c.arabicText to c.background,
            c.onPlayingAyahHighlight to c.playingAyahHighlight,
        )
    }

    @Test
    fun `the word being recited reaches AAA on its pill`() = assertEach(ARABIC) { c ->
        listOf(c.onCurrentWordHighlight to c.currentWordHighlight)
    }

    @Test
    fun `the pill stands out from the page and from the playing highlight`() = assertEach(UI) { c ->
        listOf(c.currentWordHighlight to c.surface, c.currentWordHighlight to c.playingAyahHighlight)
    }

    @Test
    fun `the translation being read reaches AAA too`() = assertEach(ARABIC) { c ->
        listOf(c.currentWord to c.surface, c.currentWordOnHighlight to c.playingAyahHighlight)
    }

    @Test
    fun `words still to come are quieter but readable`() = assertEach(TEXT) { c ->
        listOf(c.upcomingWord to c.surface, c.upcomingWordOnHighlight to c.playingAyahHighlight)
    }

    @Test
    fun `translations are readable on the page and on the playing highlight`() = assertEach(TEXT) { c ->
        listOf(c.translationText to c.surface, c.translationText to c.playingAyahHighlight)
    }

    @Test
    fun `content on colored containers is readable`() = assertEach(TEXT) { c ->
        listOf(
            c.onPrimary to c.primary,
            c.onPrimaryContainer to c.primaryContainer,
            c.onSecondary to c.secondary,
            c.onSecondaryContainer to c.secondaryContainer,
            c.onTertiary to c.tertiary,
            c.onTertiaryContainer to c.tertiaryContainer,
            c.onError to c.error,
            c.onErrorContainer to c.errorContainer,
            c.inverseOnSurface to c.inverseSurface,
        )
    }

    @Test
    fun `controls, ornaments and progress stand out from the page`() = assertEach(UI) { c ->
        listOf(
            c.primary to c.surface,
            c.ornament to c.surface,
            c.outline to c.surface,
            c.primary to c.progressTrack,
        )
    }

    private fun assertEach(minimum: Double, pairs: (QandeelColors) -> List<Pair<Color, Color>>) {
        val failures = schemes.flatMap { (name, colors) ->
            pairs(colors).mapNotNull { (foreground, background) ->
                val ratio = contrast(foreground, background)
                "$name: $foreground on $background is ${"%.2f".format(ratio)}:1".takeIf { ratio < minimum }
            }
        }
        assertTrue("Below $minimum:1 —\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private companion object {
        const val TEXT = 4.5
        const val ARABIC = 7.0
        const val UI = 3.0
    }
}

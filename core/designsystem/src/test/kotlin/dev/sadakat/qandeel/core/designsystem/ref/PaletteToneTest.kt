package dev.sadakat.qandeel.core.designsystem.ref

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cbrt

/**
 * A palette's tone is CIE L* — the same perceived lightness in every palette, which is what lets
 * tone numbers predict text contrast. Every tone of every palette must survive the trip through
 * sRGB within 1 L*.
 */
class PaletteToneTest {

    private val palettes = mapOf(
        "primary" to QandeelPalettes.primary,
        "secondary" to QandeelPalettes.secondary,
        "tertiary" to QandeelPalettes.tertiary,
        "neutral" to QandeelPalettes.neutral,
        "neutralVariant" to QandeelPalettes.neutralVariant,
        "error" to QandeelPalettes.error,
        "baselinePrimary" to QandeelPalettes.baselinePrimary,
        "baselineSecondary" to QandeelPalettes.baselineSecondary,
        "baselineTertiary" to QandeelPalettes.baselineTertiary,
        "baselineNeutral" to QandeelPalettes.baselineNeutral,
        "baselineNeutralVariant" to QandeelPalettes.baselineNeutralVariant,
        "sepiaNeutral" to QandeelPalettes.sepiaNeutral,
        "sepiaNeutralVariant" to QandeelPalettes.sepiaNeutralVariant,
    )

    @Test
    fun `every generated tone is its tone number in CIE L*`() {
        val failures = palettes.flatMap { (name, palette) ->
            palette.generatedTones.mapNotNull { tone ->
                val lightness = palette[tone].lstar()
                "$name tone $tone is L* ${"%.2f".format(lightness)}".takeIf { abs(lightness - tone) > TOLERANCE }
            }
        }
        assertTrue("Tone drifted from L* —\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** CIE L* of a color, from WCAG relative luminance (its Y tristimulus value, 0..1). */
    private fun Color.lstar(): Double {
        val y = luminance().toDouble()
        return if (y > 216.0 / 24389.0) 116 * cbrt(y) - 16 else y * 24389 / 27
    }

    private companion object {
        const val TOLERANCE = 1.0
    }
}

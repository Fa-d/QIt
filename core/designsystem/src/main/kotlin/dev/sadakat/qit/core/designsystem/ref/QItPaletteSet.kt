package dev.sadakat.qit.core.designsystem.ref

import androidx.compose.runtime.Immutable

/**
 * The tonal palettes one look is built from. The [dev.sadakat.qit.core.designsystem.color.QItColors]
 * builders take a set, so the same role mappings dress every look: only the palettes — and the page
 * tone's tone numbers — differ. Every set shares Material's standard error palette.
 */
@Immutable
data class QItPaletteSet(
    val primary: TonalPalette,
    val secondary: TonalPalette,
    val tertiary: TonalPalette,
    val neutral: TonalPalette,
    val neutralVariant: TonalPalette,
    val error: TonalPalette,
) {
    companion object {
        /** The brand's palettes: deep green, sage, gold over warm paper. */
        val Mushaf = QItPaletteSet(
            primary = QItPalettes.primary,
            secondary = QItPalettes.secondary,
            tertiary = QItPalettes.tertiary,
            neutral = QItPalettes.neutral,
            neutralVariant = QItPalettes.neutralVariant,
            error = QItPalettes.error,
        )

        /** Material 3's baseline palettes, from its seed #6750A4. */
        val Baseline = QItPaletteSet(
            primary = QItPalettes.baselinePrimary,
            secondary = QItPalettes.baselineSecondary,
            tertiary = QItPalettes.baselineTertiary,
            neutral = QItPalettes.baselineNeutral,
            neutralVariant = QItPalettes.baselineNeutralVariant,
            error = QItPalettes.error,
        )
    }
}

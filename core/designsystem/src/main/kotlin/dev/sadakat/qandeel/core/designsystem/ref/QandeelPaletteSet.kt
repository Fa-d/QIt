package dev.sadakat.qandeel.core.designsystem.ref

import androidx.compose.runtime.Immutable

/**
 * The tonal palettes one look is built from. The [dev.sadakat.qandeel.core.designsystem.color.QandeelColors]
 * builders take a set, so the same role mappings dress every look: only the palettes — and the page
 * tone's tone numbers — differ. Every set shares Material's standard error palette.
 */
@Immutable
data class QandeelPaletteSet(
    val primary: TonalPalette,
    val secondary: TonalPalette,
    val tertiary: TonalPalette,
    val neutral: TonalPalette,
    val neutralVariant: TonalPalette,
    val error: TonalPalette,
) {
    companion object {
        /** The brand's palettes: deep green, sage, gold over warm paper. */
        val Mushaf = QandeelPaletteSet(
            primary = QandeelPalettes.primary,
            secondary = QandeelPalettes.secondary,
            tertiary = QandeelPalettes.tertiary,
            neutral = QandeelPalettes.neutral,
            neutralVariant = QandeelPalettes.neutralVariant,
            error = QandeelPalettes.error,
        )

        /** Material 3's baseline palettes, from its seed #6750A4. */
        val Baseline = QandeelPaletteSet(
            primary = QandeelPalettes.baselinePrimary,
            secondary = QandeelPalettes.baselineSecondary,
            tertiary = QandeelPalettes.baselineTertiary,
            neutral = QandeelPalettes.baselineNeutral,
            neutralVariant = QandeelPalettes.baselineNeutralVariant,
            error = QandeelPalettes.error,
        )
    }
}

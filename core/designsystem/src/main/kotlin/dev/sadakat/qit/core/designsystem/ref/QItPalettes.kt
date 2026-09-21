package dev.sadakat.qit.core.designsystem.ref

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Reference tokens: the six tonal palettes every color role is picked from. Generated in the HCT
 * color space (Material Color Utilities) from the brand seeds, so a tone means the same perceived
 * lightness in every palette: tone 0 is black, 100 white, and tone contrast predicts text contrast.
 *
 * Only theme code (the semantic [dev.sadakat.qit.core.designsystem.color.QItColors] builders and the
 * apps' MaterialTheme mappings) reads these; UI code uses semantic roles. This file is the only one
 * in the codebase allowed to spell out color literals.
 */
@Immutable
class TonalPalette internal constructor(private val tones: Map<Int, Color>) {
    /** The color at [tone] (0..100); only the generated tones exist. */
    operator fun get(tone: Int): Color = requireNotNull(tones[tone]) { "Tone $tone is not generated" }
}

object QItPalettes {
    /** Deep mushaf green — the brand color (hue 165, chroma 30). */
    val primary = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF001209),
            6 to Color(0xFF00180D),
            10 to Color(0xFF002114),
            12 to Color(0xFF002517),
            15 to Color(0xFF002C1C),
            17 to Color(0xFF00311F),
            20 to Color(0xFF003824),
            22 to Color(0xFF003D28),
            24 to Color(0xFF05422C),
            25 to Color(0xFF09442E),
            30 to Color(0xFF195039),
            35 to Color(0xFF275C44),
            40 to Color(0xFF336850),
            50 to Color(0xFF4D8267),
            60 to Color(0xFF669C80),
            70 to Color(0xFF80B79A),
            80 to Color(0xFF9BD3B4),
            87 to Color(0xFFAEE7C7),
            90 to Color(0xFFB6EFD0),
            92 to Color(0xFFBCF5D5),
            94 to Color(0xFFC1FBDB),
            95 to Color(0xFFC4FEDE),
            96 to Color(0xFFCDFFE2),
            98 to Color(0xFFE8FFEF),
            99 to Color(0xFFF4FFF6),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** Sage — a quieter green for secondary containers (hue 165, chroma 14). */
    val secondary = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF02110A),
            6 to Color(0xFF05170F),
            10 to Color(0xFF0D1F16),
            12 to Color(0xFF12231A),
            15 to Color(0xFF182920),
            17 to Color(0xFF1C2E24),
            20 to Color(0xFF22342B),
            22 to Color(0xFF27392F),
            24 to Color(0xFF2B3D33),
            25 to Color(0xFF2D3F35),
            30 to Color(0xFF394B41),
            35 to Color(0xFF44574C),
            40 to Color(0xFF506358),
            50 to Color(0xFF687C70),
            60 to Color(0xFF829589),
            70 to Color(0xFF9CB0A3),
            80 to Color(0xFFB7CBBE),
            87 to Color(0xFFCADFD1),
            90 to Color(0xFFD3E8D9),
            92 to Color(0xFFD8EDDF),
            94 to Color(0xFFDEF3E5),
            95 to Color(0xFFE1F6E8),
            96 to Color(0xFFE4F9EA),
            98 to Color(0xFFE9FFF0),
            99 to Color(0xFFF4FFF6),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** Illumination gold — the accent of ornaments and highlights (hue 86, chroma 44). */
    val tertiary = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF150D00),
            6 to Color(0xFF1B1200),
            10 to Color(0xFF261A00),
            12 to Color(0xFF2B1D00),
            15 to Color(0xFF322300),
            17 to Color(0xFF372700),
            20 to Color(0xFF3F2E00),
            22 to Color(0xFF453200),
            24 to Color(0xFF4A3600),
            25 to Color(0xFF4D3800),
            30 to Color(0xFF5B4300),
            35 to Color(0xFF6A4E00),
            40 to Color(0xFF795900),
            50 to Color(0xFF977100),
            60 to Color(0xFFB48A23),
            70 to Color(0xFFD1A53C),
            80 to Color(0xFFEFC055),
            87 to Color(0xFFFFD579),
            90 to Color(0xFFFFDF9E),
            92 to Color(0xFFFFE5B4),
            94 to Color(0xFFFFECCA),
            95 to Color(0xFFFFEFD4),
            96 to Color(0xFFFFF2DE),
            98 to Color(0xFFFFF8F2),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** Warm paper and ink — every surface and body text (hue 90, chroma 5). */
    val neutral = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF110E08),
            6 to Color(0xFF16130D),
            10 to Color(0xFF1E1B14),
            12 to Color(0xFF221F18),
            15 to Color(0xFF29251E),
            17 to Color(0xFF2D2A22),
            20 to Color(0xFF343029),
            22 to Color(0xFF38342D),
            24 to Color(0xFF3D3931),
            25 to Color(0xFF3F3B33),
            30 to Color(0xFF4B463E),
            35 to Color(0xFF565249),
            40 to Color(0xFF635E55),
            50 to Color(0xFF7C766D),
            60 to Color(0xFF969086),
            70 to Color(0xFFB1AAA0),
            80 to Color(0xFFCDC5BB),
            87 to Color(0xFFE1D9CE),
            90 to Color(0xFFE9E1D6),
            92 to Color(0xFFEFE7DC),
            94 to Color(0xFFF5EDE1),
            95 to Color(0xFFF8F0E4),
            96 to Color(0xFFFBF2E7),
            98 to Color(0xFFFFF8F1),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** Warm paper, slightly stronger — outlines and secondary text (hue 90, chroma 10). */
    val neutralVariant = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF130E03),
            6 to Color(0xFF181306),
            10 to Color(0xFF211B0D),
            12 to Color(0xFF251F11),
            15 to Color(0xFF2B2516),
            17 to Color(0xFF30291A),
            20 to Color(0xFF363020),
            22 to Color(0xFF3B3424),
            24 to Color(0xFF3F3828),
            25 to Color(0xFF423B2A),
            30 to Color(0xFF4D4635),
            35 to Color(0xFF5A5240),
            40 to Color(0xFF665E4B),
            50 to Color(0xFF7F7663),
            60 to Color(0xFF9A907B),
            70 to Color(0xFFB5AA95),
            80 to Color(0xFFD1C5AF),
            87 to Color(0xFFE5D9C2),
            90 to Color(0xFFEDE1CA),
            92 to Color(0xFFF3E7D0),
            94 to Color(0xFFF9EDD5),
            95 to Color(0xFFFCEFD8),
            96 to Color(0xFFFFF2DB),
            98 to Color(0xFFFFF8F1),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** Error red, Material's standard error hue. */
    val error = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF280001),
            6 to Color(0xFF310001),
            10 to Color(0xFF410002),
            12 to Color(0xFF490002),
            15 to Color(0xFF540003),
            17 to Color(0xFF5C0004),
            20 to Color(0xFF690005),
            22 to Color(0xFF710005),
            24 to Color(0xFF790006),
            25 to Color(0xFF7E0007),
            30 to Color(0xFF93000A),
            35 to Color(0xFFA80710),
            40 to Color(0xFFBA1A1A),
            50 to Color(0xFFDE3730),
            60 to Color(0xFFFF5449),
            70 to Color(0xFFFF897D),
            80 to Color(0xFFFFB4AB),
            87 to Color(0xFFFFCFC9),
            90 to Color(0xFFFFDAD6),
            92 to Color(0xFFFFE2DE),
            94 to Color(0xFFFFE9E6),
            95 to Color(0xFFFFEDEA),
            96 to Color(0xFFFFF0EE),
            98 to Color(0xFFFFF8F7),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )
}

package dev.sadakat.qandeel.core.designsystem.ref

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Reference tokens: the tonal palettes every color role is picked from. Generated in the HCT
 * color space (Material Color Utilities) from the brand seeds, so a tone means the same perceived
 * lightness in every palette: tone 0 is black, 100 white, and tone contrast predicts text contrast.
 * The non-brand palettes (Material 3's baseline, the sepia page) live in the generated region at
 * the bottom, written by scripts/build_palettes.py.
 *
 * Only theme code (the semantic [dev.sadakat.qandeel.core.designsystem.color.QandeelColors] builders and the
 * apps' MaterialTheme mappings) reads these; UI code uses semantic roles. This file is the only one
 * in the codebase allowed to spell out color literals.
 */
@Immutable
class TonalPalette internal constructor(private val tones: Map<Int, Color>) {
    /** The color at [tone] (0..100); only the generated tones exist. */
    operator fun get(tone: Int): Color = requireNotNull(tones[tone]) { "Tone $tone is not generated" }

    /** The tones this palette carries, ascending. */
    val generatedTones: List<Int>
        get() = tones.keys.sorted()
}

object QandeelPalettes {
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

    // region generated by scripts/build_palettes.py

    /** Material 3's baseline purple — its seed #6750A4 (hue 299, chroma 48). */
    val baselinePrimary = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF13003A),
            6 to Color(0xFF190048),
            10 to Color(0xFF22005D),
            12 to Color(0xFF260561),
            15 to Color(0xFF2D1067),
            17 to Color(0xFF31166C),
            20 to Color(0xFF381E72),
            22 to Color(0xFF3C2377),
            24 to Color(0xFF41287C),
            25 to Color(0xFF432B7E),
            30 to Color(0xFF4F378A),
            35 to Color(0xFF5B4397),
            40 to Color(0xFF6750A4),
            50 to Color(0xFF8069BF),
            60 to Color(0xFF9A83DB),
            70 to Color(0xFFB69DF8),
            80 to Color(0xFFCFBCFF),
            87 to Color(0xFFE1D3FF),
            90 to Color(0xFFE9DDFF),
            92 to Color(0xFFEEE4FF),
            94 to Color(0xFFF3EAFF),
            95 to Color(0xFFF6EEFF),
            96 to Color(0xFFF8F1FF),
            98 to Color(0xFFFDF7FF),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** The baseline's quieter purple for secondary containers (hue 299, chroma 16). */
    val baselineSecondary = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF100B1D),
            6 to Color(0xFF151022),
            10 to Color(0xFF1E192B),
            12 to Color(0xFF221D2F),
            15 to Color(0xFF282336),
            17 to Color(0xFF2C273A),
            20 to Color(0xFF332D41),
            22 to Color(0xFF373245),
            24 to Color(0xFF3C364A),
            25 to Color(0xFF3E384C),
            30 to Color(0xFF4A4458),
            35 to Color(0xFF564F64),
            40 to Color(0xFF625B71),
            50 to Color(0xFF7B748A),
            60 to Color(0xFF958DA4),
            70 to Color(0xFFB0A7C0),
            80 to Color(0xFFCBC2DB),
            87 to Color(0xFFDFD6EF),
            90 to Color(0xFFE8DEF8),
            92 to Color(0xFFEEE4FE),
            94 to Color(0xFFF3EAFF),
            95 to Color(0xFFF6EEFF),
            96 to Color(0xFFF8F1FF),
            98 to Color(0xFFFDF7FF),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** The baseline's counterpoint — the seed hue rotated 60 degrees (hue 359, chroma 24). */
    val baselineTertiary = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF210410),
            6 to Color(0xFF270815),
            10 to Color(0xFF31101D),
            12 to Color(0xFF361421),
            15 to Color(0xFF3D1B27),
            17 to Color(0xFF421F2C),
            20 to Color(0xFF4A2532),
            22 to Color(0xFF4F2936),
            24 to Color(0xFF542E3B),
            25 to Color(0xFF56303D),
            30 to Color(0xFF633B48),
            35 to Color(0xFF704654),
            40 to Color(0xFF7E5260),
            50 to Color(0xFF996A79),
            60 to Color(0xFFB58392),
            70 to Color(0xFFD29DAD),
            80 to Color(0xFFEFB8C8),
            87 to Color(0xFFFFCDDB),
            90 to Color(0xFFFFD9E3),
            92 to Color(0xFFFFE1E8),
            94 to Color(0xFFFFE8ED),
            95 to Color(0xFFFFECF0),
            96 to Color(0xFFFFF0F2),
            98 to Color(0xFFFFF8F8),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** The baseline's surfaces and body text (hue 299, chroma 4). */
    val baselineNeutral = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF0F0E11),
            6 to Color(0xFF141316),
            10 to Color(0xFF1C1B1E),
            12 to Color(0xFF201F22),
            15 to Color(0xFF272529),
            17 to Color(0xFF2B292D),
            20 to Color(0xFF313033),
            22 to Color(0xFF363438),
            24 to Color(0xFF3A383C),
            25 to Color(0xFF3D3B3E),
            30 to Color(0xFF48464A),
            35 to Color(0xFF545156),
            40 to Color(0xFF605D62),
            50 to Color(0xFF79767A),
            60 to Color(0xFF938F94),
            70 to Color(0xFFAEAAAE),
            80 to Color(0xFFCAC5CA),
            87 to Color(0xFFDDD8DD),
            90 to Color(0xFFE6E1E6),
            92 to Color(0xFFECE7EB),
            94 to Color(0xFFF2ECF1),
            95 to Color(0xFFF4EFF4),
            96 to Color(0xFFF7F2F7),
            98 to Color(0xFFFDF8FD),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** The baseline's outlines and secondary text (hue 299, chroma 8). */
    val baselineNeutralVariant = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF0F0D14),
            6 to Color(0xFF14121A),
            10 to Color(0xFF1D1A22),
            12 to Color(0xFF211E26),
            15 to Color(0xFF27242D),
            17 to Color(0xFF2B2931),
            20 to Color(0xFF322F38),
            22 to Color(0xFF36333C),
            24 to Color(0xFF3B3840),
            25 to Color(0xFF3D3A43),
            30 to Color(0xFF49454E),
            35 to Color(0xFF54515A),
            40 to Color(0xFF615D66),
            50 to Color(0xFF7A757F),
            60 to Color(0xFF948F99),
            70 to Color(0xFFAFA9B4),
            80 to Color(0xFFCAC4CF),
            87 to Color(0xFFDED8E3),
            90 to Color(0xFFE7E0EB),
            92 to Color(0xFFECE6F1),
            94 to Color(0xFFF2EBF7),
            95 to Color(0xFFF5EEFA),
            96 to Color(0xFFF8F1FD),
            98 to Color(0xFFFDF7FF),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** Sepia paper and ink — the warm page of a classic e-reader (hue 95, chroma 8; tone 94 is near #F4ECD8). */
    val sepiaNeutral = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF110E05),
            6 to Color(0xFF161309),
            10 to Color(0xFF1F1B10),
            12 to Color(0xFF231F14),
            15 to Color(0xFF29261A),
            17 to Color(0xFF2E2A1E),
            20 to Color(0xFF343024),
            22 to Color(0xFF393428),
            24 to Color(0xFF3D392C),
            25 to Color(0xFF403B2E),
            30 to Color(0xFF4B4639),
            35 to Color(0xFF575244),
            40 to Color(0xFF645E50),
            50 to Color(0xFF7D7767),
            60 to Color(0xFF979080),
            70 to Color(0xFFB2AB9A),
            80 to Color(0xFFCEC6B4),
            84 to Color(0xFFD9D1BF),
            85 to Color(0xFFDCD4C2),
            86 to Color(0xFFDFD6C4),
            87 to Color(0xFFE2D9C7),
            88 to Color(0xFFE5DCCA),
            90 to Color(0xFFEAE2CF),
            92 to Color(0xFFF0E7D5),
            94 to Color(0xFFF6EDDA),
            95 to Color(0xFFF9F0DD),
            96 to Color(0xFFFCF3E0),
            98 to Color(0xFFFFF8EF),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    /** Sepia paper, slightly stronger — outlines and secondary text (hue 95, chroma 16). */
    val sepiaNeutralVariant = TonalPalette(
        mapOf(
            0 to Color(0xFF000000),
            4 to Color(0xFF130E00),
            6 to Color(0xFF191301),
            10 to Color(0xFF221B04),
            12 to Color(0xFF261F07),
            15 to Color(0xFF2C250C),
            17 to Color(0xFF312910),
            20 to Color(0xFF383016),
            22 to Color(0xFF3C341A),
            24 to Color(0xFF41391E),
            25 to Color(0xFF433B20),
            30 to Color(0xFF4F462A),
            35 to Color(0xFF5B5235),
            40 to Color(0xFF675E40),
            50 to Color(0xFF817657),
            60 to Color(0xFF9C906E),
            70 to Color(0xFFB7AB87),
            80 to Color(0xFFD3C6A1),
            84 to Color(0xFFDED1AB),
            85 to Color(0xFFE1D4AE),
            86 to Color(0xFFE4D6B1),
            87 to Color(0xFFE7D9B3),
            88 to Color(0xFFEADCB6),
            90 to Color(0xFFF0E2BB),
            92 to Color(0xFFF6E7C1),
            94 to Color(0xFFFBEDC6),
            95 to Color(0xFFFEF0C9),
            96 to Color(0xFFFFF3D5),
            98 to Color(0xFFFFF8EF),
            99 to Color(0xFFFFFBFF),
            100 to Color(0xFFFFFFFF),
        ),
    )

    // endregion generated by scripts/build_palettes.py
}

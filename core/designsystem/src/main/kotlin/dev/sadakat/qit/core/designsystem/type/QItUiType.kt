package dev.sadakat.qit.core.designsystem.type

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Latin type scale, named after Material's roles so the phone maps it 1:1. Headings use the
 * serif, everything read in passing (titles of rows, body, labels) the sans.
 */
@Immutable
data class QItUiType(
    val displayLarge: TextStyle = heading(57, 64),
    val displayMedium: TextStyle = heading(45, 52),
    val displaySmall: TextStyle = heading(36, 44),
    val headlineLarge: TextStyle = heading(32, 40),
    val headlineMedium: TextStyle = heading(28, 36),
    val headlineSmall: TextStyle = heading(24, 32),
    val titleLarge: TextStyle = heading(22, 28),
    val titleMedium: TextStyle = body(16, 24, FontWeight.Medium, letterSpacing = 0.15),
    val titleSmall: TextStyle = body(14, 20, FontWeight.Medium, letterSpacing = 0.1),
    val bodyLarge: TextStyle = body(16, 24, letterSpacing = 0.5),
    val bodyMedium: TextStyle = body(14, 20, letterSpacing = 0.25),
    val bodySmall: TextStyle = body(12, 16, letterSpacing = 0.4),
    val labelLarge: TextStyle = body(14, 20, FontWeight.Medium, letterSpacing = 0.1),
    val labelMedium: TextStyle = body(12, 16, FontWeight.Medium, letterSpacing = 0.5),
    val labelSmall: TextStyle = body(11, 16, FontWeight.Medium, letterSpacing = 0.5),
) {
    companion object {
        /** The brand's scale: serif headings for a printed-page feel. The same as `QItUiType()`. */
        fun mushaf(): QItUiType = scale(QItFonts.Heading, headingWeight = FontWeight.Normal, strong = FontWeight.Medium)

        /** Material's baseline: the platform sans throughout, headings at [headingWeight]. */
        fun sans(headingWeight: FontWeight = FontWeight.Normal): QItUiType =
            scale(QItFonts.Body, headingWeight, strong = FontWeight.Medium)

        /** Material 3 Expressive's emphasized styles: the sans, with heavier headings, titles and labels. */
        fun emphasized(): QItUiType =
            scale(QItFonts.Body, headingWeight = FontWeight.Medium, strong = FontWeight.SemiBold)

        private fun scale(headingFamily: FontFamily, headingWeight: FontWeight, strong: FontWeight) = QItUiType(
            displayLarge = heading(57, 64, headingFamily, headingWeight),
            displayMedium = heading(45, 52, headingFamily, headingWeight),
            displaySmall = heading(36, 44, headingFamily, headingWeight),
            headlineLarge = heading(32, 40, headingFamily, headingWeight),
            headlineMedium = heading(28, 36, headingFamily, headingWeight),
            headlineSmall = heading(24, 32, headingFamily, headingWeight),
            titleLarge = heading(22, 28, headingFamily, headingWeight),
            titleMedium = body(16, 24, strong, letterSpacing = 0.15),
            titleSmall = body(14, 20, strong, letterSpacing = 0.1),
            bodyLarge = body(16, 24, letterSpacing = 0.5),
            bodyMedium = body(14, 20, letterSpacing = 0.25),
            bodySmall = body(12, 16, letterSpacing = 0.4),
            labelLarge = body(14, 20, strong, letterSpacing = 0.1),
            labelMedium = body(12, 16, strong, letterSpacing = 0.5),
            labelSmall = body(11, 16, strong, letterSpacing = 0.5),
        )
    }
}

private fun heading(
    size: Int,
    lineHeight: Int,
    family: FontFamily = QItFonts.Heading,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private fun body(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal, letterSpacing: Double) = TextStyle(
    fontFamily = QItFonts.Body,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

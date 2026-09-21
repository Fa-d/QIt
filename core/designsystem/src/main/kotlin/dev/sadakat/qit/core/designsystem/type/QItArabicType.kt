package dev.sadakat.qit.core.designsystem.type

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/**
 * The Quran's Arabic, in Amiri Quran. Line heights are generous (about 1.8×) because stacked marks
 * above and below the letters need the room, and [LineHeightStyle.Trim.None] keeps the first and last
 * lines from clipping them.
 *
 * [display], [body] and [title] follow the reader's text-size setting ([scaled]); [label] and the
 * watch styles are fixed, because rows and a watch face have no room to grow.
 */
@Immutable
data class QItArabicType(
    /** The ayah in the full-screen player. */
    val display: TextStyle = arabic(34, 64),
    /** Ayahs in the reader. */
    val body: TextStyle = arabic(26, 50),
    /** The basmala and the continue card. */
    val title: TextStyle = arabic(24, 44),
    /** Surah names in lists. */
    val label: TextStyle = arabic(18, 32),
    val watchBody: TextStyle = arabic(20, 36),
    val watchTitle: TextStyle = arabic(16, 28),
) {
    /** The reading styles multiplied by [factor]; 1 returns this. */
    fun scaled(factor: Float): QItArabicType = if (factor == 1f) {
        this
    } else {
        copy(display = display.scaled(factor), body = body.scaled(factor), title = title.scaled(factor))
    }
}

private fun arabic(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = QItFonts.AmiriQuran,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    textDirection = TextDirection.Rtl,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

private fun TextStyle.scaled(factor: Float) = copy(fontSize = fontSize * factor, lineHeight = lineHeight * factor)

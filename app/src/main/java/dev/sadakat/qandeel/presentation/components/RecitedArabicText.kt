package dev.sadakat.qandeel.presentation.components

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.domain.model.ArabicWords
import dev.sadakat.qandeel.core.domain.player.WordPointer

/**
 * An ayah's Arabic with the word pointer: words already recited in full ink, the word being recited
 * on a solid pill, words still to come quieter. While the translation is read the whole Arabic steps
 * back. The pill is drawn behind the text and only colors change, so the text never reflows as the
 * pointer moves.
 *
 * With [keepCurrentLineInView], the line holding the current word is scrolled into view by the
 * nearest scrolling parent (long ayahs outgrow the screen). [onHighlight] picks the colors for the
 * reader's highlighted, reciting ayah; [recitedColor] overrides the full ink (e.g. while it animates).
 */
@Composable
fun RecitedArabicText(
    text: String,
    pointer: WordPointer,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    onHighlight: Boolean = false,
    keepCurrentLineInView: Boolean = false,
    recitedColor: Color = Color.Unspecified,
) {
    val colors = QandeelTheme.colors
    val ink = PointerInk(
        recited = recitedColor.takeOrElse { if (onHighlight) colors.onPlayingAyahHighlight else colors.arabicText },
        current = colors.onCurrentWordHighlight,
        upcoming = if (onHighlight) colors.upcomingWordOnHighlight else colors.upcomingWord,
    )
    val pill = colors.currentWordHighlight
    val pillRadius = QandeelTheme.radius.sm
    val words = remember(text) { ArabicWords.ranges(text) }
    val currentWord = (pointer as? WordPointer.Reciting)?.let { words.getOrNull(it.word) }
    // Keyed on the pointer's own inks: the full ink is the Text's color, so animating it rebuilds nothing.
    val annotated = remember(text, words, pointer, ink.current, ink.upcoming) { pointedText(text, words, pointer, ink) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val requester = remember { BringIntoViewRequester() }

    if (keepCurrentLineInView) {
        LaunchedEffect(pointer, layout, words) {
            val word = (pointer as? WordPointer.Reciting)?.word ?: return@LaunchedEffect
            val textLayout = layout ?: return@LaunchedEffect
            val range = words.getOrNull(word) ?: return@LaunchedEffect
            val line = textLayout.getLineForOffset(range.first)
            requester.bringIntoView(
                Rect(0f, textLayout.getLineTop(line), textLayout.size.width.toFloat(), textLayout.getLineBottom(line)),
            )
        }
    }
    Text(
        text = annotated,
        style = style,
        color = ink.recited,
        textAlign = textAlign,
        onTextLayout = { layout = it },
        modifier = modifier
            .bringIntoViewRequester(requester)
            .drawBehind {
                val textLayout = layout ?: return@drawBehind
                // A layout of the previous text can outlive it by a frame.
                val range = currentWord?.takeIf { it.last < textLayout.layoutInput.text.length } ?: return@drawBehind
                val radius = CornerRadius(pillRadius.toPx())
                for (rect in wordPills(textLayout, range, style.fontSize.toPx())) {
                    drawRoundRect(color = pill, topLeft = rect.topLeft, size = rect.size, cornerRadius = radius)
                }
            },
    )
}

/**
 * Where the pill behind the word at [range] goes: one rect per line the word is on (a pause mark
 * after a space can wrap to the next line), a little wider than the glyphs and as tall as the
 * stacked marks reach.
 */
private fun wordPills(layout: TextLayoutResult, range: IntRange, fontSizePx: Float): List<Rect> {
    val end = range.last + 1
    val padding = fontSizePx * PILL_PADDING_EM
    return (layout.getLineForOffset(range.first)..layout.getLineForOffset(range.last)).mapNotNull { line ->
        val start = maxOf(range.first, layout.getLineStart(line))
        val lineEnd = minOf(end, layout.getLineEnd(line, visibleEnd = true))
        if (start >= lineEnd) return@mapNotNull null
        val glyphs = layout.getPathForRange(start, lineEnd).getBounds()
        val baseline = layout.getLineBaseline(line)
        Rect(
            left = glyphs.left - padding,
            top = baseline - fontSizePx * PILL_ASCENT_EM,
            right = glyphs.right + padding,
            bottom = baseline + fontSizePx * PILL_DESCENT_EM,
        )
    }
}

/** How far the pill reaches past the word's glyphs on each side, in ems: less than half a space. */
private const val PILL_PADDING_EM = 0.18f

/**
 * How far the pill reaches above and below the baseline, in ems: Amiri Quran's letters sit low in
 * the tall line, with marks stacked above them and a kasra below.
 */
private const val PILL_ASCENT_EM = 1.15f
private const val PILL_DESCENT_EM = 0.6f

/** The pointer's three inks; the current word's is the one on its pill. */
private data class PointerInk(val recited: Color, val current: Color, val upcoming: Color)

private fun pointedText(text: String, words: List<IntRange>, pointer: WordPointer, ink: PointerInk): AnnotatedString =
    when (pointer) {
        WordPointer.Off -> AnnotatedString(text)

        WordPointer.Translating -> buildAnnotatedString { withStyle(SpanStyle(color = ink.upcoming)) { append(text) } }

        is WordPointer.Reciting -> buildAnnotatedString {
            append(text)
            words.forEachIndexed { index, range ->
                val color = when {
                    index < pointer.word -> return@forEachIndexed

                    // recited: the text's own ink
                    index == pointer.word -> ink.current

                    else -> ink.upcoming
                }
                addStyle(SpanStyle(color = color), range.first, range.last + 1)
            }
        }
    }

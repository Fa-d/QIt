package dev.sadakat.qit.presentation.components

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
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.model.ArabicWords
import dev.sadakat.qit.core.domain.player.WordPointer

/**
 * An ayah's Arabic with the word pointer: words already recited in full ink, the word being recited
 * in the recitation color, words still to come quieter. While the translation is read the whole
 * Arabic steps back. Only colors change, so the text never reflows as the pointer moves.
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
    val colors = QItTheme.colors
    val ink = PointerInk(
        recited = recitedColor.takeOrElse { if (onHighlight) colors.onPlayingAyahHighlight else colors.arabicText },
        current = if (onHighlight) colors.currentWordOnHighlight else colors.currentWord,
        upcoming = if (onHighlight) colors.upcomingWordOnHighlight else colors.upcomingWord,
    )
    val words = remember(text) { ArabicWords.ranges(text) }
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
        modifier = modifier.bringIntoViewRequester(requester),
    )
}

/** The pointer's three inks. */
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

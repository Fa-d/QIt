package dev.sadakat.qandeel.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.designsystem.component.ReaderTokens
import dev.sadakat.qandeel.core.domain.model.ArabicWords
import dev.sadakat.qandeel.core.domain.player.WordPointer

/**
 * An ayah's Arabic word by word, each word over its meaning, flowing right to left. The word pointer
 * lights them as [RecitedArabicText] does: words already recited in full ink, the word being recited
 * with its meaning on the solid pill, words still to come quieter. While the translation is read the
 * whole ayah steps back.
 *
 * [meanings] holds one meaning per word of [text] as [ArabicWords] splits it; a missing one leaves
 * its word alone. With [keepCurrentWordInView] the word being recited is scrolled into view by the
 * nearest scrolling parent. [onHighlight] and [recitedColor] work as in [RecitedArabicText].
 *
 * With [onWordClick] a tap on a word reports its index (to play from it); a long press then goes to
 * [onWordLongPress], so pressing a word does what pressing its ayah does.
 */
@Composable
fun WordByWordText(
    text: String,
    meanings: List<String>,
    pointer: WordPointer,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onHighlight: Boolean = false,
    keepCurrentWordInView: Boolean = false,
    recitedColor: Color = Color.Unspecified,
    onWordClick: ((Int) -> Unit)? = null,
    onWordLongPress: () -> Unit = {},
) {
    val colors = QandeelTheme.colors
    val recited = recitedColor.takeOrElse { if (onHighlight) colors.onPlayingAyahHighlight else colors.arabicText }
    val upcoming = if (onHighlight) colors.upcomingWordOnHighlight else colors.upcomingWord
    val meaningInk = if (onHighlight) colors.onPlayingAyahHighlight else colors.translationText
    val words = remember(text) { ArabicWords.ranges(text).map { text.substring(it) } }
    val current = (pointer as? WordPointer.Reciting)?.word

    // Right to left like the Arabic itself: the first word starts the row at the right edge.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(QandeelTheme.spacing.xxs),
            verticalArrangement = Arrangement.spacedBy(QandeelTheme.spacing.xs),
        ) {
            words.forEachIndexed { index, word ->
                val ink = when {
                    pointer == WordPointer.Translating -> CellInk(upcoming, upcoming)
                    current == null || index < current -> CellInk(recited, meaningInk)
                    index == current -> CellInk(colors.onCurrentWordHighlight, colors.onCurrentWordHighlight)
                    else -> CellInk(upcoming, upcoming)
                }
                WordCell(
                    word = word,
                    meaning = meanings.getOrNull(index),
                    ink = ink,
                    style = style,
                    isCurrent = index == current,
                    keepInView = keepCurrentWordInView && index == current,
                    modifier = if (onWordClick == null) {
                        Modifier
                    } else {
                        // A gesture, not clickable: a word can be narrower than a touch target, and
                        // the ayah around it is the accessible target (it plays from the ayah).
                        Modifier.pointerInput(index, onWordClick) {
                            detectTapGestures(onTap = { onWordClick(index) }, onLongPress = { onWordLongPress() })
                        }
                    },
                )
            }
        }
    }
}

/** A word's two inks: its Arabic and its meaning. */
private data class CellInk(val word: Color, val meaning: Color)

@Composable
private fun WordCell(
    word: String,
    meaning: String?,
    ink: CellInk,
    style: TextStyle,
    isCurrent: Boolean,
    keepInView: Boolean,
    modifier: Modifier = Modifier,
) {
    val requester = remember { BringIntoViewRequester() }
    if (keepInView) LaunchedEffect(Unit) { requester.bringIntoView() }
    val pill = QandeelTheme.colors.currentWordHighlight
    val shape = RoundedCornerShape(QandeelTheme.radius.sm)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .bringIntoViewRequester(requester)
            .background(if (isCurrent) pill else Color.Transparent, shape)
            .padding(horizontal = QandeelTheme.spacing.xs, vertical = QandeelTheme.spacing.xxs),
    ) {
        Text(text = word, style = style, color = ink.word)
        if (meaning != null) {
            Text(
                text = meaning,
                // English and Bangla read left to right; inside the right-to-left row a leading
                // "(is)" would otherwise be moved to the end.
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                color = ink.meaning,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = ReaderTokens.WordMeaningMaxWidth)
                    // Clear of the marks some words carry low under their letters.
                    .padding(top = QandeelTheme.spacing.xs, bottom = QandeelTheme.spacing.xxs),
            )
        }
    }
}

package dev.sadakat.qandeel.presentation.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.domain.model.Ayah
import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.player.WordPointer
import dev.sadakat.qandeel.presentation.components.NumberBadge
import dev.sadakat.qandeel.presentation.components.RecitedArabicText
import dev.sadakat.qandeel.presentation.components.WordByWordText

/** Semantics flag marking the ayah that is currently playing. */
val AyahIsPlaying = SemanticsPropertyKey<Boolean>("AyahIsPlaying")

var SemanticsPropertyReceiver.ayahIsPlaying by AyahIsPlaying

/**
 * One ayah of the reader: its number in the octagram (with how many times it was heard under it),
 * the Arabic (right-aligned, sized by the reading setting) and the mode's translation when one is
 * shown. With [wordMeanings] the Arabic is laid out word by word, each word over its meaning. The
 * reciting ayah is highlighted and carries the word [pointer]; with [followWords] its recited line
 * is kept on screen. A deep link pulses the same gold wash once so the eye finds the landed-on ayah.
 *
 * A tap plays from the ayah ([onClick]); a long press opens its options ([onLongClick]), which a
 * screen reader also offers as [accessibilityActions]. In word by word, [onWordClick] plays from the
 * tapped word.
 */
@Composable
fun AyahItem(
    ayah: Ayah,
    translationTrack: Track?,
    isPlaying: Boolean,
    pulsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    onWordClick: ((Int) -> Unit)? = null,
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
    pointer: WordPointer = WordPointer.Off,
    heardTimes: Int = 0,
    followWords: Boolean = false,
    wordMeanings: List<String>? = null,
) {
    val colors = QandeelTheme.colors
    val motion = QandeelTheme.motion
    val inks = animatedInks(isPlaying = isPlaying, translating = isPlaying && pointer == WordPointer.Translating)
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(pulsed) {
        if (pulsed) {
            pulse.snapTo(PULSE_ALPHA)
            pulse.animateTo(0f, tween(motion.durationLong, easing = motion.easingStandard))
        }
    }
    // The highlight is colour alone; announce the state so it isn't colour-only information.
    val recitingState = stringResource(R.string.reciting_ayah_state)
    val playLabel = stringResource(R.string.ayah_cd_play_from_here)
    val optionsLabel = stringResource(R.string.ayah_cd_options)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ayah_${ayah.number}")
            .semantics {
                ayahIsPlaying = isPlaying
                if (isPlaying) stateDescription = recitingState
                if (accessibilityActions.isNotEmpty()) customActions = accessibilityActions
            }
            .clip(RoundedCornerShape(QandeelTheme.radius.lg))
            .background(inks.background)
            .background(colors.tertiaryContainer.copy(alpha = pulse.value))
            .combinedClickable(
                onClickLabel = playLabel,
                onLongClickLabel = optionsLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = QandeelTheme.spacing.md, vertical = QandeelTheme.spacing.lg),
    ) {
        // The number sits beside the ayah, not on a line of its own: more of the surah fits on screen.
        Row {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NumberBadge(
                    number = ayah.number,
                    size = QandeelTheme.sizes.numberBadgeSmall,
                    label = stringResource(R.string.ayah_cd_number, ayah.number),
                )
                if (heardTimes > 0) HeardTimes(heardTimes, onHighlight = isPlaying)
            }
            Spacer(Modifier.width(QandeelTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                AyahArabic(
                    arabic = ayah.arabic,
                    wordMeanings = wordMeanings,
                    pointer = if (isPlaying) pointer else WordPointer.Off,
                    isPlaying = isPlaying,
                    color = inks.arabic,
                    followWords = isPlaying && followWords,
                    onWordClick = onWordClick,
                    onWordLongPress = onLongClick,
                )
                translationTrack?.let { track ->
                    ayah.translation(track)?.let { translation ->
                        Text(
                            text = translation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = inks.translation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = QandeelTheme.spacing.xs),
                        )
                    }
                }
            }
        }
    }
}

/** An ayah's colors, animated as it starts and stops being recited. */
private class AyahInks(val background: Color, val arabic: Color, val translation: Color)

@Composable
private fun animatedInks(isPlaying: Boolean, translating: Boolean): AyahInks {
    val colors = QandeelTheme.colors
    val motion = QandeelTheme.motion
    val background by animateColorAsState(
        targetValue = if (isPlaying) colors.playingAyahHighlight else Color.Transparent,
        animationSpec = motion.standard(),
        label = "ayahBackground",
    )
    val arabic by animateColorAsState(
        targetValue = if (isPlaying) colors.onPlayingAyahHighlight else colors.arabicText,
        animationSpec = motion.standard(),
        label = "ayahArabic",
    )
    val translation by animateColorAsState(
        targetValue = when {
            translating -> colors.currentWordOnHighlight
            isPlaying -> colors.onPlayingAyahHighlight
            else -> colors.translationText
        },
        animationSpec = motion.standard(),
        label = "ayahTranslation",
    )
    return AyahInks(background, arabic, translation)
}

/** The ayah's Arabic: word by word over [wordMeanings] when there are any, else as one flowing text. */
@Composable
private fun AyahArabic(
    arabic: String,
    wordMeanings: List<String>?,
    pointer: WordPointer,
    isPlaying: Boolean,
    color: Color,
    followWords: Boolean,
    onWordClick: ((Int) -> Unit)?,
    onWordLongPress: () -> Unit,
) {
    if (wordMeanings != null) {
        WordByWordText(
            text = arabic,
            meanings = wordMeanings,
            pointer = pointer,
            style = QandeelTheme.arabic.body,
            recitedColor = color,
            onHighlight = isPlaying,
            keepCurrentWordInView = followWords,
            onWordClick = onWordClick,
            onWordLongPress = onWordLongPress,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        RecitedArabicText(
            text = arabic,
            pointer = pointer,
            style = QandeelTheme.arabic.body,
            recitedColor = color,
            onHighlight = isPlaying,
            keepCurrentLineInView = followWords,
            // Right, not End: the Arabic styles set an RTL text direction, where End is the left edge.
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** "×12" under the ayah number: how many times it was heard. */
@Composable
private fun HeardTimes(times: Int, onHighlight: Boolean) {
    val description = pluralStringResource(R.plurals.ayah_heard_times, times, times)
    Text(
        text = stringResource(R.string.ayah_heard_short, times),
        style = MaterialTheme.typography.labelSmall,
        color = if (onHighlight) {
            QandeelTheme.colors.onPlayingAyahHighlight
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .padding(top = QandeelTheme.spacing.xxs)
            .semantics { contentDescription = description },
    )
}

private const val PULSE_ALPHA = 1f

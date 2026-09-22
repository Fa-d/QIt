package dev.sadakat.qit.presentation.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.presentation.components.NumberBadge
import dev.sadakat.qit.presentation.components.RecitedArabicText

/** Semantics flag marking the ayah that is currently playing. */
val AyahIsPlaying = SemanticsPropertyKey<Boolean>("AyahIsPlaying")

var SemanticsPropertyReceiver.ayahIsPlaying by AyahIsPlaying

/**
 * One ayah of the reader: its number in the octagram (with how many times it was heard under it),
 * the Arabic (right-aligned, sized by the reading setting) and the mode's translation when one is
 * shown. The reciting ayah is highlighted and carries the word [pointer]; with [followWords] its
 * recited line is kept on screen. A deep link pulses the same gold wash once so the eye finds the
 * landed-on ayah.
 */
@Composable
fun AyahItem(
    ayah: Ayah,
    translationTrack: Track?,
    isPlaying: Boolean,
    pulsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pointer: WordPointer = WordPointer.Off,
    heardTimes: Int = 0,
    followWords: Boolean = false,
) {
    val colors = QItTheme.colors
    val motion = QItTheme.motion
    val background by animateColorAsState(
        targetValue = if (isPlaying) colors.playingAyahHighlight else Color.Transparent,
        animationSpec = motion.standard(),
        label = "ayahBackground",
    )
    val arabicColor by animateColorAsState(
        targetValue = if (isPlaying) colors.onPlayingAyahHighlight else colors.arabicText,
        animationSpec = motion.standard(),
        label = "ayahArabic",
    )
    val translating = isPlaying && pointer == WordPointer.Translating
    val translationColor by animateColorAsState(
        targetValue = when {
            translating -> colors.currentWordOnHighlight
            isPlaying -> colors.onPlayingAyahHighlight
            else -> colors.translationText
        },
        animationSpec = motion.standard(),
        label = "ayahTranslation",
    )
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(pulsed) {
        if (pulsed) {
            pulse.snapTo(PULSE_ALPHA)
            pulse.animateTo(0f, tween(motion.durationLong, easing = motion.easingStandard))
        }
    }
    // The highlight is colour alone; announce the state so it isn't colour-only information.
    val recitingState = stringResource(R.string.reciting_ayah_state)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ayah_${ayah.number}")
            .semantics {
                ayahIsPlaying = isPlaying
                if (isPlaying) stateDescription = recitingState
            }
            .clip(RoundedCornerShape(QItTheme.radius.lg))
            .background(background)
            .background(colors.tertiaryContainer.copy(alpha = pulse.value))
            .clickable(onClick = onClick)
            .padding(horizontal = QItTheme.spacing.md, vertical = QItTheme.spacing.lg),
    ) {
        // The number sits beside the ayah, not on a line of its own: more of the surah fits on screen.
        Row {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NumberBadge(number = ayah.number, size = QItTheme.sizes.numberBadgeSmall)
                if (heardTimes > 0) HeardTimes(heardTimes, onHighlight = isPlaying)
            }
            Spacer(Modifier.width(QItTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                RecitedArabicText(
                    text = ayah.arabic,
                    pointer = if (isPlaying) pointer else WordPointer.Off,
                    style = QItTheme.arabic.body,
                    recitedColor = arabicColor,
                    onHighlight = isPlaying,
                    keepCurrentLineInView = isPlaying && followWords,
                    // Right, not End: the Arabic styles set an RTL text direction, where End is the left edge.
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                )
                translationTrack?.let { track ->
                    ayah.translation(track)?.let { translation ->
                        Text(
                            text = translation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = translationColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = QItTheme.spacing.xs),
                        )
                    }
                }
            }
        }
    }
}

/** "×12" under the ayah number: how many times it was heard. */
@Composable
private fun HeardTimes(times: Int, onHighlight: Boolean) {
    val description = pluralStringResource(R.plurals.ayah_heard_times, times, times)
    Text(
        text = stringResource(R.string.ayah_heard_short, times),
        style = MaterialTheme.typography.labelSmall,
        color = if (onHighlight) QItTheme.colors.onPlayingAyahHighlight else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = QItTheme.spacing.xxs)
            .semantics { contentDescription = description },
    )
}

private const val PULSE_ALPHA = 1f

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.presentation.components.NumberBadge

/** Semantics flag marking the ayah that is currently playing. */
val AyahIsPlaying = SemanticsPropertyKey<Boolean>("AyahIsPlaying")

var SemanticsPropertyReceiver.ayahIsPlaying by AyahIsPlaying

/**
 * One ayah of the reader: its number in the octagram, the Arabic (right-aligned, sized by the
 * reading setting) and the mode's translation when one is shown. The reciting ayah is highlighted;
 * a deep link pulses the same gold wash once so the eye finds the landed-on ayah.
 */
@Composable
fun AyahItem(
    ayah: Ayah,
    translationTrack: Track?,
    isPlaying: Boolean,
    pulsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
    val translationColor by animateColorAsState(
        targetValue = if (isPlaying) colors.onPlayingAyahHighlight else colors.translationText,
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
            NumberBadge(number = ayah.number, size = QItTheme.sizes.numberBadgeSmall)
            Spacer(Modifier.width(QItTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ayah.arabic,
                    style = QItTheme.arabic.body,
                    color = arabicColor,
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

private const val PULSE_ALPHA = 1f

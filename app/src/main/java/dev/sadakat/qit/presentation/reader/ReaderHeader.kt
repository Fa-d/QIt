package dev.sadakat.qit.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Revelation
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.model.SurahListening
import dev.sadakat.qit.core.ui.kit.QItSegmentedToggle
import dev.sadakat.qit.presentation.components.NumberBadge
import kotlin.math.roundToInt

/**
 * The reader's first item: the surah's identity (number, Arabic and English names), how far it has
 * been listened to, the basmala, the labelled recitation-mode selector and the play action —
 * everything one needs before the ayahs.
 */
@Composable
fun ReaderHeader(
    surah: Surah,
    mode: RecitationMode,
    onPlaySurah: () -> Unit,
    onModeChange: (RecitationMode) -> Unit,
    modifier: Modifier = Modifier,
    listening: SurahListening? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = QItTheme.spacing.sm,
                start = QItTheme.spacing.screenGutter,
                end = QItTheme.spacing.screenGutter,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            NumberBadge(number = surah.number)
            Spacer(Modifier.width(QItTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(
                        R.string.surah_meta_line,
                        surah.meaningEnglish,
                        pluralStringResource(R.plurals.ayah_count, surah.ayahCount, surah.ayahCount),
                        stringResource(surah.revelation.labelRes()),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(QItTheme.spacing.md))
            Text(
                text = surah.nameArabic,
                style = QItTheme.arabic.title,
                color = QItTheme.colors.arabicText,
            )
        }
        listening?.let { ListeningLine(it, Modifier.padding(top = QItTheme.spacing.md)) }
        if (QuranMeta.hasBasmalaPrefix(surah.number)) {
            Text(
                text = stringResource(R.string.basmala),
                style = QItTheme.arabic.title,
                color = QItTheme.colors.arabicText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = QItTheme.spacing.xl),
            )
        }
        ModeSelector(
            mode = mode,
            onModeChange = onModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = QItTheme.spacing.lg),
        )
        Button(
            onClick = onPlaySurah,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = QItTheme.spacing.lg),
        ) {
            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(QItTheme.spacing.xs))
            Text(stringResource(R.string.play_surah))
        }
        // Gold rule: where the front matter ends and the recited text begins.
        HorizontalDivider(
            modifier = Modifier.padding(top = QItTheme.spacing.xl),
            thickness = QItTheme.sizes.ornamentStroke,
            color = QItTheme.colors.ornament,
        )
    }
}

/** "Heard in full 2× · 64% into the next round", or "23 of 110 ayahs heard" before the first round. */
@Composable
private fun ListeningLine(listening: SurahListening, modifier: Modifier = Modifier) {
    val text = when {
        listening.rounds == 0 -> stringResource(R.string.reader_heard_ayahs, listening.ayahsHeard, listening.ayahCount)

        listening.ayahsIntoNextRound == 0 -> stringResource(R.string.reader_heard_rounds, listening.rounds)

        else -> stringResource(
            R.string.reader_heard_rounds_next,
            listening.rounds,
            (listening.nextRoundProgress * PERCENT).roundToInt(),
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag("reader_listening"),
    ) {
        Icon(
            Icons.Rounded.Headphones,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(QItTheme.sizes.iconSmall),
        )
        Spacer(Modifier.width(QItTheme.spacing.sm))
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private const val PERCENT = 100

/** The labelled recitation-mode selector: Arabic, with or without a translation after it. */
@Composable
private fun ModeSelector(mode: RecitationMode, onModeChange: (RecitationMode) -> Unit, modifier: Modifier = Modifier) {
    QItSegmentedToggle(
        options = RecitationMode.entries,
        selected = mode,
        onSelect = onModeChange,
        label = { stringResource(it.labelRes()) },
        modifier = modifier,
    )
}

private fun RecitationMode.labelRes(): Int = when (this) {
    RecitationMode.ARABIC_ONLY -> R.string.mode_arabic
    RecitationMode.ARABIC_ENGLISH -> R.string.mode_english
    RecitationMode.ARABIC_BANGLA -> R.string.mode_bangla
}

private fun Revelation.labelRes(): Int = when (this) {
    Revelation.MECCAN -> R.string.revelation_meccan
    Revelation.MEDINAN -> R.string.revelation_medinan
}

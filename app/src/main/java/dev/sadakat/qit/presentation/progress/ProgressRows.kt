package dev.sadakat.qit.presentation.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.sadakat.qit.R
import dev.sadakat.qit.core.designsystem.QItTheme
import dev.sadakat.qit.presentation.components.NumberBadge

/**
 * One heard surah: its number in the octagram, names, what's been heard and how far the round in
 * progress has come. Tapping it opens the surah in the reader.
 */
@Composable
internal fun ProgressRow(row: ProgressRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The row is one tap target; clickable merges its texts so TalkBack reads them as one sentence.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QItTheme.sizes.touchTarget)
            .testTag("progress_surah_${row.surah}")
            .clickable(onClick = onClick)
            .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberBadge(row.surah)
        Spacer(Modifier.width(QItTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = row.nameEnglish ?: stringResource(R.string.surah_fallback_name, row.surah),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(QItTheme.spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { row.nextRoundProgress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = QItTheme.colors.progressTrack,
                    gapSize = QItTheme.spacing.none,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(QItTheme.sizes.strokeThin),
                )
                Spacer(Modifier.width(QItTheme.spacing.sm))
                Text(
                    text = roundLabel(row),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        row.nameArabicShort?.let { arabic ->
            Spacer(Modifier.width(QItTheme.spacing.sm))
            Text(
                text = arabic,
                style = QItTheme.arabic.label,
                color = QItTheme.colors.arabicText,
                maxLines = 1,
            )
        }
    }
}

/** Completed rounds and listens, or — before the first full round — how many ayahs were heard. */
@Composable
private fun subtitle(row: ProgressRowUi): String = if (row.rounds >= 1) {
    val rounds = pluralStringResource(R.plurals.progress_full_rounds, row.rounds, row.rounds)
    val listens = pluralStringResource(R.plurals.progress_listens, row.totalListens, row.totalListens)
    stringResource(R.string.progress_rounds_listens, rounds, listens)
} else {
    stringResource(R.string.progress_ayahs_heard, row.ayahsHeard, row.ayahCount)
}

/** How far the round in progress has come: "64% heard" in round 1, "64% into round 3" after. */
@Composable
private fun roundLabel(row: ProgressRowUi): String = if (row.rounds >= 1) {
    stringResource(R.string.progress_into_round, percent(row.nextRoundProgress), row.rounds + ROUND_AHEAD)
} else {
    stringResource(R.string.progress_percent_heard, percent(row.nextRoundProgress))
}

private const val ROUND_AHEAD = 1

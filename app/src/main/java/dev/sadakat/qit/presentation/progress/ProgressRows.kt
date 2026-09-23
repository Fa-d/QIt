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
 * One heard surah: its number in the octagram, its names, what's been heard ("1 full round · 43%
 * into round 2 · 10 listens") and a bar of the round in progress, as wide as the text so the bars
 * line up down the list. Tapping it opens the surah in the reader.
 */
@Composable
internal fun ProgressRow(row: ProgressRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The row is one tap target; clickable merges its texts so TalkBack reads them as one sentence.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QItTheme.sizes.touchTarget)
            .testTag("progress_surah_${row.surah}")
            .clickable(onClickLabel = stringResource(R.string.cd_open_surah), onClick = onClick)
            .padding(horizontal = QItTheme.spacing.screenGutter, vertical = QItTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberBadge(row.surah, label = stringResource(R.string.home_cd_surah_number, row.surah))
        Spacer(Modifier.width(QItTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.nameEnglish ?: stringResource(R.string.surah_fallback_name, row.surah),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
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
            Text(
                text = subtitle(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { row.nextRoundProgress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = QItTheme.colors.progressTrack,
                gapSize = QItTheme.spacing.none,
                drawStopIndicator = {},
                modifier = Modifier
                    .padding(top = QItTheme.spacing.sm)
                    .fillMaxWidth()
                    .height(QItTheme.sizes.strokeThin),
            )
        }
    }
}

/**
 * What has been heard: the full rounds and how far the next one has come (or, before the first
 * round, how many ayahs), then the listens.
 */
@Composable
private fun subtitle(row: ProgressRowUi): String {
    val listens = pluralStringResource(R.plurals.progress_listens, row.totalListens, row.totalListens)
    val heard = when {
        row.rounds == 0 -> stringResource(R.string.progress_ayahs_heard, row.ayahsHeard, row.ayahCount)

        row.ayahsIntoNextRound == 0 -> pluralStringResource(R.plurals.progress_full_rounds, row.rounds, row.rounds)

        else -> stringResource(
            R.string.progress_rounds_listens,
            pluralStringResource(R.plurals.progress_full_rounds, row.rounds, row.rounds),
            stringResource(R.string.progress_into_round, percent(row.nextRoundProgress), row.rounds + ROUND_AHEAD),
        )
    }
    return stringResource(R.string.progress_rounds_listens, heard, listens)
}

private const val ROUND_AHEAD = 1

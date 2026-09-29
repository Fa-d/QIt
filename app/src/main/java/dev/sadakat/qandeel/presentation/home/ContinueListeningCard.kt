package dev.sadakat.qandeel.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.sadakat.qandeel.R
import dev.sadakat.qandeel.core.designsystem.QandeelTheme
import dev.sadakat.qandeel.core.designsystem.component.PlayerTokens
import dev.sadakat.qandeel.core.ui.kit.QandeelCard
import dev.sadakat.qandeel.core.ui.kit.QandeelEmphasis

/**
 * Where listening stands: surah, ayah and progress through the surah, with one tap to play or pause.
 * Tapping the card itself opens the reader there.
 */
@Composable
fun ContinueListeningCard(
    item: ContinueListeningUi,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QandeelCard(
        onClick = onOpen,
        emphasis = QandeelEmphasis.PRIMARY,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = QandeelTheme.spacing.screenGutter, vertical = QandeelTheme.spacing.sm)
            .testTag("continue_listening"),
    ) {
        Row(
            modifier = Modifier.padding(QandeelTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    // A restored session is queued but paused: that's still "continue", not "now playing".
                    text = stringResource(
                        if (item.isPlaying) R.string.home_now_playing else R.string.home_continue_listening,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(QandeelTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.home_card_position, item.surahName, item.surah, item.ayah),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.surahNameArabic,
                    style = QandeelTheme.arabic.label,
                    maxLines = 1,
                )
                Spacer(Modifier.height(QandeelTheme.spacing.sm))
                LinearProgressIndicator(
                    progress = { item.progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface,
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(QandeelTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.home_card_progress, item.ayah, item.ayahCount),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(QandeelTheme.spacing.lg))
            FilledIconButton(
                onClick = onPlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.size(PlayerTokens.CardPlayButtonSize),
            ) {
                Icon(
                    imageVector = if (item.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (item.isPlaying) R.string.cd_pause else R.string.cd_play),
                    modifier = Modifier.size(QandeelTheme.sizes.iconLarge),
                )
            }
        }
    }
}

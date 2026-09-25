package dev.sadakat.qit.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.sadakat.qit.R
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.ui.kit.QItMenu
import dev.sadakat.qit.core.ui.kit.QItTopBar
import dev.sadakat.qit.core.ui.kit.QItTopBarScroll
import dev.sadakat.qit.presentation.components.DownloadIndicator
import kotlin.math.roundToInt

/**
 * The reader's app bar: it hides while reading down and returns on scroll up ([scroll],
 * wired by the screen). Back, the surah's identity, the "Aa" reading-settings action and a labelled
 * overflow menu for the download and watch actions.
 */
@Composable
fun ReaderTopBar(
    surah: Surah?,
    scroll: QItTopBarScroll,
    onBack: () -> Unit,
    onOpenReadingSettings: () -> Unit,
    downloadState: SurahDownloadState,
    onDownload: () -> Unit,
    onRemoveClick: () -> Unit,
    onSendToWatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QItTopBar(
        title = {
            Column {
                Text(
                    text = surah?.nameEnglish.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (surah != null) {
                    Text(
                        text = surah.meaningEnglish,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenReadingSettings) {
                Icon(
                    imageVector = Icons.Rounded.FormatSize,
                    contentDescription = stringResource(R.string.cd_reading_settings),
                )
            }
            ReaderOverflowMenu(
                downloadState = downloadState,
                onDownload = onDownload,
                onRemoveClick = onRemoveClick,
                onSendToWatch = onSendToWatch,
            )
        },
        scroll = scroll,
        modifier = modifier,
    )
}

/** The reader's labelled actions: this surah's download, in whatever state it is, and the watch. */
@Composable
private fun ReaderOverflowMenu(
    downloadState: SurahDownloadState,
    onDownload: () -> Unit,
    onRemoveClick: () -> Unit,
    onSendToWatch: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
            )
        }
        QItMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DownloadMenuItem(
                state = downloadState,
                onDownload = {
                    expanded = false
                    onDownload()
                },
                onRemoveClick = {
                    expanded = false
                    onRemoveClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.send_to_watch)) },
                onClick = {
                    expanded = false
                    onSendToWatch()
                },
            )
        }
    }
}

/** One labelled item per download state, so the state and the action read as words, not icons. */
@Composable
private fun DownloadMenuItem(state: SurahDownloadState, onDownload: () -> Unit, onRemoveClick: () -> Unit) {
    when (state) {
        is SurahDownloadState.Downloading -> DropdownMenuItem(
            text = { Text(stringResource(R.string.downloading_percent, percent(state.progress))) },
            trailingIcon = { DownloadIndicator(state) },
            enabled = false,
            onClick = {},
        )

        is SurahDownloadState.Downloaded -> DropdownMenuItem(
            text = { Text(stringResource(R.string.remove_download)) },
            onClick = onRemoveClick,
        )

        is SurahDownloadState.Failed -> DropdownMenuItem(
            text = { Text(stringResource(R.string.retry_download)) },
            onClick = onDownload,
        )

        is SurahDownloadState.NotDownloaded -> DropdownMenuItem(
            text = { Text(stringResource(R.string.download_for_offline)) },
            onClick = onDownload,
        )
    }
}

private fun percent(fraction: Float): Int = (fraction * PERCENT).roundToInt()

private const val PERCENT = 100

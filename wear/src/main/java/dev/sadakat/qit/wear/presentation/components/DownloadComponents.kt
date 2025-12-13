package dev.sadakat.qit.wear.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import dev.sadakat.qit.wear.application.usecase.storage.StorageInfo

/**
 * Circular progress indicator for WearOS download progress
 */
@Composable
fun DownloadProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    showPercentage: Boolean = false
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(size),
            strokeWidth = 2.dp
        )

        if (showPercentage) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

/**
 * Chip that displays the download status of a song
 */
@Composable
fun DownloadStatusChip(
    downloadStatus: DownloadStatus,
    onDownloadClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (downloadStatus) {
        is DownloadStatus.NotDownloaded -> {
            Chip(
                label = {
                    Text(
                        text = "Download",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Download",
                        modifier = Modifier.size(ChipDefaults.IconSize)
                    )
                },
                onClick = onDownloadClick,
                colors = ChipDefaults.secondaryChipColors(),
                modifier = modifier.fillMaxWidth()
            )
        }

        is DownloadStatus.Downloading -> {
            Chip(
                label = {
                    Text(
                        text = "Downloading ${(downloadStatus.progress * 100).toInt()}%",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = {
                    DownloadProgressIndicator(
                        progress = downloadStatus.progress,
                        size = ChipDefaults.IconSize
                    )
                },
                secondaryLabel = {
                    Text("Tap to cancel")
                },
                onClick = onCancelClick,
                colors = ChipDefaults.primaryChipColors(),
                modifier = modifier.fillMaxWidth()
            )
        }

        is DownloadStatus.Downloaded -> {
            Chip(
                label = {
                    Text(
                        text = "Downloaded",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                onClick = { /* Already downloaded */ },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        modifier = Modifier.size(ChipDefaults.IconSize),
                        tint = MaterialTheme.colors.primary
                    )
                },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = modifier.fillMaxWidth(),
                enabled = false
            )
        }

        is DownloadStatus.Failed -> {
            Chip(
                label = {
                    Text(
                        text = "Failed",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Failed",
                        modifier = Modifier.size(ChipDefaults.IconSize),
                        tint = MaterialTheme.colors.error
                    )
                },
                secondaryLabel = {
                    Text("Tap to retry")
                },
                onClick = onRetryClick,
                colors = ChipDefaults.secondaryChipColors(),
                modifier = modifier.fillMaxWidth()
            )
        }

        is DownloadStatus.Paused -> {
            Chip(
                label = {
                    Text(
                        text = "Paused ${(downloadStatus.progress * 100).toInt()}%",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Paused",
                        modifier = Modifier.size(ChipDefaults.IconSize)
                    )
                },
                secondaryLabel = {
                    Text("Tap to resume")
                },
                onClick = onRetryClick,
                colors = ChipDefaults.secondaryChipColors(),
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Button that triggers download with state handling
 */
@Composable
fun DownloadButton(
    downloadStatus: DownloadStatus,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    when (downloadStatus) {
        is DownloadStatus.NotDownloaded -> {
            Button(
                onClick = onDownloadClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.primaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Download",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                )
            }
        }

        is DownloadStatus.Downloading -> {
            Button(
                onClick = onCancelClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.primaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Cancel",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                )
            }
        }

        is DownloadStatus.Downloaded -> {
            Button(
                onClick = {},
                modifier = modifier,
                enabled = false,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                )
            }
        }

        is DownloadStatus.Failed, is DownloadStatus.Paused -> {
            Button(
                onClick = onRetryClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.primaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                )
            }
        }
    }
}

/**
 * Linear progress bar showing storage usage
 */
@Composable
fun StorageUsageIndicator(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Storage",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant
            )
            Text(
                text = "${storageInfo.usedSpace.format()} / ${storageInfo.totalSpace.format()}",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant
            )
        }

        // Custom linear progress indicator for Wear
        val progressColor = when {
            storageInfo.usagePercentage > 0.9f -> MaterialTheme.colors.error
            storageInfo.usagePercentage > 0.7f -> Color.Yellow
            else -> MaterialTheme.colors.primary
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(storageInfo.usagePercentage.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(progressColor)
            )
        }

        Text(
            text = "${(storageInfo.usagePercentage * 100).toInt()}% used",
            style = MaterialTheme.typography.caption3,
            color = MaterialTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Compact download indicator icon for song items
 */
@Composable
fun DownloadIndicatorIcon(
    downloadStatus: DownloadStatus,
    modifier: Modifier = Modifier
) {
    when (downloadStatus) {
        is DownloadStatus.Downloading -> {
            DownloadProgressIndicator(
                progress = downloadStatus.progress,
                modifier = modifier,
                size = 20.dp
            )
        }

        is DownloadStatus.Downloaded -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Downloaded",
                modifier = modifier.size(20.dp),
                tint = MaterialTheme.colors.primary
            )
        }

        is DownloadStatus.Failed -> {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Download Failed",
                modifier = modifier.size(20.dp),
                tint = MaterialTheme.colors.error
            )
        }

        is DownloadStatus.Paused -> {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Paused",
                modifier = modifier.size(20.dp),
                tint = MaterialTheme.colors.onSurfaceVariant
            )
        }

        is DownloadStatus.NotDownloaded -> {
            // Don't show an icon for not downloaded
        }
    }
}

/**
 * Download item displaying song with download controls
 */
@Composable
fun DownloadItem(
    songTitle: String,
    artist: String,
    downloadStatus: DownloadStatus,
    fileSize: FileSize,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        label = {
            Text(
                text = songTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = fileSize.format(),
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
        },
        icon = {
            DownloadIndicatorIcon(
                downloadStatus = downloadStatus,
                modifier = Modifier.size(ChipDefaults.IconSize)
            )
        },
        onClick = {
            when (downloadStatus) {
                is DownloadStatus.NotDownloaded -> onDownloadClick()
                is DownloadStatus.Downloading -> onCancelClick()
                is DownloadStatus.Failed, is DownloadStatus.Paused -> onRetryClick()
                is DownloadStatus.Downloaded -> {} // No action for downloaded
            }
        },
        colors = when (downloadStatus) {
            is DownloadStatus.Downloading -> ChipDefaults.primaryChipColors()
            is DownloadStatus.Downloaded -> ChipDefaults.secondaryChipColors()
            else -> ChipDefaults.secondaryChipColors()
        },
        modifier = modifier.fillMaxWidth()
    )
}

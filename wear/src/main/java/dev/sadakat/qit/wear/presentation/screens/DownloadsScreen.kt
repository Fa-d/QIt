package dev.sadakat.qit.wear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import dev.sadakat.qit.wear.presentation.components.DownloadItem
import dev.sadakat.qit.wear.presentation.components.StorageUsageIndicator
import dev.sadakat.qit.wear.presentation.viewmodel.DownloadViewModel

/**
 * Screen for managing downloaded songs on WearOS
 */
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit = {},
    downloadViewModel: DownloadViewModel = hiltViewModel()
) {
    val downloadedSongs by downloadViewModel.downloadedSongs.collectAsState()
    val activeDownloads by downloadViewModel.activeDownloads.collectAsState()
    val storageInfo by downloadViewModel.storageInfo.collectAsState()
    val isLoading by downloadViewModel.isLoading.collectAsState()
    val downloadError by downloadViewModel.downloadError.collectAsState()

    val scalingLazyListState = rememberScalingLazyListState()

    if (isLoading && downloadedSongs.isEmpty()) {
        // Show loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            positionIndicator = {
                PositionIndicator(scalingLazyListState = scalingLazyListState)
            }
        ) {
            ScalingLazyColumn(
                state = scalingLazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            // Header
            item {
                Text(
                    text = "Downloads",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3
                )
            }

            // Storage usage indicator
            storageInfo?.let { storage ->
                item {
                    StorageUsageIndicator(
                        storageInfo = storage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Download summary
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${downloadViewModel.getDownloadedSongCount()} songs downloaded",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )

                    val activeCount = downloadViewModel.getActiveDownloadCount()
                    if (activeCount > 0) {
                        Text(
                            text = "$activeCount active downloads",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }

            // Show error if any
            downloadError?.let { error ->
                item {
                    Text(
                        text = error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.error
                    )
                }
            }

            // Active downloads section
            if (activeDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = "Downloading",
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }

                // Show songs currently downloading
                items(downloadedSongs.filter { song ->
                    activeDownloads.containsKey(song.id)
                }) { song ->
                    val progress = activeDownloads[song.id] ?: 0f
                    DownloadItem(
                        songTitle = song.title,
                        artist = song.artistName(),
                        downloadStatus = dev.sadakat.qit.shared.domain.valueobject.DownloadStatus.Downloading(
                            progress
                        ),
                        fileSize = song.fileSize,
                        onDownloadClick = {},
                        onCancelClick = {
                            downloadViewModel.cancelDownload(song.id)
                        },
                        onRetryClick = {}
                    )
                }
            }

            // Downloaded songs section
            if (downloadedSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "Downloaded Songs",
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }

                items(downloadedSongs.filter { song ->
                    song.downloadStatus.isDownloaded()
                }) { song ->
                    DownloadItem(
                        songTitle = song.title,
                        artist = song.artistName(),
                        downloadStatus = song.downloadStatus,
                        fileSize = song.fileSize,
                        onDownloadClick = {},
                        onCancelClick = {},
                        onRetryClick = {}
                    )
                }
            }

            // Empty state
            if (downloadedSongs.isEmpty() && activeDownloads.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No downloads yet",
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = "Download songs to listen offline",
                            style = MaterialTheme.typography.caption2,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }

            // Clear all downloads button (only show if there are downloads)
            if (downloadedSongs.isNotEmpty()) {
                item {
                    Button(
                        onClick = {
                            downloadViewModel.clearAllDownloads()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("Clear All")
                    }
                }
            }

            // Back button
            item {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text("Back")
                }
            }
            }
        }
    }
}

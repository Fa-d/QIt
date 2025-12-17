package dev.sadakat.qit.wear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.wear.presentation.viewmodel.MusicLibraryItem
import dev.sadakat.qit.wear.presentation.viewmodel.MusicLibraryViewModel

/**
 * Music Library screen showing all songs in a flat list grouped by playlist
 * Simplified navigation - tap song to play immediately
 */
@Composable
fun MusicLibraryScreen(
    onSongClick: (SongId) -> Unit,
    viewModel: MusicLibraryViewModel = hiltViewModel()
) {
    val libraryItems by viewModel.libraryItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scalingLazyListState = rememberScalingLazyListState()

    if (isLoading && libraryItems.isEmpty()) {
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = 32.dp  // Add extra bottom padding for round watches
                )
            ) {
                // Screen title
                item {
                    Text(
                        text = "Music Library",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.title3
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Empty state
                if (libraryItems.isEmpty() && !isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No music found",
                                style = MaterialTheme.typography.body2,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sync music from your phone",
                                style = MaterialTheme.typography.caption2,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }

                // Library items (headers and songs)
                items(libraryItems.size) { index ->
                    when (val item = libraryItems[index]) {
                        is MusicLibraryItem.PlaylistHeader -> {
                            // Playlist section header
                            Text(
                                text = item.playlist.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.caption1,
                                color = MaterialTheme.colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        is MusicLibraryItem.SongItem -> {
                            // Song chip
                            Chip(
                                label = {
                                    Text(
                                        text = item.song.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = { onSongClick(item.song.id) },
                                modifier = Modifier.fillMaxWidth(),
                                secondaryLabel = item.song.artist?.let { artist ->
                                    {
                                        Text(
                                            text = artist,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                icon = {
                                    // Show download status icon
                                    when (item.song.downloadStatus) {
                                        is DownloadStatus.Downloaded -> {
                                            Icon(
                                                Icons.Default.DownloadDone,
                                                contentDescription = "Downloaded",
                                                tint = MaterialTheme.colors.primary
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                Icons.Default.CloudDownload,
                                                contentDescription = "Stream",
                                                tint = MaterialTheme.colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                colors = ChipDefaults.secondaryChipColors()
                            )
                        }
                    }
                }
            }
        }
    }
}

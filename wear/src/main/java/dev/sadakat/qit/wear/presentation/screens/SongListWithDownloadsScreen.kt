package dev.sadakat.qit.wear.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.DownloadStatus
import dev.sadakat.qit.wear.presentation.components.DownloadButton
import dev.sadakat.qit.wear.presentation.components.DownloadIndicatorIcon
import dev.sadakat.qit.wear.presentation.viewmodel.DownloadViewModel
import dev.sadakat.qit.wear.presentation.viewmodel.PlaybackViewModel
import dev.sadakat.qit.wear.presentation.viewmodel.PlaylistViewModel

/**
 * Example screen showing song list with integrated download controls
 * This demonstrates how to use DownloadViewModel with domain entities
 */
@Composable
fun SongListWithDownloadsScreen(
    playlistId: String,
    onSongClick: () -> Unit,
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel()
) {
    val playlist by playlistViewModel.selectedPlaylist.collectAsState()
    val songs by playlistViewModel.playlistSongs.collectAsState()

    LaunchedEffect(playlistId) {
        playlistViewModel.selectPlaylist(playlistId)
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = playlist?.name ?: "Playlist",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3
            )
        }

        if (songs.isEmpty()) {
            item {
                Text(
                    text = "No songs in playlist",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2
                )
            }
        } else {
            items(songs) { song ->
                // This assumes songs are domain entities with downloadStatus
                // If using shared.model.Song, you'll need to map to domain entities
                SongChipWithDownload(
                    song = song,
                    downloadStatus = downloadViewModel.getDownloadStatus(song),
                    onPlayClick = {
                        playbackViewModel.setPlaylist(songs, songs.indexOf(song))
                        playbackViewModel.play()
                        onSongClick()
                    },
                    onDownloadClick = {
                        downloadViewModel.startDownload(song.id)
                    },
                    onCancelDownload = {
                        downloadViewModel.cancelDownload(song.id)
                    },
                    onRetryDownload = {
                        downloadViewModel.retryDownload(song.id)
                    }
                )
            }
        }
    }
}

/**
 * Song chip with integrated download controls
 */
@Composable
fun SongChipWithDownload(
    song: Song,
    downloadStatus: DownloadStatus,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        label = {
            Text(
                text = song.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = song.artistName(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            // Show download status indicator
            DownloadIndicatorIcon(
                downloadStatus = downloadStatus,
                modifier = Modifier.size(ChipDefaults.IconSize)
            )
        },
        onClick = onPlayClick,
        colors = ChipDefaults.secondaryChipColors(),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Alternative: Song chip with download button instead of indicator
 */
@Composable
fun SongChipWithDownloadButton(
    song: Song,
    downloadStatus: DownloadStatus,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        label = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = song.artistName(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            DownloadButton(
                downloadStatus = downloadStatus,
                onDownloadClick = onDownloadClick,
                onCancelClick = onCancelDownload,
                onRetryClick = onRetryDownload,
                modifier = Modifier.size(ChipDefaults.IconSize)
            )
        },
        onClick = onPlayClick,
        colors = ChipDefaults.secondaryChipColors(),
        modifier = modifier.fillMaxWidth()
    )
}

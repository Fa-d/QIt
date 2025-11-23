package dev.sadakat.qit.wear.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.sadakat.qit.shared.model.Song
import dev.sadakat.qit.wear.presentation.viewmodel.PlaybackViewModel
import dev.sadakat.qit.wear.presentation.viewmodel.PlaylistViewModel

@Composable
fun SongListScreen(
    playlistId: String,
    onSongClick: () -> Unit,
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
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
                SongChip(
                    song = song,
                    onClick = {
                        playbackViewModel.setPlaylist(songs, songs.indexOf(song))
                        playbackViewModel.play()
                        onSongClick()
                    }
                )
            }
        }
    }
}

@Composable
fun SongChip(
    song: Song,
    onClick: () -> Unit
) {
    Chip(
        label = {
            Text(
                text = song.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = song.artist?.let {
            {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        onClick = onClick,
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

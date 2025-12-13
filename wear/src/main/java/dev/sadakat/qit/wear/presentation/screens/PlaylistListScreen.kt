package dev.sadakat.qit.wear.presentation.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.wear.presentation.viewmodel.PlaylistViewModel

@Composable
fun PlaylistListScreen(
    onPlaylistClick: (String) -> Unit,
    onDownloadsClick: (() -> Unit)? = null,
    onSyncSettingsClick: (() -> Unit)? = null,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Playlists",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3
            )
        }

        // Sync Settings button
        onSyncSettingsClick?.let { syncClick ->
            item {
                Chip(
                    label = {
                        Text(
                            text = "Sync Settings",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text("Manage phone sync")
                    },
                    onClick = syncClick,
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Downloads button
        onDownloadsClick?.let { downloadsClick ->
            item {
                Chip(
                    label = {
                        Text(
                            text = "Downloads",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text("View downloaded songs")
                    },
                    onClick = downloadsClick,
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (playlists.isEmpty()) {
            item {
                Text(
                    text = "No playlists available.\nSync from phone.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2
                )
            }
        } else {
            items(playlists) { playlist ->
                PlaylistChip(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id.value) }
                )
            }
        }
    }
}

@Composable
fun PlaylistChip(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Chip(
        label = {
            Text(
                text = playlist.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = "${playlist.songCount()} songs",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        onClick = onClick,
        colors = ChipDefaults.primaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

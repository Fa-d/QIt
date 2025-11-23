package dev.sadakat.qit.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.sadakat.qit.presentation.viewmodel.WatchSyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSyncScreen(
    viewModel: WatchSyncViewModel = hiltViewModel()
) {
    val isWatchConnected by viewModel.isWatchConnected.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncStatus) {
        when (syncStatus) {
            is WatchSyncViewModel.SyncStatus.Success -> {
                snackbarHostState.showSnackbar((syncStatus as WatchSyncViewModel.SyncStatus.Success).message)
                viewModel.clearSyncStatus()
            }
            is WatchSyncViewModel.SyncStatus.Error -> {
                snackbarHostState.showSnackbar((syncStatus as WatchSyncViewModel.SyncStatus.Error).message)
                viewModel.clearSyncStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch Sync") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (isWatchConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = if (isWatchConnected) "Watch Connected" else "Watch Not Connected",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    if (isWatchConnected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Not Connected",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            when (syncStatus) {
                is WatchSyncViewModel.SyncStatus.Syncing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Syncing...")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = { viewModel.syncPlaylistsToWatch() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isWatchConnected
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Sync Playlists to Watch")
                    }
                }
            }

            Button(
                onClick = { viewModel.checkWatchConnection() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check Connection")
            }
        }
    }
}

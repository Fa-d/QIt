package dev.sadakat.qit.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.sadakat.qit.presentation.viewmodel.WatchSyncViewModel
import dev.sadakat.qit.shared.domain.valueobject.ConnectionDiagnostics
import dev.sadakat.qit.shared.domain.valueobject.PlaybackDestination
import dev.sadakat.qit.shared.domain.valueobject.WatchAppStatus
import dev.sadakat.qit.shared.domain.valueobject.WatchNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSyncScreen(
    viewModel: WatchSyncViewModel = hiltViewModel()
) {
    val watchAppStatus by viewModel.watchAppStatus.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val playbackDestination by viewModel.playbackDestination.collectAsState()
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
                title = { Text("Watch Sync") },
                actions = {
                    IconButton(onClick = { viewModel.refreshWatchStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main status card
            WatchStatusCard(watchAppStatus)

            // Installation prompt if not installed
            if (!watchAppStatus.isInstalled) {
                InstallationPromptCard()
            }

            // Connection diagnostics
            if (watchAppStatus.isInstalled) {
                ConnectionDiagnosticsCard(watchAppStatus.connectionDiagnostics)
            }

            // Watch nodes list
            if (watchAppStatus.watchNodes.isNotEmpty()) {
                WatchNodesCard(watchAppStatus.watchNodes)
            }

            // Playback destination settings
            PlaybackDestinationCard(
                currentDestination = playbackDestination,
                onDestinationChange = { viewModel.setPlaybackDestination(it) }
            )

            // Sync controls
            when (syncStatus) {
                is WatchSyncViewModel.SyncStatus.Syncing -> {
                    SyncingCard()
                }
                else -> {
                    SyncControlsCard(
                        enabled = watchAppStatus.isConnected,
                        onSyncClick = { viewModel.syncPlaylistsToWatch() }
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchStatusCard(status: WatchAppStatus) {
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
                tint = when {
                    status.isConnected -> MaterialTheme.colorScheme.primary
                    status.isInstalled -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = when {
                    status.isConnected -> "Watch Connected"
                    status.isInstalled -> "Watch App Installed"
                    else -> "Watch App Not Installed"
                },
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (status.isInstalled) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (status.isInstalled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (status.isInstalled) "Installed" else "Not Installed",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (status.appVersion != null) {
                Text(
                    text = "Version: ${status.appVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InstallationPromptCard() {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Install Watch App",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = "To sync your music to your Wear OS watch, install the QIt companion app from the Play Store on your watch.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Button(
                onClick = {
                    // Open Play Store on watch
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://details?id=dev.sadakat.qit.wear")
                        // This flag tells Android to open on the watch if available
                        addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to web browser
                        val webIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://play.google.com/store/apps/details?id=dev.sadakat.qit.wear")
                        }
                        context.startActivity(webIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open in Play Store")
            }
        }
    }
}

@Composable
private fun ConnectionDiagnosticsCard(diagnostics: ConnectionDiagnostics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connection Diagnostics",
                style = MaterialTheme.typography.titleMedium
            )

            DiagnosticRow(
                label = "Bluetooth",
                value = if (diagnostics.bluetoothEnabled) "Enabled" else "Disabled",
                isHealthy = diagnostics.bluetoothEnabled
            )

            DiagnosticRow(
                label = "Watch Capability",
                value = if (diagnostics.hasCapability) "Detected" else "Not Detected",
                isHealthy = diagnostics.hasCapability
            )

            DiagnosticRow(
                label = "Connected Devices",
                value = diagnostics.nodeCount.toString(),
                isHealthy = diagnostics.nodeCount > 0
            )

            diagnostics.errorMessage?.let { error ->
                Text(
                    text = "Error: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Last checked: ${formatTimestamp(diagnostics.lastCheckTimestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    isHealthy: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Icon(
                if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isHealthy)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun WatchNodesCard(nodes: List<WatchNode>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connected Watches",
                style = MaterialTheme.typography.titleMedium
            )

            nodes.forEach { node ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Watch,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = node.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (node.isNearby) "Nearby" else "Not nearby",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (node.isNearby) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = "Connected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (node != nodes.last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SyncingCard() {
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

@Composable
private fun SyncControlsCard(
    enabled: Boolean,
    onSyncClick: () -> Unit
) {
    Button(
        onClick = onSyncClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Icon(Icons.Default.Sync, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text("Sync Playlists to Watch")
    }
}

@Composable
private fun PlaybackDestinationCard(
    currentDestination: PlaybackDestination,
    onDestinationChange: (PlaybackDestination) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Playback Destination",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Choose where music should play by default when you select a song",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Phone option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentDestination == PlaybackDestination.PHONE,
                    onClick = { onDestinationChange(PlaybackDestination.PHONE) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = PlaybackDestination.PHONE.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = PlaybackDestination.PHONE.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Watch option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentDestination == PlaybackDestination.WATCH,
                    onClick = { onDestinationChange(PlaybackDestination.WATCH) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = PlaybackDestination.WATCH.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = PlaybackDestination.WATCH.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> "${diff / 86400_000} days ago"
    }
}

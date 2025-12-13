package dev.sadakat.qit.wear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import dev.sadakat.qit.wear.presentation.components.LastSyncIndicator
import dev.sadakat.qit.wear.presentation.components.ManualSyncButton
import dev.sadakat.qit.wear.presentation.components.SyncProgressIndicator
import dev.sadakat.qit.wear.presentation.components.SyncStatusCard
import dev.sadakat.qit.wear.presentation.model.SyncStatus
import dev.sadakat.qit.wear.presentation.viewmodel.PhoneSyncViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for sync settings and status on WearOS
 * Displays sync status, last sync time, manual sync trigger, and settings
 */
@Composable
fun SyncSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PhoneSyncViewModel = hiltViewModel()
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    val isPhoneConnected by viewModel.isPhoneConnected.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header
        item {
            Text(
                text = "Sync Settings",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3
            )
        }

        // Phone connection status
        item {
            Chip(
                label = {
                    Text(
                        text = if (isPhoneConnected) "Phone Connected" else "Phone Disconnected",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = {
                    Icon(
                        imageVector = if (isPhoneConnected) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = "Connection status",
                        modifier = Modifier.size(ChipDefaults.IconSize),
                        tint = if (isPhoneConnected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurfaceVariant
                    )
                },
                onClick = {
                    viewModel.checkPhoneConnection()
                },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sync status card
        item {
            SyncStatusCard(
                syncStatus = syncStatus,
                onClick = {
                    if (syncStatus !is SyncStatus.Syncing) {
                        viewModel.triggerManualSync()
                    }
                },
                modifier = Modifier.padding(horizontal = 0.dp)
            )
        }

        // Last sync indicator
        if (lastSyncTime != null || syncStatus is SyncStatus.Success) {
            item {
                LastSyncIndicator(
                    lastSyncTime = lastSyncTime
                )
            }
        }

        // Manual sync button (only show when not syncing)
        if (syncStatus !is SyncStatus.Syncing) {
            item {
                ManualSyncButton(
                    enabled = isPhoneConnected,
                    isSyncing = false,
                    onClick = {
                        viewModel.triggerManualSync()
                    }
                )
            }
        } else {
            // Show progress indicator when syncing
            item {
                SyncProgressIndicator(
                    modifier = Modifier.padding(vertical = 16.dp),
                    showLabel = true
                )
            }
        }

        // Auto-sync toggle
        item {
            Chip(
                label = {
                    Text(
                        text = "Auto-sync",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                secondaryLabel = {
                    Text(
                        text = if (autoSyncEnabled) "Enabled" else "Disabled",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = {
                    Icon(
                        imageVector = if (autoSyncEnabled) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = "Auto-sync status",
                        modifier = Modifier.size(ChipDefaults.IconSize),
                        tint = if (autoSyncEnabled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurfaceVariant
                    )
                },
                onClick = {
                    viewModel.toggleAutoSync(!autoSyncEnabled)
                },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recent sync activity header (if we have sync history)
        if (lastSyncTime != null) {
            item {
                Text(
                    text = "Recent Activity",
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }

            // Show most recent sync info
            item {
                RecentSyncItem(
                    syncStatus = syncStatus,
                    timestamp = lastSyncTime
                )
            }
        }

        // Info text
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (!isPhoneConnected) {
                        "Connect your phone to sync"
                    } else if (autoSyncEnabled) {
                        "Auto-sync will run in background"
                    } else {
                        "Use manual sync to update"
                    },
                    style = MaterialTheme.typography.caption2,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
        }

        // Clear sync status button (for errors)
        if (syncStatus is SyncStatus.Error) {
            item {
                Button(
                    onClick = {
                        viewModel.clearSyncStatus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("Clear Error")
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

/**
 * Item showing a recent sync activity
 */
@Composable
private fun RecentSyncItem(
    syncStatus: SyncStatus,
    timestamp: Long?,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (syncStatus) {
        is SyncStatus.Success -> "Completed" to MaterialTheme.colors.primary
        is SyncStatus.Error -> "Failed" to MaterialTheme.colors.error
        is SyncStatus.Conflict -> "Conflicts" to androidx.compose.ui.graphics.Color(0xFFFFA726)
        is SyncStatus.Syncing -> "In Progress" to MaterialTheme.colors.primary
        is SyncStatus.Idle -> "Ready" to MaterialTheme.colors.onSurfaceVariant
    }

    Chip(
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = statusColor
                )
                timestamp?.let {
                    Text(
                        text = formatTimestamp(it),
                        style = MaterialTheme.typography.caption3,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
        },
        secondaryLabel = when (syncStatus) {
            is SyncStatus.Success -> {
                {
                    Text(
                        text = "${syncStatus.itemCount} items synced",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            is SyncStatus.Error -> {
                {
                    Text(
                        text = syncStatus.message,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            is SyncStatus.Conflict -> {
                {
                    Text(
                        text = "${syncStatus.conflictCount} conflicts",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            else -> null
        },
        onClick = { /* View details - TODO */ },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Format timestamp to time string (HH:mm)
 */
private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

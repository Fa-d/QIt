package dev.sadakat.qit.wear.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
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
import dev.sadakat.qit.wear.presentation.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Card displaying the current sync status with icon and message
 */
@Composable
fun SyncStatusCard(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val (icon, iconTint, label, secondaryLabel) = when (syncStatus) {
        is SyncStatus.Idle -> {
            Tuple4(
                Icons.Default.Info,
                MaterialTheme.colors.onSurfaceVariant,
                "Ready to sync",
                "Tap to sync from phone"
            )
        }
        is SyncStatus.Syncing -> {
            Tuple4(
                null, // Will use CircularProgressIndicator
                MaterialTheme.colors.primary,
                "Syncing...",
                "Please wait"
            )
        }
        is SyncStatus.Success -> {
            Tuple4(
                Icons.Default.CheckCircle,
                MaterialTheme.colors.primary,
                "Sync successful",
                "${syncStatus.itemCount} items synced"
            )
        }
        is SyncStatus.Error -> {
            Tuple4(
                Icons.Default.Error,
                MaterialTheme.colors.error,
                "Sync failed",
                syncStatus.message
            )
        }
        is SyncStatus.Conflict -> {
            Tuple4(
                Icons.Default.Warning,
                Color(0xFFFFA726), // Orange color for warning
                "Conflicts detected",
                "${syncStatus.conflictCount} conflicts found"
            )
        }
    }

    Chip(
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = {
            if (syncStatus is SyncStatus.Syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ChipDefaults.IconSize),
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(ChipDefaults.IconSize),
                    tint = iconTint
                )
            }
        },
        onClick = onClick,
        colors = when (syncStatus) {
            is SyncStatus.Syncing -> ChipDefaults.primaryChipColors()
            is SyncStatus.Error, is SyncStatus.Conflict -> ChipDefaults.secondaryChipColors()
            else -> ChipDefaults.secondaryChipColors()
        },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Indicator showing when the last sync occurred
 */
@Composable
fun LastSyncIndicator(
    lastSyncTime: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Last Sync",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Text(
            text = formatLastSyncTime(lastSyncTime),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
    }
}

/**
 * Button to trigger manual sync
 */
@Composable
fun ManualSyncButton(
    enabled: Boolean = true,
    isSyncing: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        enabled = enabled && !isSyncing,
        colors = ButtonDefaults.primaryButtonColors()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    indicatorColor = MaterialTheme.colors.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isSyncing) "Syncing..." else "Sync Now")
        }
    }
}

/**
 * Circular progress indicator for sync progress
 */
@Composable
fun SyncProgressIndicator(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
        if (showLabel) {
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "Syncing...",
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact sync status icon for smaller UI elements
 */
@Composable
fun SyncStatusIcon(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    when (syncStatus) {
        is SyncStatus.Idle -> {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Ready to sync",
                modifier = modifier.size(20.dp),
                tint = MaterialTheme.colors.onSurfaceVariant
            )
        }
        is SyncStatus.Syncing -> {
            CircularProgressIndicator(
                modifier = modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        }
        is SyncStatus.Success -> {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Synced",
                modifier = modifier.size(20.dp),
                tint = MaterialTheme.colors.primary
            )
        }
        is SyncStatus.Error -> {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Sync error",
                modifier = modifier.size(20.dp),
                tint = MaterialTheme.colors.error
            )
        }
        is SyncStatus.Conflict -> {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Conflicts",
                modifier = modifier.size(20.dp),
                tint = Color(0xFFFFA726) // Orange
            )
        }
    }
}

/**
 * Formats the last sync timestamp into a human-readable string
 */
private fun formatLastSyncTime(timestamp: Long?): String {
    if (timestamp == null) {
        return "Never synced"
    }

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$minutes minute${if (minutes > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours hour${if (hours > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days day${if (days > 1) "s" else ""} ago"
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

/**
 * Helper data class to hold four values (similar to Kotlin's Triple but with 4 values)
 */
private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)


package dev.sadakat.qit.wear.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import dev.sadakat.qit.wear.presentation.model.ConnectionState
import dev.sadakat.qit.wear.presentation.model.StreamingMode

/**
 * Connection status indicator for Wear OS.
 * Shows streaming/connection state with appropriate icons and animations.
 */
@Composable
fun ConnectionStatus(
    connectionState: ConnectionState,
    streamingMode: StreamingMode,
    modifier: Modifier = Modifier,
    batteryLevel: Float? = null,
    onRetry: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    when (connectionState) {
        is ConnectionState.Connected -> {
            ConnectedStatus(
                streamingMode = streamingMode,
                batteryLevel = batteryLevel,
                modifier = modifier
            )
        }
        is ConnectionState.Connecting -> {
            ConnectingStatus(
                alpha = alpha,
                modifier = modifier
            )
        }
        is ConnectionState.Disconnected -> {
            DisconnectedStatus(
                reason = connectionState.reason,
                onRetry = onRetry,
                modifier = modifier
            )
        }
        is ConnectionState.Error -> {
            ErrorStatus(
                message = connectionState.message,
                onRetry = onRetry,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ConnectedStatus(
    streamingMode: StreamingMode,
    batteryLevel: Float?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (streamingMode) {
            StreamingMode.Streaming -> {
                // WiFi icon for streaming
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = "Streaming from phone",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Streaming",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.primary
                )

                // Battery level if available
                batteryLevel?.let { battery ->
                    Spacer(modifier = Modifier.width(8.dp))
                    BatteryIndicator(level = battery)
                }
            }
            StreamingMode.Offline -> {
                // Download checkmark for offline
                Icon(
                    Icons.Default.DownloadDone,
                    contentDescription = "Playing offline",
                    tint = Color.Green,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Offline",
                    style = MaterialTheme.typography.caption2,
                    color = Color.Green
                )
            }
            StreamingMode.Unknown -> {
                // No display for unknown mode
            }
        }
    }
}

@Composable
private fun ConnectingStatus(
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Wifi,
            contentDescription = "Connecting",
            tint = MaterialTheme.colors.primary.copy(alpha = alpha),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "Connecting...",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.primary.copy(alpha = alpha)
        )
    }
}

@Composable
private fun DisconnectedStatus(
    reason: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WifiOff,
                contentDescription = "Disconnected",
                tint = Color.Red,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = reason,
                style = MaterialTheme.typography.caption2,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }

        // Retry button
        onRetry?.let { retry ->
            Button(
                onClick = retry,
                modifier = Modifier.height(ButtonDefaults.SmallButtonSize),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red.copy(alpha = 0.2f)
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorStatus(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.caption2,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }

        // Retry button
        onRetry?.let { retry ->
            Button(
                onClick = retry,
                modifier = Modifier.height(ButtonDefaults.SmallButtonSize),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red.copy(alpha = 0.2f)
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BatteryIndicator(
    level: Float,
    modifier: Modifier = Modifier
) {
    val batteryColor = when {
        level > 0.5f -> Color.Green
        level > 0.2f -> Color.Yellow
        else -> Color.Red
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (level > 0.5f) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
            contentDescription = "Phone battery",
            tint = batteryColor,
            modifier = Modifier.size(14.dp)
        )

        Text(
            text = "${(level * 100).toInt()}%",
            style = MaterialTheme.typography.caption2,
            color = batteryColor
        )
    }
}

/**
 * Buffering indicator for streaming playback.
 */
@Composable
fun BufferingIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.HourglassEmpty,
            contentDescription = "Buffering",
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.primary
        )
    }
}


package dev.sadakat.qit.wear.presentation.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
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
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.ButtonDefaults

/**
 * Volume control component optimized for Wear OS.
 * Supports rotary input and provides haptic feedback.
 */
@Composable
fun VolumeControl(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    onMuteToggle: () -> Unit = {}
) {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isRoundScreen = configuration.isScreenRound

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Volume icon
        Button(
            onClick = onMuteToggle,
            modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            colors = ButtonDefaults.buttonColors()
        ) {
            val volumeIcon = when {
                isMuted || volume == 0f -> Icons.Default.VolumeOff
                volume < 0.3f -> Icons.Default.VolumeMute
                volume < 0.7f -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            }
            Icon(
                volumeIcon,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                modifier = Modifier.size(24.dp)
            )
        }

        // Vertical volume slider
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(120.dp)
                .onRotaryScrollEvent { event ->
                    // Handle rotary input
                    val delta = -event.verticalScrollPixels
                    val newVolume = (volume + delta / 500f).coerceIn(0f, 1f)
                    onVolumeChange(newVolume)

                    // Provide haptic feedback
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                    true
                }
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
            )

            // Volume fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(volume)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colors.primary)
            )

            // Thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-volume * 112).dp)
                    .background(
                        MaterialTheme.colors.onPrimary,
                        CircleShape
                    )
            )
        }

        // Volume percentage text
        Text(
            text = "${(volume * 100).toInt()}%",
            style = MaterialTheme.typography.caption2,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface
        )
    }
}

/**
 * Compact volume control for tight spaces.
 */
@Composable
fun CompactVolumeControl(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Volume down button
        Button(
            onClick = { onVolumeChange((volume - 0.1f).coerceAtLeast(0f)) },
            modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.surface
            )
        ) {
            Icon(
                Icons.Default.VolumeDown,
                contentDescription = "Volume Down",
                modifier = Modifier.size(20.dp)
            )
        }

        // Volume indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Simple progress bar
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(volume)
                        .background(
                            MaterialTheme.colors.primary,
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            Text(
                text = "${(volume * 100).toInt()}%",
                style = MaterialTheme.typography.caption2,
                fontSize = 10.sp
            )
        }

        // Volume up button
        Button(
            onClick = { onVolumeChange((volume + 0.1f).coerceAtMost(1f)) },
            modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.surface
            )
        ) {
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = "Volume Up",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Volume control with visual feedback for Wear OS.
 * Shows a popup with volume level when changed.
 */
@Composable
fun VolumeControlWithFeedback(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    showFeedback: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        CompactVolumeControl(
            volume = volume,
            onVolumeChange = onVolumeChange
        )

        // Volume feedback popup (shown when visible)
        if (showFeedback) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.surface.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colors.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Volume: ${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.caption1,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
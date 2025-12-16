package dev.sadakat.qit.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.R
import dev.sadakat.qit.shared.domain.entity.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the media notification for the music player.
 * Shows current track info and provides playback controls.
 */
@Singleton
@OptIn(UnstableApi::class)
class MediaNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val mediaSession: MediaSession,
    private val coroutineScope: CoroutineScope,
    private val imageLoader: ImageLoader
) {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "music_playback"
        private const val CHANNEL_NAME = "Music Playback"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows current playing song and playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Build and return the media notification
     */
    fun buildNotification(): Notification {
        val currentSong = getCurrentSong()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(currentSong?.title ?: "Unknown")
            .setContentText(currentSong?.artist ?: "")
            .setSubText(currentSong?.album ?: "")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                null
            )
            .addAction(
                if (exoPlayer.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (exoPlayer.isPlaying) "Pause" else "Play",
                null
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                null
            )

        return builder.build()
    }

    /**
     * Create pending intent for playback actions
     */
    private fun createPendingIntent(action: PlaybackAction) = null // Placeholder - will need proper implementation

    /**
     * Get current song from player
     */
    private fun getCurrentSong(): Song? {
        // This will need proper implementation based on your data model
        // For now, return null as placeholder
        return null
    }

    /**
     * Update the notification (e.g., when song changes)
     */
    fun updateNotification() {
        val notification = buildNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Clear the notification
     */
    fun clearNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Playback actions for the notification
     */
    enum class PlaybackAction {
        PLAY_PAUSE,
        PREVIOUS,
        NEXT,
        STOP
    }
}
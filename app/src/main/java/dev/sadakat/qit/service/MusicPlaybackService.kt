package dev.sadakat.qit.service

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.playback.PlaybackManager
import dev.sadakat.qit.player.MediaNotificationManager
import dev.sadakat.qit.player.MusicSessionCallback
import javax.inject.Inject

/**
 * Music playback service for the phone app.
 * Handles MediaSession integration and background playback.
 */
@UnstableApi
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var mediaSessionCallback: MusicSessionCallback

    @Inject
    lateinit var mediaNotificationManager: MediaNotificationManager

    @Inject
    lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        // Start foreground service with notification
        mediaNotificationManager.updateNotification()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playbackManager.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaNotificationManager.clearNotification()
        mediaSession.release()
        playbackManager.release()
        super.onDestroy()
    }

    // Note: onMediaNotificationClicked is removed in Media3
    // Media clicks are handled through MediaSession callbacks
}
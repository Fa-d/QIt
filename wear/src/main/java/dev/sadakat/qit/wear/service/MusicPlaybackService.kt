package dev.sadakat.qit.wear.service

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Exposes the app-wide player through a MediaSession so system UI
 * (media notification, headset buttons, etc.) can control playback.
 *
 * The ExoPlayer instance is an app-scoped singleton shared with
 * [dev.sadakat.qit.wear.playback.PlaybackManager], so commands coming through
 * the session and commands from the app UI act on the same player.
 * Its lifecycle is therefore owned by the application scope, not by this
 * service - the service only releases the MediaSession wrapper.
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @Inject lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        // Initialize Media3 player and session
        mediaSession = MediaSession.Builder(this, player)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // Only release the session; the player is app-scoped and shared.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}

package dev.sadakat.qit.service

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.playback.PlaybackManager
import javax.inject.Inject

/**
 * Music playback service for the phone app.
 *
 * The MediaSession and its player are app-scoped singletons shared with
 * [PlaybackManager]. Media3's default media notification provider posts a
 * fully functional media notification automatically whenever the session is
 * active, so no manual notification is posted here (a manual notification
 * built without session action pending intents would have dead buttons).
 */
@UnstableApi
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var mediaSession: MediaSession

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
        // The MediaSession and ExoPlayer are app-scoped singletons shared with
        // PlaybackManager; releasing them here would break playback for the
        // remainder of the process lifetime.
        super.onDestroy()
    }
}

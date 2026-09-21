package dev.sadakat.qit.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps Quran playback alive in the background. Media3's default notification provider posts the
 * media notification for every session added to this service.
 *
 * The MediaSession and its player are app-scoped singletons (see MediaModule), so they are not
 * released here.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class QuranPlaybackService : MediaSessionService() {

    @Inject
    lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate() // Hilt injects here.
        // Media3 only adds a session itself when a controller connects, and the app drives its
        // player directly, so without this no notification (or foreground service) ever appears.
        addSession(mediaSession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}

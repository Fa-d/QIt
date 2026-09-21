package dev.sadakat.qit.wear.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.core.data.player.ExoQuranPlayer
import javax.inject.Inject

/**
 * Keeps Quran playback alive in the background and exposes it to system media controls.
 *
 * The player is app-scoped and shared with the UI, so only the MediaSession wrapper is released
 * here. The session uses [ExoQuranPlayer.sessionPlayer] so next/previous move by ayah.
 */
@AndroidEntryPoint
class QuranPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @Inject
    lateinit var quranPlayer: ExoQuranPlayer

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, quranPlayer.sessionPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}

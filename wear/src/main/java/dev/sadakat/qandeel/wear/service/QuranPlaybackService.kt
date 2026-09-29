package dev.sadakat.qandeel.wear.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qandeel.core.data.R
import dev.sadakat.qandeel.core.data.player.ExoQuranPlayer
import javax.inject.Inject

/**
 * Keeps Quran playback alive in the background and exposes it to system media controls.
 *
 * The player is app-scoped and shared with the UI, so only the MediaSession wrapper is released
 * here. The session uses [ExoQuranPlayer.sessionPlayer] so next/previous move by ayah. The media
 * notification shows the lamp as its small icon.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class QuranPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @Inject
    lateinit var quranPlayer: ExoQuranPlayer

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_notification) },
        )
        // Added explicitly: Media3 only adds a session itself when a controller connects, and the
        // app drives its player directly, so otherwise no media notification ever appears.
        mediaSession = MediaSession.Builder(this, quranPlayer.sessionPlayer).build().also(::addSession)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}

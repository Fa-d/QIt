package dev.sadakat.qit.shared.quran.player

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.sadakat.qit.shared.quran.model.RecitationMode
import dev.sadakat.qit.shared.quran.settings.QuranSettings
import dev.sadakat.qit.shared.quran.text.QuranText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * [QuranPlayer] on the app-wide [ExoPlayer] (built with [dev.sadakat.qit.shared.quran.audio.QuranCache.playbackDataSourceFactory]).
 * [scope] runs on the main thread. When playback starts it starts the app's MediaSessionService
 * (resolved through its `androidx.media3.session.MediaSessionService` intent filter), so playback
 * survives the app going to the background.
 */
class ExoQuranPlayer(
    private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val quranText: QuranText,
    private val settings: QuranSettings,
    private val scope: CoroutineScope
) : QuranPlayer {

    override val nowPlaying: StateFlow<NowPlaying?> get() = TODO("W2b")

    override val error: StateFlow<String?> get() = TODO("W2b")

    override val sessionPlayer: Player get() = TODO("W2b")

    override fun play(surah: Int, fromAyah: Int, mode: RecitationMode) {
        TODO("W2b")
    }

    override fun togglePlayPause() {
        TODO("W2b")
    }

    override fun nextAyah() {
        TODO("W2b")
    }

    override fun previousAyah() {
        TODO("W2b")
    }

    override fun stop() {
        TODO("W2b")
    }

    override fun restoreLast(playWhenReady: Boolean) {
        TODO("W2b")
    }
}

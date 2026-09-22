package dev.sadakat.qit.wear.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.core.domain.player.QuranPlayer
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var player: QuranPlayer

    private var openNowPlaying by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openNowPlaying = wantsNowPlaying(intent?.extras)
        // Only a fresh launch resumes: a recreated activity must not restart playback paused since.
        if (savedInstanceState == null && wantsResume(intent?.extras)) resumePlayback(player)
        setContent {
            WearQuranApp(
                openNowPlayingRequest = openNowPlaying,
                onClearNowPlayingRequest = { openNowPlaying = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (wantsNowPlaying(intent.extras)) openNowPlaying = true
        if (wantsResume(intent.extras)) resumePlayback(player)
    }
}

/** True when the launching extras ask for Now playing (the tile's Continue action). */
internal fun wantsNowPlaying(extras: Bundle?): Boolean =
    extras?.getBoolean(WearIntents.EXTRA_OPEN_NOW_PLAYING, false) == true

/** True when the launching extras also ask to resume listening (the tile's Continue / Resume). */
internal fun wantsResume(extras: Bundle?): Boolean =
    extras?.getBoolean(WearIntents.EXTRA_RESUME_PLAYBACK, false) == true

/** Resumes what's queued, or re-queues the saved position and plays it. The activity is in the foreground here. */
internal fun resumePlayback(player: QuranPlayer) {
    val nowPlaying = player.nowPlaying.value
    when {
        nowPlaying == null -> player.restoreLast(playWhenReady = true)
        !nowPlaying.isPlaying -> player.togglePlayPause()
    }
}

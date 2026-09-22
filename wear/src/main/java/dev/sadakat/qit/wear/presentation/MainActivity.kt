package dev.sadakat.qit.wear.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var openNowPlaying by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openNowPlaying = wantsNowPlaying(intent?.extras)
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
    }
}

/** True when the launching extras ask for Now playing (the tile's Continue action). */
internal fun wantsNowPlaying(extras: Bundle?): Boolean =
    extras?.getBoolean(WearIntents.EXTRA_OPEN_NOW_PLAYING, false) == true

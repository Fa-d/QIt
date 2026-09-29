package dev.sadakat.qandeel.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qandeel.core.domain.player.QuranPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Asks the system to redraw the tile. */
fun interface TileUpdates {
    fun requestUpdate()
}

/** [TileUpdates] through the system's tile updater. */
class SystemTileUpdates @Inject constructor(@param:ApplicationContext private val context: Context) : TileUpdates {
    override fun requestUpdate() = TileService.getUpdater(context).requestUpdate(QuranTileService::class.java)
}

/**
 * Keeps the tile current: whenever what it shows changes — the surah, the ayah, playing or paused —
 * it asks for a redraw. Changes are debounced (an ayah can be seconds long, and the system throttles
 * tile updates anyway), so the tile may trail the recitation by a moment.
 */
@Singleton
class TileRefresher @Inject constructor(private val player: QuranPlayer, private val updates: TileUpdates) {

    @OptIn(FlowPreview::class) // debounce
    fun start(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)) {
        scope.launch {
            player.nowPlaying
                .map { it?.let { nowPlaying -> Shown(nowPlaying.surah, nowPlaying.ayah, nowPlaying.isPlaying) } }
                .distinctUntilChanged()
                // The first value is what the tile already shows.
                .drop(1)
                .debounce(DEBOUNCE_MS)
                .collect { updates.requestUpdate() }
        }
    }

    private data class Shown(val surah: Int, val ayah: Int, val isPlaying: Boolean)

    private companion object {
        const val DEBOUNCE_MS = 1_000L
    }
}

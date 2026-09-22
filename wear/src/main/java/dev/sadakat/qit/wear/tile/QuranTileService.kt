package dev.sadakat.qit.wear.tile

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders.booleanExtra
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.tile
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.wear.R
import dev.sadakat.qit.wear.presentation.MainActivity
import dev.sadakat.qit.wear.presentation.WearIntents
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The QIt tile: continue listening, or see what plays and pause it, one swipe from the watch face.
 *
 * Pause is handled here (a load action: the tile asks for a new layout and this sees the click).
 * Resuming goes through the app instead — it's in the foreground then, so it may start the playback
 * service; a tile running in the background can't.
 */
// All three arguments, positionally: leaving one to its default compiles to Kotlin's synthetic
// default-arguments constructor, which bypasses Hilt's generated base class (and crashed onCreate).
@AndroidEntryPoint
class QuranTileService : Material3TileService(false, QuranTileColorScheme, null) {

    @Inject
    lateinit var player: QuranPlayer

    @Inject
    lateinit var settings: QuranSettings

    @Inject
    lateinit var quranText: QuranText

    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        var nowPlaying = player.nowPlaying.value
        if (requestParams.currentState.lastClickableId == PAUSE_ID && nowPlaying?.isPlaying == true) {
            player.togglePlayPause()
            nowPlaying = nowPlaying.copy(isPlaying = false)
        }
        val names = runCatching {
            quranText.surahs().associate { it.number to it.nameEnglish }
        }.getOrDefault(emptyMap())
        val state = tileStateOf(nowPlaying, settings.lastPosition.first()) { number ->
            names[number] ?: getString(R.string.tile_surah_fallback, number)
        }
        return tile(timeline = Timeline.fromLayoutElement(quranTileLayout(state, labels(), clicks())))
    }

    private fun labels() = TileLabels(
        appName = getString(R.string.app_name),
        nothingYet = getString(R.string.tile_nothing_yet),
        open = getString(R.string.tile_open),
        continueListening = getString(R.string.tile_continue),
        pause = getString(R.string.tile_pause),
        resume = getString(R.string.tile_resume),
        position = { surah, ayah -> getString(R.string.tile_position, surah, ayah) },
        progress = { ayah, count -> getString(R.string.tile_progress, ayah, count) },
    )

    private fun clicks(): TileClicks {
        val app = ComponentName(this, MainActivity::class.java)
        val nowPlaying = mapOf(WearIntents.EXTRA_OPEN_NOW_PLAYING to booleanExtra(true))
        val resume = nowPlaying + (WearIntents.EXTRA_RESUME_PLAYBACK to booleanExtra(true))
        return TileClicks(
            open = clickable(action = launchAction(app), id = OPEN_ID),
            nowPlaying = clickable(action = launchAction(app, nowPlaying), id = NOW_PLAYING_ID),
            resume = clickable(action = launchAction(app, resume), id = RESUME_ID),
            pause = clickable(id = PAUSE_ID),
        )
    }

    private companion object {
        const val OPEN_ID = "open"
        const val NOW_PLAYING_ID = "now_playing"
        const val RESUME_ID = "resume"
        const val PAUSE_ID = "pause"
    }
}

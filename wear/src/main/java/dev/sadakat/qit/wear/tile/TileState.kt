package dev.sadakat.qit.wear.tile

import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.repository.LastPosition

/** What the tile shows. */
sealed interface TileState {
    /** Nothing has ever played: the tile invites to open the app. */
    data object NothingYet : TileState

    /** Nothing is queued, but listening stopped at [ref] last time. */
    data class Continue(val surahName: String, val ref: AyahRef, val ayahCount: Int) : TileState

    /** Something is queued, playing or paused. */
    data class Queued(val surahName: String, val ref: AyahRef, val ayahCount: Int, val isPlaying: Boolean) : TileState
}

/**
 * The tile's state from the player and the saved position: what's queued wins; after a restart
 * (the process died, nothing is queued) the saved position still offers to continue.
 */
fun tileStateOf(nowPlaying: NowPlaying?, lastPosition: LastPosition?, surahName: (Int) -> String): TileState = when {
    nowPlaying != null -> TileState.Queued(
        surahName = surahName(nowPlaying.surah),
        ref = AyahRef(nowPlaying.surah, nowPlaying.ayah),
        ayahCount = nowPlaying.ayahCount,
        isPlaying = nowPlaying.isPlaying,
    )

    lastPosition != null -> TileState.Continue(
        surahName = surahName(lastPosition.ref.surah),
        ref = lastPosition.ref,
        ayahCount = QuranMeta.ayahCount(lastPosition.ref.surah),
    )

    else -> TileState.NothingYet
}

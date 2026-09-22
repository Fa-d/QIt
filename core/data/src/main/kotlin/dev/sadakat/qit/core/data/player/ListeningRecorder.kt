package dev.sadakat.qit.core.data.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.sadakat.qit.core.domain.audio.QueueItemId
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.player.ListenTracker
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Writes what is heard on [player] into [history]: each ayah whose Arabic plays to its end (the
 * rules are [ListenTracker]'s), and the time spent listening to each surah.
 *
 * A listener of its own, so it sees the player's events in the order they happened: when a repeat
 * jumps back at an ayah's end, the end is reported before the jump, and the ayah that just ended is
 * the one counted. [clock] is monotonic (listening time), [wallClock] the epoch (when heard).
 */
class ListeningRecorder(
    private val player: Player,
    private val history: ListeningHistory,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val wallClock: () -> Long,
) : Player.Listener {

    private val tracker = ListenTracker()

    /** When the current stretch of listening began on [clock], and in which surah; null while silent. */
    private var listeningSince: Long? = null
    private var listeningSurah: Int? = null

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        when (reason) {
            Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> {
                record(tracker.finish(idOf(oldPosition.mediaItem)))
                tracker.start(idOf(newPosition.mediaItem), newPosition.positionMs)
            }

            Player.DISCONTINUITY_REASON_SEEK, Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT ->
                if (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) {
                    tracker.seek(oldPosition.positionMs, newPosition.positionMs)
                } else {
                    tracker.start(idOf(newPosition.mediaItem), newPosition.positionMs)
                }

            else -> {}
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // A new queue; transitions within one are reported (in order) as position discontinuities.
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
            tracker.start(idOf(mediaItem), player.currentPosition)
        }
        // Save listening time per ayah, so little is lost if the process dies mid-surah.
        if (listeningSince != null) restartListening()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        // The player paused at the end of an item (a repeat or the sleep timer takes over there).
        if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
            record(tracker.finishCurrent())
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) record(tracker.finishCurrent())
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            startListening()
        } else {
            flushListening()
        }
    }

    private fun startListening() {
        listeningSince = clock()
        listeningSurah = idOf(player.currentMediaItem)?.surah
    }

    private fun restartListening() {
        flushListening()
        if (player.isPlaying) startListening()
    }

    /** Adds the time listened since [listeningSince] to its surah. */
    private fun flushListening() {
        val since = listeningSince ?: return
        val surah = listeningSurah
        listeningSince = null
        listeningSurah = null
        val ms = clock() - since
        if (surah != null && ms > 0) scope.launch { history.addListeningTime(surah, ms) }
    }

    private fun record(heard: AyahRef?) {
        heard ?: return
        val at = wallClock()
        scope.launch { history.recordHeard(heard, at) }
    }

    private fun idOf(item: MediaItem?): QueueItemId? = QueueItemId.parse(item?.mediaId)
}

package dev.sadakat.qit.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.sadakat.qit.playback.PlaybackManager
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media session callback for handling media control events
 * such as play, pause, skip, and queue manipulation.
 */
@Singleton
class MusicSessionCallback @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val coroutineScope: CoroutineScope
) : MediaSession.Callback {

    override fun onAddMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        // Handle adding items to the queue
        coroutineScope.launch {
            val songs = mediaItems.mapNotNull { mediaItem ->
                // Convert MediaItem to Song - this would need proper implementation
                createSongFromMediaItem(mediaItem)
            }
            if (songs.isNotEmpty()) {
                playbackManager.addToQueue(songs)
            }
        }
        return Futures.immediateFuture(mediaItems.toMutableList())
    }

    override fun onSetMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        // Replace current queue with new items
        coroutineScope.launch {
            val songs = mediaItems.mapNotNull { mediaItem ->
                createSongFromMediaItem(mediaItem)
            }
            if (songs.isNotEmpty()) {
                playbackManager.setPlaybackQueue(songs, startIndex)
            }
        }
        return Futures.immediateFuture(
            MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
        )
    }

    /**
     * Create a Song from a MediaItem.
     * This is a placeholder - you'll need to implement proper mapping
     * based on your data model.
     */
    private fun createSongFromMediaItem(mediaItem: MediaItem): Song? {
        // This would need proper implementation based on your data model
        // For now, return null as a placeholder
        return null
    }
}
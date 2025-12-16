package dev.sadakat.qit.player

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.sadakat.qit.playback.PlaybackManager
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.valueobject.FileSize
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
     * Converts MediaItem metadata to Song entity.
     */
    private fun createSongFromMediaItem(mediaItem: MediaItem): Song? {
        return try {
            val metadata = mediaItem.mediaMetadata
            val durationMs = if (metadata.durationMs != C.TIME_UNSET) metadata.durationMs else null
            Song(
                id = SongId(mediaItem.mediaId),
                title = metadata.title?.toString() ?: "Unknown Title",
                artist = metadata.artist?.toString(),
                album = metadata.albumTitle?.toString(),
                duration = durationMs?.let { dev.sadakat.qit.shared.domain.valueobject.Duration.fromMilliseconds(it) } ?: dev.sadakat.qit.shared.domain.valueobject.Duration.ZERO,
                filePath = mediaItem.localConfiguration?.uri?.toString(),
                coverArtUri = metadata.artworkUri?.toString(),
                uri = mediaItem.localConfiguration?.uri?.toString() ?: "",
                fileSize = FileSize.ZERO,
                mimeType = "audio/mpeg",
                bitrate = 128,
                dateAdded = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
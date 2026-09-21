package dev.sadakat.qit.wear.service

import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import android.net.Uri
import android.os.SystemClock
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.dto.DownloadCompleteMessage
import dev.sadakat.qit.shared.dto.DownloadProgressMessage
import dev.sadakat.qit.shared.dto.DownloadStartMessage
import dev.sadakat.qit.shared.dto.PlaybackCommandMessage
import dev.sadakat.qit.shared.dto.PlaylistSyncMessage
import dev.sadakat.qit.shared.dto.SongSyncMessage
import dev.sadakat.qit.shared.dto.StreamErrorMessage
import dev.sadakat.qit.wear.application.usecase.playback.PlaySongUseCase
import dev.sadakat.qit.wear.data.local.dao.PlaylistDao
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.data.local.entity.PlaylistEntity
import dev.sadakat.qit.wear.data.local.entity.SongEntity
import dev.sadakat.qit.wear.di.ApplicationScope
import dev.sadakat.qit.wear.infrastructure.download.WearDownloadRepository
import dev.sadakat.qit.wear.infrastructure.streaming.WearStreamingRepository
import dev.sadakat.qit.wear.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Service that listens for messages and data events from the phone.
 *
 * Responsibilities:
 *  - Playlist / song metadata sync (message + data items) -> local database
 *  - Playback commands coming from the phone
 *  - Download control messages (start/progress/complete) -> forwarded to
 *    [WearDownloadRepository]
 *
 * NOTE: Download and audio-stream *channels* are intentionally NOT handled
 * here. They are consumed by [WearDownloadRepository] and
 * [dev.sadakat.qit.wear.infrastructure.streaming.WearStreamingRepository]
 * respectively - reading the same channel from two places would corrupt the
 * transferred data.
 */
@AndroidEntryPoint
class PhoneDataService : WearableListenerService() {

    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var playSongUseCase: PlaySongUseCase
    @Inject lateinit var downloadRepository: WearDownloadRepository
    @Inject lateinit var streamingRepository: WearStreamingRepository
    @Inject lateinit var songDao: SongDao
    @Inject lateinit var playlistDao: PlaylistDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Application-lifetime scope for sync/transfer handling: GMS unbinds and
     * destroys this service as soon as callbacks return, cancelling
     * [serviceScope] - a message whose handling coroutine was already
     * dispatched would otherwise be silently dropped after consumption.
     */
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Serializes sync-message processing: chunked messages of one burst must
     * be applied in order (playlist songId merging, full-sync reconciliation),
     * and each handler runs in its own coroutine.
     *
     * PROCESS-wide (companion), not per instance: handlers run on
     * [applicationScope] and survive onDestroy; a re-bound new service
     * instance would otherwise serialize on a fresh mutex and race the
     * surviving coroutine of the old instance.
     */
    private val syncMutex get() = SyncState.syncMutex

    /**
     * Playlist songId merge state for chunked playlist syncs (P13): same-id
     * chunks of one burst are unioned. Keyed by playlist id.
     */
    private val playlistMergeStates get() = SyncState.playlistMergeStates

    /**
     * Full-sync session tracking (P16): when the first message of a full-sync
     * burst arrives (older than [FULL_SYNC_SESSION_GAP_MS] since the previous
     * one), the playlist table and all NON-downloaded song rows are cleared so
     * deletions on the phone propagate. Downloaded rows survive (their files
     * and download state are preserved); rows re-inserted by the burst carry
     * fresh metadata.
     */
    private var lastFullSyncMessageAt
        get() = SyncState.lastFullSyncMessageAt
        set(value) { SyncState.lastFullSyncMessageAt = value }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem != null) {
                try {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val path = event.dataItem.uri.path

                    when (path) {
                        WearPaths.PLAYLISTS_DATA -> {
                            val playlistJson = dataMap.getString("data")
                            if (playlistJson != null) {
                                handlePlaylistSync(playlistJson)
                            }
                        }
                        WearPaths.SONGS_DATA -> {
                            val songJson = dataMap.getString("data")
                            if (songJson != null) {
                                handleSongSync(songJson)
                            }
                        }
                        WearPaths.SETTINGS_DATA -> {
                            // Handle settings sync if needed
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling data event", e)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            WearPaths.PLAYLIST_SYNC -> {
                handlePlaylistSync(String(messageEvent.data))
            }
            WearPaths.SONG_SYNC -> {
                handleSongSync(String(messageEvent.data))
            }
            WearPaths.PLAYBACK_COMMAND -> {
                handlePlaybackCommand(String(messageEvent.data))
            }
            WearPaths.DOWNLOAD_START -> {
                handleDownloadStart(messageEvent.data)
            }
            WearPaths.DOWNLOAD_PROGRESS -> {
                handleDownloadProgress(messageEvent.data)
            }
            WearPaths.DOWNLOAD_COMPLETE -> {
                handleDownloadComplete(messageEvent.data)
            }
            WearPaths.STREAM_AUDIO_ERROR -> {
                handleStreamError(messageEvent.data)
            }
        }
    }

    /**
     * Persist synced playlists.
     *
     * Large playlists are chunked by the phone into multiple PlaylistDtos
     * sharing the same id; chunks of one burst are MERGED (songIds unioned)
     * instead of overwriting each other. A burst is delimited by time: a
     * message for a playlist id received within [PLAYLIST_BURST_GAP_MS] of
     * the previous one continues the burst.
     */
    private fun handlePlaylistSync(playlistData: String) {
        serviceScope.launch {
            syncMutex.withLock {
                try {
                    Log.d(TAG, "Handling playlist sync, data length: ${playlistData.length}")

                    val message = json.decodeFromString<PlaylistSyncMessage>(playlistData)
                    Log.d(TAG, "Received ${message.playlists.size} playlists to sync (fullSync=${message.fullSync})")

                    if (message.fullSync) {
                        startFullSyncSessionIfNeeded()
                    }

                    val now = System.currentTimeMillis()
                    val playlistEntities = message.playlists.map { dto ->
                        val previous = playlistMergeStates[dto.id]
                        val mergedIds: List<String> =
                            if (previous != null && now - previous.lastAtMs < PLAYLIST_BURST_GAP_MS) {
                                // Continuation chunk of the same burst: union.
                                (previous.songIds + dto.songIds).toList()
                            } else {
                                dto.songIds
                            }
                        playlistMergeStates[dto.id] =
                            PlaylistMergeState(LinkedHashSet(mergedIds), now)

                        PlaylistEntity(
                            id = dto.id,
                            name = dto.name,
                            description = dto.description,
                            songIds = mergedIds.joinToString(","),
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt,
                            coverArtUri = dto.coverArtUri
                        )
                    }

                    playlistDao.insertPlaylists(playlistEntities)
                    Log.d(TAG, "Successfully synced ${playlistEntities.size} playlists to local database")
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling playlist sync", e)
                }
            }
        }
    }

    /**
     * Persist synced song metadata.
     *
     * IMPORTANT: download-related columns (isDownloaded / localFilePath /
     * downloadProgress) are preserved for rows that already exist - otherwise
     * every re-sync would wipe the watch's download state and orphan the
     * downloaded files.
     */
    private fun handleSongSync(songData: String) {
        serviceScope.launch {
            syncMutex.withLock {
                try {
                    Log.d(TAG, "Handling song sync, data length: ${songData.length}")

                    val message = json.decodeFromString<SongSyncMessage>(songData)
                    Log.d(TAG, "Received ${message.songs.size} songs to sync (fullSync=${message.fullSync})")

                    if (message.fullSync) {
                        startFullSyncSessionIfNeeded()
                    }

                    // Fetch existing rows so download state can be preserved
                    val existingById = songDao
                        .getSongsByIds(message.songs.map { it.id })
                        .associateBy { it.id }

                    val songEntities = message.songs.map { dto ->
                        val existing = existingById[dto.id]
                        SongEntity(
                            id = dto.id,
                            title = dto.title,
                            artist = dto.artist,
                            album = dto.album,
                            duration = dto.durationMs,
                            localFilePath = existing?.localFilePath,
                            isDownloaded = existing?.isDownloaded ?: false,
                            fileSize = if (dto.fileSizeBytes > 0) dto.fileSizeBytes else existing?.fileSize ?: 0L,
                            downloadProgress = existing?.downloadProgress ?: 0f,
                            dateAdded = dto.dateAdded,
                            coverArtUri = dto.coverArtUri,
                            mimeType = dto.mimeType,
                            bitrate = dto.bitrate
                        )
                    }

                    songDao.insertSongs(songEntities)
                    Log.d(TAG, "Successfully synced ${songEntities.size} songs to local database")
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling song sync", e)
                }
            }
        }
    }

    /**
     * Full-sync session start (see [lastFullSyncMessageAt]): clears the
     * playlist table and non-downloaded song rows when a NEW full-sync burst
     * begins, so deletions made on the phone propagate to the watch.
     * MUST be called while holding [syncMutex].
     */
    private suspend fun startFullSyncSessionIfNeeded() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFullSyncMessageAt > FULL_SYNC_SESSION_GAP_MS) {
            Log.d(TAG, "New full-sync session: clearing playlists and non-downloaded songs")
            playlistDao.deleteAllPlaylists()
            songDao.deleteNonDownloadedSongs()
            playlistMergeStates.clear()
        }
        lastFullSyncMessageAt = now
    }

    /**
     * Handles a stream-request failure negatively acknowledged by the phone:
     * aborts the pending buffer and surfaces a StreamingStatus.Error, which
     * the PlaybackManager stream monitor turns into a stopped player + error
     * state (instead of buffering for 30s and silently "ending").
     */
    private fun handleStreamError(data: ByteArray) {
        serviceScope.launch {
            try {
                val message = json.decodeFromString<StreamErrorMessage>(String(data))
                streamingRepository.handleStreamError(SongId.from(message.songId), message.reason)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling stream error message", e)
            }
        }
    }

    private fun handleDownloadStart(data: ByteArray) {
        try {
            val message = json.decodeFromString<DownloadStartMessage>(String(data))
            downloadRepository.handleDownloadStart(SongId.from(message.songId), message.fileSize)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling download start", e)
        }
    }

    private fun handleDownloadProgress(data: ByteArray) {
        try {
            val message = json.decodeFromString<DownloadProgressMessage>(String(data))
            downloadRepository.handleDownloadProgress(SongId.from(message.songId), message.progress)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling download progress", e)
        }
    }

    private fun handleDownloadComplete(data: ByteArray) {
        try {
            val message = json.decodeFromString<DownloadCompleteMessage>(String(data))
            downloadRepository.handleDownloadComplete(SongId.from(message.songId), message.success)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling download complete", e)
        }
    }

    private fun handlePlaybackCommand(commandData: String) {
        serviceScope.launch {
            try {
                // Parse the playback command message
                val message = json.decodeFromString<PlaybackCommandMessage>(commandData)

                Log.d(TAG, "Received playback command: ${message.command}, songId: ${message.songId}")

                when (message.command.uppercase()) {
                    "PLAY" -> {
                        val songIdStr = message.songId
                        if (songIdStr != null) {
                            // Play a specific song
                            val songId = SongId.from(songIdStr)
                            val result = playSongUseCase(PlaySongUseCase.Params(songId))

                            result.fold(
                                onSuccess = { playbackSource ->
                                    Log.d(TAG, "Successfully initiated playback for song: ${message.songId}")
                                    // The ExoPlayer singleton lives on the main
                                    // Looper: player calls must hop there, this
                                    // coroutine runs on Dispatchers.IO.
                                    when (playbackSource) {
                                        is dev.sadakat.qit.wear.application.usecase.playback.PlaybackSource.Local ->
                                            withContext(Dispatchers.Main) {
                                                playbackManager.playLocalSong(playbackSource.song)
                                            }
                                        is dev.sadakat.qit.wear.application.usecase.playback.PlaybackSource.Streaming -> {
                                            // Streaming has already been initiated by the use case.
                                            // Tell the playback manager to start playing the stream -
                                            // the shared ExoPlayer resolves the streaming:// URI
                                            // through the per-song streaming buffer.
                                            Log.d(TAG, "Starting streaming playback for song: ${playbackSource.song.title}")
                                            val streamUri = Uri.parse("streaming://phone/${playbackSource.song.id.value}")
                                            withContext(Dispatchers.Main) {
                                                playbackManager.playStreamedSong(playbackSource.song, streamUri)
                                            }
                                        }
                                    }
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Failed to play song: ${error.message}", error)
                                }
                            )
                        } else {
                            // Resume playback
                            withContext(Dispatchers.Main) { playbackManager.play() }
                        }
                    }
                    "PAUSE" -> withContext(Dispatchers.Main) { playbackManager.pause() }
                    "STOP" -> withContext(Dispatchers.Main) { playbackManager.stop() }
                    "SKIP_NEXT", "NEXT" -> withContext(Dispatchers.Main) { playbackManager.skipToNext() }
                    "SKIP_PREVIOUS", "PREVIOUS" -> withContext(Dispatchers.Main) { playbackManager.skipToPrevious() }
                    else -> Log.w(TAG, "Unknown playback command: ${message.command}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling playback command", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "PhoneDataService"

        /**
         * Chunks of one playlist-sync burst are sent back-to-back; a gap
         * larger than this starts a new burst (fresh replace semantics
         * instead of union).
         */
        private const val PLAYLIST_BURST_GAP_MS = 10_000L

        /**
         * Full-sync chunks are sent back-to-back too; a gap larger than this
         * is treated as a NEW full-sync session (reconciliation runs again).
         */
        private const val FULL_SYNC_SESSION_GAP_MS = 15_000L
    }
}

/**
 * Process-wide state for sync-message processing in [PhoneDataService].
 *
 * Handlers run on the APPLICATION scope and survive the service being
 * unbound/destroyed by GMS; a re-bound service instance must therefore
 * serialize on the SAME mutex and share the same merge/session state as
 * any still-running handler coroutine of the previous instance.
 *
 * All fields are guarded by [SyncState.syncMutex].
 */
private object SyncState {
    val syncMutex = Mutex()

    /** Playlist songId merge state for chunked playlist syncs, keyed by playlist id. */
    val playlistMergeStates = mutableMapOf<String, PlaylistMergeState>()

    /** elapsedRealtime of the last message of the current/previous full-sync session. */
    var lastFullSyncMessageAt = 0L
}

/**
 * Merge state for one playlist id within a chunked sync burst: the union of
 * songIds seen so far and the wall-clock time of the last chunk.
 */
private data class PlaylistMergeState(
    val songIds: LinkedHashSet<String>,
    val lastAtMs: Long
)

package dev.sadakat.qit.service

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.di.ApplicationScope
import dev.sadakat.qit.infrastructure.wearable.WearableSyncRepository
import dev.sadakat.qit.playback.PlaybackManager
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.dto.DownloadRequestMessage
import dev.sadakat.qit.shared.dto.PlaybackCommandMessage
import dev.sadakat.qit.shared.dto.StreamRequestMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Service that listens for messages and data events from the WearOS watch
 */
@AndroidEntryPoint
class WatchDataService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Application-lifetime scope for long-running transfers (downloads,
     * streams, syncs). GMS destroys this service when idle, which cancels
     * [serviceScope] - transfers must survive that (they live in the
     * singleton repositories that own this scope).
     */
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var playlistRepository: PlaylistRepository
    @Inject lateinit var syncRepository: SyncRepository
    @Inject lateinit var downloadRepository: DownloadRepository
    @Inject lateinit var streamingRepository: StreamingRepository
    @Inject lateinit var playbackManager: PlaybackManager

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.d(TAG, "Message received: ${messageEvent.path} from ${messageEvent.sourceNodeId}")

        when (messageEvent.path) {
            WearPaths.REQUEST_PLAYLIST_SYNC -> {
                handlePlaylistSyncRequest(messageEvent.sourceNodeId)
            }
            WearPaths.REQUEST_SONG_SYNC -> {
                handleSongSyncRequest(messageEvent.sourceNodeId, messageEvent.data)
            }
            WearPaths.REQUEST_FULL_SYNC -> {
                handleFullSyncRequest(messageEvent.sourceNodeId)
            }
            WearPaths.REQUEST_DELTA_SYNC -> {
                handleDeltaSyncRequest(messageEvent.sourceNodeId, messageEvent.data)
            }
            WearPaths.DOWNLOAD_REQUEST -> {
                handleDownloadRequest(messageEvent.sourceNodeId, messageEvent.data)
            }
            WearPaths.DOWNLOAD_CANCEL -> {
                handleDownloadCancel(messageEvent.data)
            }
            WearPaths.PLAYBACK_COMMAND -> {
                handlePlaybackCommand(messageEvent.data)
            }
            WearPaths.WATCH_VERSION_ANNOUNCEMENT -> {
                handleWatchVersionAnnouncement(messageEvent.data)
            }
            WearPaths.AUDIO_STREAM + "request" -> {
                handleStreamRequest(messageEvent.sourceNodeId, messageEvent.data)
            }
        }
    }

    private fun handlePlaylistSyncRequest(nodeId: String) {
        applicationScope.launch {
            try {
                Log.d(TAG, "Handling playlist sync request from $nodeId")

                // Get all playlists from database
                val playlists = playlistRepository.getAllPlaylists().first()

                Log.d(TAG, "Found ${playlists.size} playlists to sync")

                // Sync to watch
                val result = syncRepository.syncPlaylistsToWatch(playlists)

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully synced ${playlists.size} playlists to watch")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to sync playlists to watch", error)
                    }
                )

                // A playlist is useless without its song metadata - send the
                // songs along as well (chunked inside syncSongsToWatch).
                val songs = musicRepository.getAllSongs().first()
                syncRepository.syncSongsToWatch(songs).fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully synced ${songs.size} songs to watch")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to sync songs to watch", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling playlist sync request", e)
            }
        }
    }

    private fun handleSongSyncRequest(nodeId: String, data: ByteArray) {
        applicationScope.launch {
            try {
                Log.d(TAG, "Handling song sync request from $nodeId")

                // The watch may encode a playlistId in the payload to request
                // only that playlist's songs; an empty payload means ALL songs.
                val playlistId = String(data).trim().takeIf { it.isNotEmpty() }

                val songs = if (playlistId != null) {
                    Log.d(TAG, "Song sync request scoped to playlist: $playlistId")
                    playlistRepository.getSongsForPlaylist(PlaylistId.from(playlistId))
                        .getOrDefault(emptyList())
                } else {
                    musicRepository.getAllSongs().first()
                }

                Log.d(TAG, "Found ${songs.size} songs to sync")

                // Sync to watch
                val result = syncRepository.syncSongsToWatch(songs)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully synced ${songs.size} songs to watch")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to sync songs to watch", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling song sync request", e)
            }
        }
    }

    /**
     * Handles a full sync request from the watch:
     * sends all playlists followed by all songs.
     *
     * Messages are flagged fullSync=true so the watch can reconcile deletions
     * (clear playlist table / drop non-downloaded song rows not present in
     * the incoming data).
     */
    private fun handleFullSyncRequest(nodeId: String) {
        applicationScope.launch {
            try {
                Log.d(TAG, "Handling full sync request from $nodeId")

                val playlists = playlistRepository.getAllPlaylists().first()
                syncRepository.syncPlaylistsToWatch(playlists, fullSync = true)
                    .onFailure { Log.e(TAG, "Failed to sync playlists to watch", it) }

                val songs = musicRepository.getAllSongs().first()
                syncRepository.syncSongsToWatch(songs, fullSync = true)
                    .onFailure { Log.e(TAG, "Failed to sync songs to watch", it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling full sync request", e)
            }
        }
    }

    /**
     * Handles a delta sync request from the watch.
     * The payload contains the watch's last sync timestamp (as text); only
     * playlists/songs changed after that timestamp are sent.
     */
    private fun handleDeltaSyncRequest(nodeId: String, data: ByteArray) {
        applicationScope.launch {
            try {
                val sinceTimestamp = String(data).trim().toLongOrNull() ?: 0L
                Log.d(TAG, "Handling delta sync request from $nodeId (since $sinceTimestamp)")

                // Only send items that changed after the watch's last sync
                val playlists = playlistRepository.getAllPlaylists().first()
                    .filter { it.updatedAt > sinceTimestamp }
                if (playlists.isNotEmpty()) {
                    syncRepository.syncPlaylistsToWatch(playlists)
                        .onFailure { Log.e(TAG, "Failed to sync changed playlists", it) }
                }

                val songs = musicRepository.getAllSongs().first()
                    .filter { it.dateAdded > sinceTimestamp }
                if (songs.isNotEmpty()) {
                    syncRepository.syncSongsToWatch(songs)
                        .onFailure { Log.e(TAG, "Failed to sync changed songs", it) }
                }

                Log.d(TAG, "Delta sync completed: ${playlists.size} playlists, ${songs.size} songs")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling delta sync request", e)
            }
        }
    }

    private fun handleDownloadRequest(nodeId: String, data: ByteArray) {
        applicationScope.launch {
            try {
                // Parse download request message
                val requestJson = String(data)
                val request = json.decodeFromString<DownloadRequestMessage>(requestJson)

                Log.d(TAG, "Handling download request for song ${request.songId} with quality ${request.quality} from $nodeId")

                // Parse audio quality
                val quality = try {
                    AudioQuality.valueOf(request.quality.uppercase())
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Invalid quality ${request.quality}, using MEDIUM")
                    AudioQuality.MEDIUM
                }

                // Use DownloadRepository to handle the download.
                // sourceNodeId is threaded through so the repository can
                // negatively acknowledge failures to the requesting watch.
                val songId = SongId.from(request.songId)
                val result = downloadRepository.downloadSong(songId, quality, sourceNodeId = nodeId)

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully initiated download for song: ${request.songId}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to download song ${request.songId}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling download request", e)
            }
        }
    }

    private fun handleDownloadCancel(data: ByteArray) {
        serviceScope.launch {
            try {
                val songId = SongId.from(String(data))
                Log.d(TAG, "Handling download cancel for song ${songId.value}")

                downloadRepository.cancelDownload(songId).fold(
                    onSuccess = { Log.d(TAG, "Cancelled download for song: ${songId.value}") },
                    onFailure = { error -> Log.e(TAG, "Failed to cancel download ${songId.value}", error) }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling download cancel", e)
            }
        }
    }

    /**
     * Executes a playback command received from the watch.
     *
     * The watch acts as a remote control for the PHONE's playback, so the
     * command must be executed locally. Forwarding it back to the watch (the
     * old behaviour) would either do nothing or ping-pong commands.
     */
    private fun handlePlaybackCommand(data: ByteArray) {
        serviceScope.launch {
            try {
                // Parse playback command message
                val command = json.decodeFromString<PlaybackCommandMessage>(String(data))

                Log.d(TAG, "Handling playback command: ${command.command}")

                // Resolve the song (DB access) on the service's IO context...
                val playSongId = command.songId?.let { SongId.from(it) }
                val song = playSongId?.let { musicRepository.getSongById(it).getOrNull() }

                // ...then touch the player on the main thread. ExoPlayer is a
                // main-Looper singleton: calling it from this IO coroutine
                // throws IllegalStateException, which would silently kill
                // every remote-control command.
                withContext(Dispatchers.Main) {
                    when (command.command.uppercase()) {
                        "PLAY" -> {
                            if (song != null) {
                                playbackManager.playSongOnPhone(song)
                            } else if (playSongId != null) {
                                Log.e(TAG, "Song not found for remote play command: ${playSongId.value}")
                            } else {
                                playbackManager.playLocal()
                            }
                        }
                        "PAUSE" -> playbackManager.pauseLocal()
                        "PLAY_PAUSE" -> {
                            if (playbackManager.player.isPlaying) {
                                playbackManager.pauseLocal()
                            } else {
                                playbackManager.playLocal()
                            }
                        }
                        "STOP" -> playbackManager.stopLocal()
                        "SKIP_NEXT", "NEXT" -> playbackManager.skipToNextLocal()
                        "SKIP_PREVIOUS", "PREVIOUS" -> playbackManager.skipToPreviousLocal()
                        else -> Log.w(TAG, "Unknown playback command: ${command.command}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling playback command", e)
            }
        }
    }

    private fun handleWatchVersionAnnouncement(data: ByteArray) {
        serviceScope.launch {
            try {
                val versionData = String(data)
                Log.d(TAG, "Received watch app version: $versionData")

                // Store version in repository
                val repo = syncRepository
                if (repo is WearableSyncRepository) {
                    repo.storeWatchVersion(versionData)
                    Log.d(TAG, "Successfully stored watch app version")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling watch version announcement", e)
            }
        }
    }

    private fun handleStreamRequest(nodeId: String, data: ByteArray) {
        applicationScope.launch {
            try {
                // Parse the stream request. Preferred format is the shared
                // StreamRequestMessage JSON; the raw "songId:quality" format
                // is kept as a fallback for older watch apps.
                val payload = String(data)
                val (songIdValue, qualityName) = try {
                    val request = json.decodeFromString<StreamRequestMessage>(payload)
                    request.songId to request.quality
                } catch (e: Exception) {
                    val parts = payload.split(":")
                    if (parts.size != 2) {
                        Log.e(TAG, "Invalid stream request format: $payload")
                        return@launch
                    }
                    parts[0] to parts[1]
                }

                val songId = SongId.from(songIdValue)
                val quality = try {
                    AudioQuality.valueOf(qualityName)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Invalid quality $qualityName, using MEDIUM")
                    AudioQuality.MEDIUM
                }

                Log.d(TAG, "Handling stream request for song ${songId.value} with quality $quality from $nodeId")

                // Start streaming to watch. sourceNodeId is threaded through
                // so failures can be negatively acknowledged to the watch.
                val result = streamingRepository.streamAudioToWatch(songId, quality, sourceNodeId = nodeId)

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully started streaming song: ${songId.value}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to stream song ${songId.value}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling stream request", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "WatchDataService destroyed")
    }

    companion object {
        private const val TAG = "WatchDataService"
    }
}

package dev.sadakat.qit.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.dto.DownloadRequestMessage
import dev.sadakat.qit.shared.dto.PlaybackCommandMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Service that listens for messages and data events from the WearOS watch
 */
@AndroidEntryPoint
class WatchDataService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    // Using EntryPoint pattern for dependency injection in Service
    private val musicRepository: MusicRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WatchDataServiceEntryPoint::class.java
        ).musicRepository()
    }

    private val playlistRepository: PlaylistRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WatchDataServiceEntryPoint::class.java
        ).playlistRepository()
    }

    private val syncRepository: SyncRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WatchDataServiceEntryPoint::class.java
        ).syncRepository()
    }

    private val downloadRepository: DownloadRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WatchDataServiceEntryPoint::class.java
        ).downloadRepository()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.d(TAG, "Message received: ${messageEvent.path} from ${messageEvent.sourceNodeId}")

        when (messageEvent.path) {
            WearPaths.REQUEST_PLAYLIST_SYNC -> {
                handlePlaylistSyncRequest(messageEvent.sourceNodeId)
            }
            WearPaths.REQUEST_SONG_SYNC -> {
                handleSongSyncRequest(messageEvent.sourceNodeId)
            }
            WearPaths.DOWNLOAD_REQUEST -> {
                handleDownloadRequest(messageEvent.sourceNodeId, messageEvent.data)
            }
            WearPaths.PLAYBACK_COMMAND -> {
                handlePlaybackCommand(messageEvent.data)
            }
        }
    }

    private fun handlePlaylistSyncRequest(nodeId: String) {
        serviceScope.launch {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error handling playlist sync request", e)
            }
        }
    }

    private fun handleSongSyncRequest(nodeId: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Handling song sync request from $nodeId")

                // Get all songs from database
                val songs = musicRepository.getAllSongs().first()

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

    private fun handleDownloadRequest(nodeId: String, data: ByteArray) {
        serviceScope.launch {
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

                // Use DownloadRepository to handle the download
                val songId = SongId.from(request.songId)
                val result = downloadRepository.downloadSong(songId, quality)

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

    private fun handlePlaybackCommand(data: ByteArray) {
        serviceScope.launch {
            try {
                // Parse playback command message
                val commandJson = String(data)
                val command = json.decodeFromString<PlaybackCommandMessage>(commandJson)

                Log.d(TAG, "Handling playback command: ${command.command}")

                // Forward command to sync repository
                val songId = command.songId?.let { SongId.from(it) }
                val result = syncRepository.sendPlaybackCommand(command.command, songId)

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully forwarded playback command: ${command.command}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to forward playback command", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling playback command", e)
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

/**
 * Entry point for accessing dependencies in WatchDataService
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WatchDataServiceEntryPoint {
    fun musicRepository(): MusicRepository
    fun playlistRepository(): PlaylistRepository
    fun syncRepository(): SyncRepository
    fun downloadRepository(): DownloadRepository
}

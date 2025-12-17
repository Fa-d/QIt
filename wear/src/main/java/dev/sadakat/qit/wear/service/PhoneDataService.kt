package dev.sadakat.qit.wear.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import android.net.Uri
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.dto.PlaybackCommandMessage
import dev.sadakat.qit.shared.dto.PlaylistSyncMessage
import dev.sadakat.qit.shared.dto.SongSyncMessage
import dev.sadakat.qit.shared.dto.PlaylistDto
import dev.sadakat.qit.shared.dto.SongDto
import dev.sadakat.qit.shared.model.Playlist
import dev.sadakat.qit.shared.model.Song
import dev.sadakat.qit.wear.application.usecase.playback.PlaySongUseCase
import kotlinx.serialization.json.Json
import dev.sadakat.qit.wear.data.local.WearMusicDatabase
import dev.sadakat.qit.wear.data.local.entity.PlaylistEntity
import dev.sadakat.qit.wear.data.local.entity.SongEntity
import dev.sadakat.qit.wear.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Service that listens for messages and data events from the phone
 */
@AndroidEntryPoint
class PhoneDataService : WearableListenerService() {

    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var playSongUseCase: PlaySongUseCase
    @Inject lateinit var streamingRepository: StreamingRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database by lazy { WearMusicDatabase.getDatabase(this) }
    private val channelClient by lazy { Wearable.getChannelClient(this) }
    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val json = Json { ignoreUnknownKeys = true }

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
                                handlePlaylistDataSync(playlistJson)
                            }
                        }
                        WearPaths.SONGS_DATA -> {
                            val songJson = dataMap.getString("data")
                            if (songJson != null) {
                                handleSongDataSync(songJson)
                            }
                        }
                        WearPaths.SETTINGS_DATA -> {
                            // Handle settings sync if needed
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            WearPaths.PLAYLIST_SYNC -> {
                val playlistData = String(messageEvent.data)
                try {
                    // Try to parse as JSON first (new format)
                    handlePlaylistDataSync(playlistData)
                } catch (e: Exception) {
                    // Fallback to old format parsing
                    handlePlaylistSync(playlistData)
                }
            }
            WearPaths.SONG_SYNC -> {
                val songData = String(messageEvent.data)
                try {
                    // Try to parse as JSON first (new format)
                    handleSongDataSync(songData)
                } catch (e: Exception) {
                    // Fallback to old format parsing
                    handleSongSync(songData)
                }
            }
            WearPaths.PLAYBACK_COMMAND -> {
                val command = String(messageEvent.data)
                handlePlaybackCommand(command)
            }
        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        super.onChannelOpened(channel)

        // Only handle download channels here
        // Audio stream channels are handled by WearStreamingRepository
        when {
            channel.path.startsWith(WearPaths.DOWNLOAD_CHANNEL) -> {
                val songId = channel.path.substringAfter(WearPaths.DOWNLOAD_CHANNEL)
                handleFileDownload(channel, songId)
            }
        }
    }

    private fun handlePlaylistSync(playlistData: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Handling playlist sync, data length: ${playlistData.length}")

                // Deserialize JSON to PlaylistSyncMessage
                val message = json.decodeFromString<PlaylistSyncMessage>(playlistData)

                Log.d(TAG, "Received ${message.playlists.size} playlists to sync")

                // Convert PlaylistDto to PlaylistEntity
                val playlistEntities = message.playlists.map { dto ->
                    PlaylistEntity(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        songIds = dto.songIds.joinToString(","),
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        coverArtUri = dto.coverArtUri
                    )
                }

                // Insert into database
                database.playlistDao().insertPlaylists(playlistEntities)

                Log.d(TAG, "Successfully synced ${playlistEntities.size} playlists to local database")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling playlist sync", e)
                e.printStackTrace()
            }
        }
    }

    private fun handleSongSync(songData: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Handling song sync, data length: ${songData.length}")

                // Deserialize JSON to SongSyncMessage
                val message = json.decodeFromString<SongSyncMessage>(songData)

                Log.d(TAG, "Received ${message.songs.size} songs to sync")

                // Convert SongDto to SongEntity
                val songEntities = message.songs.map { dto ->
                    SongEntity(
                        id = dto.id,
                        title = dto.title,
                        artist = dto.artist,
                        album = dto.album,
                        duration = dto.durationMs,
                        localFilePath = null, // Not downloaded yet
                        isDownloaded = false,
                        fileSize = dto.fileSizeBytes,
                        downloadProgress = 0f,
                        dateAdded = dto.dateAdded,
                        coverArtUri = dto.coverArtUri,
                        mimeType = dto.mimeType,
                        bitrate = dto.bitrate
                    )
                }

                // Insert into database
                database.songDao().insertSongs(songEntities)

                Log.d(TAG, "Successfully synced ${songEntities.size} songs to local database")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling song sync", e)
                e.printStackTrace()
            }
        }
    }

    private fun handleFileDownload(channel: ChannelClient.Channel, songId: String) {
        serviceScope.launch {
            try {
                val audioDir = File(filesDir, "audio")
                if (!audioDir.exists()) {
                    audioDir.mkdirs()
                }

                val outputFile = File(audioDir, "$songId.mp3")
                val channelClient = Wearable.getChannelClient(this@PhoneDataService)
                val inputStream = channelClient.getInputStream(channel).await()

                inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }

                // Update database
                database.songDao().updateDownloadStatus(
                    songId = songId,
                    isDownloaded = true,
                    filePath = outputFile.absolutePath
                )

                channelClient.close(channel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                                    when (playbackSource) {
                                        is dev.sadakat.qit.wear.application.usecase.playback.PlaybackSource.Local -> {
                                            playbackManager.playLocalSong(playbackSource.song)
                                        }
                                        is dev.sadakat.qit.wear.application.usecase.playback.PlaybackSource.Streaming -> {
                                            // Streaming has already been initiated by the use case
                                            // We just need to tell the playback manager to start playing the stream
                                            Log.d(TAG, "Starting streaming playback for song: ${playbackSource.song.title}")
                                            val streamUri = Uri.parse("streaming://phone/${playbackSource.song.id.value}")
                                            playbackManager.playStreamedSong(playbackSource.song, streamUri)
                                        }
                                    }
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Failed to play song: ${error.message}", error)
                                }
                            )
                        } else {
                            // Resume playback
                            playbackManager.play()
                        }
                    }
                    "PAUSE" -> playbackManager.pause()
                    "STOP" -> playbackManager.stop()
                    "SKIP_NEXT", "NEXT" -> playbackManager.skipToNext()
                    "SKIP_PREVIOUS", "PREVIOUS" -> playbackManager.skipToPrevious()
                    else -> Log.w(TAG, "Unknown playback command: ${message.command}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling playback command", e)
            }
        }
    }

    companion object {
        private const val TAG = "PhoneDataService"
    }

    private fun handlePlaylistDataSync(playlistJson: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Parsing playlist sync data")
                val syncMessage = json.decodeFromString<PlaylistSyncMessage>(playlistJson)
                val playlists = syncMessage.playlists.map { dto ->
                    Playlist(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        songIds = dto.songIds
                    )
                }

                database.playlistDao().insertPlaylists(
                    playlists.map { PlaylistEntity.fromPlaylist(it) }
                )
                Log.d(TAG, "Successfully synced ${playlists.size} playlists")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing playlist sync data", e)
                e.printStackTrace()
            }
        }
    }

    private fun handleSongDataSync(songJson: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Parsing song sync data")
                val syncMessage = json.decodeFromString<SongSyncMessage>(songJson)
                val songs = syncMessage.songs.map { dto ->
                    Song(
                        id = dto.id,
                        title = dto.title,
                        artist = dto.artist,
                        album = dto.album,
                        duration = dto.durationMs,
                        filePath = dto.filePath
                    )
                }

                database.songDao().insertSongs(
                    songs.map { SongEntity.fromSong(it) }
                )
                Log.d(TAG, "Successfully synced ${songs.size} songs")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing song sync data", e)
                e.printStackTrace()
            }
        }
    }

    private fun handleSyncMetadataUpdate(timestamp: Long) {
        // Update last sync timestamp in local storage or preferences
        // This could be used to track when the last successful sync occurred
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}


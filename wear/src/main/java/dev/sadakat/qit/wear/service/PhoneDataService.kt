package dev.sadakat.qit.wear.service

import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.model.Playlist
import dev.sadakat.qit.shared.model.Song
import dev.sadakat.qit.wear.data.local.WearMusicDatabase
import dev.sadakat.qit.wear.data.local.entity.PlaylistEntity
import dev.sadakat.qit.wear.data.local.entity.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Service that listens for messages and data events from the phone
 */
class PhoneDataService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database by lazy { WearMusicDatabase.getDatabase(this) }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        // TODO: Handle data sync from phone if using DataClient
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            WearPaths.PLAYLIST_SYNC -> {
                val playlistData = String(messageEvent.data)
                handlePlaylistSync(playlistData)
            }
            WearPaths.SONG_SYNC -> {
                val songData = String(messageEvent.data)
                handleSongSync(songData)
            }
            WearPaths.PLAYBACK_COMMAND -> {
                val command = String(messageEvent.data)
                handlePlaybackCommand(command)
            }
        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        super.onChannelOpened(channel)

        when {
            channel.path.startsWith(WearPaths.DOWNLOAD_CHANNEL) -> {
                val songId = channel.path.substringAfter(WearPaths.DOWNLOAD_CHANNEL)
                handleFileDownload(channel, songId)
            }
            channel.path.startsWith(WearPaths.AUDIO_STREAM) -> {
                val songId = channel.path.substringAfter(WearPaths.AUDIO_STREAM)
                handleAudioStream(channel, songId)
            }
        }
    }

    private fun handlePlaylistSync(playlistData: String) {
        serviceScope.launch {
            try {
                val playlists = playlistData.split("|||").mapNotNull { playlistStr ->
                    val parts = playlistStr.split("::")
                    if (parts.size >= 4) {
                        Playlist(
                            id = parts[0],
                            name = parts[1],
                            description = parts[2].ifEmpty { null },
                            songIds = if (parts[3].isEmpty()) emptyList() else parts[3].split(",")
                        )
                    } else null
                }

                database.playlistDao().insertPlaylists(
                    playlists.map { PlaylistEntity.fromPlaylist(it) }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleSongSync(songData: String) {
        serviceScope.launch {
            try {
                val songs = songData.split("|||").mapNotNull { songStr ->
                    val parts = songStr.split("::")
                    if (parts.size >= 5) {
                        Song(
                            id = parts[0],
                            title = parts[1],
                            artist = parts[2].ifEmpty { null },
                            album = parts[3].ifEmpty { null },
                            duration = parts[4].toLongOrNull() ?: 0L
                        )
                    } else null
                }

                database.songDao().insertSongs(
                    songs.map { SongEntity.fromSong(it) }
                )
            } catch (e: Exception) {
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

    private fun handleAudioStream(channel: ChannelClient.Channel, songId: String) {
        serviceScope.launch {
            // TODO: Implement audio streaming
            // This would require a custom Media3 DataSource that reads from the channel
        }
    }

    private fun handlePlaybackCommand(command: String) {
        serviceScope.launch {
            // TODO: Handle playback commands
            // This would interact with the PlaybackManager
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}


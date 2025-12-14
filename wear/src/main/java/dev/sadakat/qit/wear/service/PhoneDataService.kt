package dev.sadakat.qit.wear.service

import android.content.Context
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.model.Playlist
import dev.sadakat.qit.shared.model.Song
import dev.sadakat.qit.wear.data.local.WearMusicDatabase
import dev.sadakat.qit.wear.data.local.entity.PlaylistEntity
import dev.sadakat.qit.wear.data.local.entity.SongEntity
import dev.sadakat.qit.wear.infrastructure.streaming.StreamingAudioBuffer
import dev.sadakat.qit.wear.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database by lazy { WearMusicDatabase.getDatabase(this) }
    private val channelClient by lazy { Wearable.getChannelClient(this) }

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
            try {
                // Create a streaming buffer for this channel
                val buffer = StreamingAudioBuffer()

                // Start receiving data from the channel
                val inputStream = channelClient.getInputStream(channel).await()
                val bufferedReader = inputStream.buffered(8192)

                // Read data in chunks and write to buffer
                val chunk = ByteArray(4096)
                while (true) {
                    val bytesRead = bufferedReader.read(chunk)
                    if (bytesRead == -1) break
                    buffer.write(chunk, 0, bytesRead)
                }

                buffer.markComplete()

            } catch (e: Exception) {
                e.printStackTrace()
                channelClient.close(channel)
            }
        }
    }

    private fun handlePlaybackCommand(command: String) {
        serviceScope.launch {
            when (command) {
                "play" -> playbackManager.play()
                "pause" -> playbackManager.pause()
                "stop" -> playbackManager.stop()
                "next" -> playbackManager.skipToNext()
                "previous" -> playbackManager.skipToPrevious()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}


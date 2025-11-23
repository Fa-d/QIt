package dev.sadakat.qit.data.repository

import android.content.Context
import com.google.android.gms.wearable.*
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.model.Playlist
import dev.sadakat.qit.shared.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository for syncing data with WearOS watch
 */
class WatchSyncRepository(
    private val context: Context
) {

    private val dataClient: DataClient by lazy {
        Wearable.getDataClient(context)
    }

    private val messageClient: MessageClient by lazy {
        Wearable.getMessageClient(context)
    }

    private val channelClient: ChannelClient by lazy {
        Wearable.getChannelClient(context)
    }

    private val nodeClient: NodeClient by lazy {
        Wearable.getNodeClient(context)
    }

    /**
     * Check if watch is connected
     */
    suspend fun isWatchConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get connected watch nodes
     */
    suspend fun getConnectedNodes(): List<Node> = withContext(Dispatchers.IO) {
        try {
            nodeClient.connectedNodes.await()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Sync playlists to watch
     */
    suspend fun syncPlaylistsToWatch(playlists: List<Playlist>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    return@withContext Result.failure(Exception("No watch connected"))
                }

                // Convert playlists to JSON-like string (simple serialization)
                val playlistData = playlists.joinToString("|||") { playlist ->
                    "${playlist.id}::${playlist.name}::${playlist.description ?: ""}::${playlist.songIds.joinToString(",")}"
                }

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        WearPaths.PLAYLIST_SYNC,
                        playlistData.toByteArray()
                    ).await()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Sync songs metadata to watch
     */
    suspend fun syncSongsToWatch(songs: List<Song>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    return@withContext Result.failure(Exception("No watch connected"))
                }

                // Convert songs to simple string format
                val songData = songs.joinToString("|||") { song ->
                    "${song.id}::${song.title}::${song.artist ?: ""}::${song.album ?: ""}::${song.duration}"
                }

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        WearPaths.SONG_SYNC,
                        songData.toByteArray()
                    ).await()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Send audio file to watch for offline playback
     */
    suspend fun sendAudioFileToWatch(
        songId: String,
        filePath: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return@withContext Result.failure(Exception("No watch connected"))
            }

            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found: $filePath"))
            }

            val node = nodes.first()
            val channel = channelClient.openChannel(
                node.id,
                "${WearPaths.DOWNLOAD_CHANNEL}$songId"
            ).await()

            val outputStream = channelClient.getOutputStream(channel).await()
            file.inputStream().use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    val fileSize = file.length()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        onProgress(totalBytes.toFloat() / fileSize)
                    }
                    output.flush()
                }
            }

            channelClient.close(channel)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stream audio to watch for real-time playback
     */
    suspend fun streamAudioToWatch(songId: String, filePath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    return@withContext Result.failure(Exception("No watch connected"))
                }

                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("File not found: $filePath"))
                }

                val node = nodes.first()
                val channel = channelClient.openChannel(
                    node.id,
                    "${WearPaths.AUDIO_STREAM}$songId"
                ).await()

                val outputStream = channelClient.getOutputStream(channel).await()
                file.inputStream().buffered().use { input ->
                    outputStream.buffered().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            output.flush()
                        }
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Send playback command to watch
     */
    suspend fun sendPlaybackCommand(command: String, songId: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    return@withContext Result.failure(Exception("No watch connected"))
                }

                val commandData = if (songId != null) {
                    "$command::$songId"
                } else {
                    command
                }

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        WearPaths.PLAYBACK_COMMAND,
                        commandData.toByteArray()
                    ).await()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

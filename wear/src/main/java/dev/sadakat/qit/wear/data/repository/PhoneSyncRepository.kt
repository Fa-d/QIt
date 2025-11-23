package dev.sadakat.qit.wear.data.repository

import android.content.Context
import com.google.android.gms.wearable.*
import dev.sadakat.qit.shared.constants.WearPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository for syncing data with the phone
 */
class PhoneSyncRepository(
    private val context: Context
) {

    private val messageClient: MessageClient by lazy {
        Wearable.getMessageClient(context)
    }

    private val nodeClient: NodeClient by lazy {
        Wearable.getNodeClient(context)
    }

    private val channelClient: ChannelClient by lazy {
        Wearable.getChannelClient(context)
    }

    /**
     * Check if phone is connected
     */
    suspend fun isPhoneConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get connected phone nodes
     */
    suspend fun getConnectedNodes(): List<Node> = withContext(Dispatchers.IO) {
        try {
            nodeClient.connectedNodes.await()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Request playlist sync from phone
     */
    suspend fun requestPlaylistSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return@withContext Result.failure(Exception("No phone connected"))
            }

            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    WearPaths.REQUEST_PLAYLIST_SYNC,
                    byteArrayOf()
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Request song metadata sync from phone
     */
    suspend fun requestSongSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return@withContext Result.failure(Exception("No phone connected"))
            }

            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    WearPaths.REQUEST_SONG_SYNC,
                    byteArrayOf()
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Request download of a song from phone
     */
    suspend fun requestSongDownload(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return@withContext Result.failure(Exception("No phone connected"))
            }

            val node = nodes.first()
            messageClient.sendMessage(
                node.id,
                WearPaths.DOWNLOAD_REQUEST,
                songId.toByteArray()
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send playback command to phone
     */
    suspend fun sendPlaybackCommand(command: String, songId: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    return@withContext Result.failure(Exception("No phone connected"))
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

    /**
     * Save downloaded file from channel
     */
    suspend fun saveDownloadedFile(
        songId: String,
        channel: ChannelClient.Channel,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val audioDir = File(context.filesDir, "audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val outputFile = File(audioDir, "$songId.mp3")
            val inputStream = channelClient.getInputStream(channel).await()

            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        // Note: We don't know total file size from stream, so progress is estimated
                    }
                }
            }

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

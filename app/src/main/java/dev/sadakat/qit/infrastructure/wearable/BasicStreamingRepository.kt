package dev.sadakat.qit.infrastructure.wearable

import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.StreamingStatus
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.dto.StreamErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

/**
 * Basic streaming repository implementation (Phone side)
 * Handles streaming audio files to watch via ChannelClient
 */
class BasicStreamingRepository @Inject constructor(
    private val channelClient: ChannelClient,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient,
    private val musicRepository: MusicRepository
) : StreamingRepository {

    companion object {
        private const val TAG = "BasicStreamingRepository"
        private const val CHUNK_SIZE = 8192 // 8KB chunks
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _streamingStatus = MutableStateFlow<StreamingStatus>(StreamingStatus.Idle)

    /**
     * Serializes stream start/stop. Requests arrive as separate coroutine
     * launches per message, so a song switch must fully supersede the old
     * stream before the new one registers its channel.
     *
     * The file transfer itself runs OUTSIDE the lock: it closes over its own
     * local [ChannelClient.Channel] and only re-acquires the lock for
     * identity-checked final cleanup (see [streamFile]).
     */
    private val streamMutex = Mutex()

    // Guarded by streamMutex:
    private var activeChannel: ChannelClient.Channel? = null
    private var currentStreamingSongId: SongId? = null

    override suspend fun streamAudioToWatch(
        songId: SongId,
        quality: AudioQuality,
        sourceNodeId: String?
    ): Result<Unit> {
        val setup: Pair<ChannelClient.Channel, File>
        try {
            // Resolve everything needed to open the channel while holding the
            // lock, superseding any in-flight stream first.
            setup = streamMutex.withLock {
            // Check for existing stream and stop it (song switch)
            if (currentStreamingSongId != null || activeChannel != null) {
                Log.w(TAG, "Stopping existing stream before starting new one")
                stopActiveStreamLocked()
            }

            val song = try {
                musicRepository.getSongById(songId).getOrNull()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get song details", e)
                null
            }
            if (song == null) {
                failStreamLocked(songId, sourceNodeId, "Song not found: ${songId.value}")
                return Result.failure(IllegalStateException("Song not found: ${songId.value}"))
            }

            val filePath = song.filePath
            if (filePath.isNullOrBlank()) {
                failStreamLocked(songId, sourceNodeId, "Song file path is null or empty")
                return Result.failure(IllegalStateException("Song file path is null or empty"))
            }

            val file = File(filePath)
            if (!file.exists()) {
                failStreamLocked(songId, sourceNodeId, "Song file does not exist: $filePath")
                return Result.failure(IllegalStateException("Song file does not exist: $filePath"))
            }
            Log.d(TAG, "Song file found: $filePath (${file.length()} bytes)")

            val nodes = try {
                nodeClient.connectedNodes.await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query connected nodes", e)
                emptyList()
            }
            if (nodes.isEmpty()) {
                // The requesting node id is known even when connectedNodes
                // reports empty (transient) - try to nack directly so the
                // watch is not stuck buffering until timeout.
                _streamingStatus.value = StreamingStatus.Error("No watch connected")
                sendStreamError(sourceNodeId, songId, "No watch connected")
                return Result.failure(IllegalStateException("No watch connected"))
            }

            Log.d(TAG, "Streaming to node: ${nodes.first().displayName} (${nodes.first().id})")

            _streamingStatus.value = StreamingStatus.Streaming(songId, quality)
            currentStreamingSongId = songId
            val channel = channelClient
                .openChannel(nodes.first().id, "/stream/audio/${songId.value}")
                .await()
            activeChannel = channel
            channel to file
            }
        } catch (e: Exception) {
            Log.e(TAG, "Streaming error", e)
            streamMutex.withLock {
                closeChannelQuietly(activeChannel)
                activeChannel = null
                currentStreamingSongId = null
            }
            _streamingStatus.value = StreamingStatus.Error(e.message ?: "Streaming failed")
            sendStreamError(sourceNodeId, songId, e.message ?: "Streaming failed")
            return Result.failure(e)
        }

        val (channel, file) = setup

        // Stream outside the lock so a new request can supersede this stream.
        val streamResult = streamFile(channel, file, songId)

        // Identity-checked finalization: if a newer stream took over while we
        // were transferring, its state must not be clobbered here.
        return streamMutex.withLock {
            if (streamResult.isSuccess) {
                if (activeChannel === channel) {
                    Log.d(TAG, "Streaming completed successfully for song: ${songId.value}")
                    _streamingStatus.value = StreamingStatus.Idle
                    currentStreamingSongId = null
                    activeChannel = null
                }
            } else {
                Log.e(TAG, "File streaming failed", streamResult.exceptionOrNull())
                closeChannelQuietly(channel)
                if (activeChannel === channel) {
                    _streamingStatus.value = StreamingStatus.Error(
                        streamResult.exceptionOrNull()?.message ?: "Streaming failed"
                    )
                    currentStreamingSongId = null
                    activeChannel = null
                }
            }
            streamResult
        }
    }

    override suspend fun requestStreamFromPhone(songId: SongId, quality: AudioQuality): Result<Unit> {
        // Not used on phone side
        return Result.failure(UnsupportedOperationException("This is a phone-side repository"))
    }

    override suspend fun stopStreaming(songId: SongId): Result<Unit> {
        return try {
            Log.d(TAG, "Stopping stream for song: ${songId.value}")

            streamMutex.withLock {
                // Only stop if the current streaming song matches
                if (currentStreamingSongId != songId) {
                    Log.w(
                        TAG,
                        "Requested stop for different song. Current: $currentStreamingSongId, Requested: $songId"
                    )
                    return@withLock
                }
                stopActiveStreamLocked()
                _streamingStatus.value = StreamingStatus.Idle
                Log.d(TAG, "Stream stopped successfully")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping stream", e)
            Result.failure(e)
        }
    }

    override fun observeStreamingStatus(): Flow<StreamingStatus> {
        return _streamingStatus
    }

    override suspend fun getRecommendedQuality(): AudioQuality {
        // Simple implementation - can be enhanced with connection quality check
        return AudioQuality.MEDIUM
    }

    /**
     * Stops the active stream. MUST be called while holding [streamMutex].
     * Closes the channel by the value captured under the lock; the superseded
     * transfer's failure path then cleans up by identity and cannot close the
     * NEW stream's channel.
     */
    private suspend fun stopActiveStreamLocked() {
        val channel = activeChannel
        currentStreamingSongId = null
        activeChannel = null
        if (channel != null) {
            closeChannelQuietly(channel)
        }
    }

    /**
     * Marks the stream failed and negatively acknowledges the request to the
     * watch (MUST be called while holding [streamMutex]).
     */
    private suspend fun failStreamLocked(songId: SongId, sourceNodeId: String?, reason: String) {
        Log.e(TAG, reason)
        _streamingStatus.value = StreamingStatus.Error(reason)
        sendStreamError(sourceNodeId, songId, reason)
    }

    /**
     * Sends a StreamErrorMessage to the requesting watch node so it can
     * surface an error instead of buffering for 30s and silently "ending".
     */
    private suspend fun sendStreamError(nodeId: String?, songId: SongId, reason: String) {
        val targetNodeId = nodeId ?: try {
            nodeClient.connectedNodes.await().firstOrNull()?.id
        } catch (e: Exception) {
            null
        } ?: return
        try {
            val message = json.encodeToString(StreamErrorMessage(songId.value, reason))
            messageClient.sendMessage(
                targetNodeId,
                WearPaths.STREAM_AUDIO_ERROR,
                message.toByteArray()
            ).await()
            Log.d(TAG, "Sent stream error message for song ${songId.value} to node $targetNodeId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send stream error message", e)
        }
    }

    private suspend fun closeChannelQuietly(channel: ChannelClient.Channel?) {
        if (channel == null) return
        try {
            channelClient.close(channel).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing channel", e)
        }
    }

    /**
     * Helper function to stream a file through a channel
     * Reads file in chunks and writes to output stream with progress tracking.
     *
     * If this transfer is superseded (song switch), the channel it holds is
     * closed by [stopActiveStreamLocked], the write fails, and the CALLER's
     * identity-checked cleanup handles state - this function must not touch
     * the shared fields.
     */
    private suspend fun streamFile(
        channel: ChannelClient.Channel,
        file: File,
        songId: SongId
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting output stream from channel")
            val outputStream = channelClient.getOutputStream(channel).await()

            val fileSize = file.length()
            var totalBytesWritten = 0L
            val buffer = ByteArray(CHUNK_SIZE)

            Log.d(TAG, "Starting file streaming: ${file.name}, size: $fileSize bytes")

            FileInputStream(file).use { input ->
                var bytesRead: Int
                var lastProgressUpdate = 0f

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    // Write chunk to output stream
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead

                    // Calculate progress
                    val progress = (totalBytesWritten.toFloat() / fileSize.toFloat())

                    // Update status with buffering progress every 10%
                    if (progress - lastProgressUpdate >= 0.1f || progress >= 1.0f) {
                        lastProgressUpdate = progress
                        _streamingStatus.value = StreamingStatus.Buffering(songId, progress)
                        Log.d(TAG, "Streaming progress: ${(progress * 100).toInt()}% ($totalBytesWritten / $fileSize bytes)")
                    }

                    // Handle backpressure - flush periodically to avoid overwhelming the channel
                    if (totalBytesWritten % (CHUNK_SIZE * 10) == 0L) {
                        outputStream.flush()
                    }
                }

                // Final flush
                outputStream.flush()
                Log.d(TAG, "File streaming completed: $totalBytesWritten bytes written")
            }

            // Close the channel after successful streaming
            channelClient.close(channel).await()
            Log.d(TAG, "Channel closed successfully")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming file", e)
            Result.failure(e)
        }
    }
}

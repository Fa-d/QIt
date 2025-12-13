package dev.sadakat.qit.infrastructure.wearable

import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.StreamingStatus
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    private val _streamingStatus = MutableStateFlow<StreamingStatus>(StreamingStatus.Idle)
    private var activeChannel: ChannelClient.Channel? = null
    private var currentStreamingSongId: SongId? = null

    override suspend fun streamAudioToWatch(songId: SongId, quality: AudioQuality): Result<Unit> {
        return try {
            Log.d(TAG, "Starting stream for song: ${songId.value} with quality: $quality")

            // Check for existing stream and stop it
            if (currentStreamingSongId != null) {
                Log.w(TAG, "Stopping existing stream before starting new one")
                stopStreaming(currentStreamingSongId!!)
            }

            // Get song details from repository
            val songResult = musicRepository.getSongById(songId)
            if (songResult.isFailure) {
                val error = "Failed to get song details: ${songResult.exceptionOrNull()?.message}"
                Log.e(TAG, error)
                _streamingStatus.value = StreamingStatus.Error(error)
                return Result.failure(songResult.exceptionOrNull() ?: Exception(error))
            }

            val song = songResult.getOrNull()
            if (song == null) {
                val error = "Song not found: ${songId.value}"
                Log.e(TAG, error)
                _streamingStatus.value = StreamingStatus.Error(error)
                return Result.failure(IllegalStateException(error))
            }

            // Get file path
            val filePath = song.filePath
            if (filePath.isNullOrBlank()) {
                val error = "Song file path is null or empty"
                Log.e(TAG, error)
                _streamingStatus.value = StreamingStatus.Error(error)
                return Result.failure(IllegalStateException(error))
            }

            val file = File(filePath)
            if (!file.exists()) {
                val error = "Song file does not exist: $filePath"
                Log.e(TAG, error)
                _streamingStatus.value = StreamingStatus.Error(error)
                return Result.failure(IllegalStateException(error))
            }

            Log.d(TAG, "Song file found: $filePath (${file.length()} bytes)")

            // Check for connected nodes
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                val error = "No watch connected"
                Log.e(TAG, error)
                _streamingStatus.value = StreamingStatus.Error(error)
                return Result.failure(IllegalStateException(error))
            }

            val node = nodes.first()
            Log.d(TAG, "Streaming to node: ${node.displayName} (${node.id})")

            // Update status to streaming
            _streamingStatus.value = StreamingStatus.Streaming(songId, quality)
            currentStreamingSongId = songId

            // Open channel for streaming
            val channelPath = "/stream/audio/${songId.value}"
            Log.d(TAG, "Opening channel: $channelPath")
            val channel = channelClient.openChannel(node.id, channelPath).await()
            activeChannel = channel

            // Stream the file
            val streamResult = streamFile(channel, file, songId)
            if (streamResult.isFailure) {
                Log.e(TAG, "File streaming failed", streamResult.exceptionOrNull())
                _streamingStatus.value = StreamingStatus.Error(
                    streamResult.exceptionOrNull()?.message ?: "Streaming failed"
                )
                cleanupChannel()
                return streamResult
            }

            Log.d(TAG, "Streaming completed successfully for song: ${songId.value}")
            _streamingStatus.value = StreamingStatus.Idle
            currentStreamingSongId = null
            activeChannel = null

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Streaming error", e)
            _streamingStatus.value = StreamingStatus.Error(e.message ?: "Streaming failed")
            cleanupChannel()
            Result.failure(e)
        }
    }

    override suspend fun requestStreamFromPhone(songId: SongId, quality: AudioQuality): Result<Unit> {
        // Not used on phone side
        return Result.failure(UnsupportedOperationException("This is a phone-side repository"))
    }

    override suspend fun stopStreaming(songId: SongId): Result<Unit> {
        return try {
            Log.d(TAG, "Stopping stream for song: ${songId.value}")

            // Only stop if the current streaming song matches
            if (currentStreamingSongId != songId) {
                Log.w(TAG, "Requested stop for different song. Current: $currentStreamingSongId, Requested: $songId")
                return Result.success(Unit)
            }

            cleanupChannel()
            _streamingStatus.value = StreamingStatus.Idle
            currentStreamingSongId = null

            Log.d(TAG, "Stream stopped successfully")
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
     * Helper function to clean up active channel
     */
    private suspend fun cleanupChannel() {
        activeChannel?.let { channel ->
            try {
                Log.d(TAG, "Closing active channel")
                channelClient.close(channel).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing channel", e)
            }
            activeChannel = null
        }
    }

    /**
     * Helper function to stream a file through a channel
     * Reads file in chunks and writes to output stream with progress tracking
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

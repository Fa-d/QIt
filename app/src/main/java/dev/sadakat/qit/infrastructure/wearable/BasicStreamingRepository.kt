package dev.sadakat.qit.infrastructure.wearable

import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.StreamingStatus
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.dto.StreamRequestMessage
import dev.sadakat.qit.shared.dto.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
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
    private val nodeClient: NodeClient
) : StreamingRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val _streamingStatus = MutableStateFlow<StreamingStatus>(StreamingStatus.Idle)

    override suspend fun streamAudioToWatch(songId: SongId, quality: AudioQuality): Result<Unit> {
        return try {
            _streamingStatus.value = StreamingStatus.Streaming(songId, quality)

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                _streamingStatus.value = StreamingStatus.Error("No watch connected")
                return Result.failure(IllegalStateException("No watch connected"))
            }

            val node = nodes.first()

            // Open channel for streaming
            val channel = channelClient.openChannel(
                node.id,
                "/stream/audio/${songId.value}"
            ).await()

            // TODO: Get actual file path from song
            // For now, this is a placeholder - actual implementation needs file path from repository
            _streamingStatus.value = StreamingStatus.Idle
            Result.success(Unit)
        } catch (e: Exception) {
            _streamingStatus.value = StreamingStatus.Error(e.message ?: "Streaming failed")
            Result.failure(e)
        }
    }

    override suspend fun requestStreamFromPhone(songId: SongId, quality: AudioQuality): Result<Unit> {
        // Not used on phone side
        return Result.failure(UnsupportedOperationException("This is a phone-side repository"))
    }

    override suspend fun stopStreaming(songId: SongId): Result<Unit> {
        return try {
            _streamingStatus.value = StreamingStatus.Idle
            Result.success(Unit)
        } catch (e: Exception) {
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
     * Helper function to stream a file through a channel
     */
    suspend fun streamFile(channel: ChannelClient.Channel, file: File): Result<Unit> {
        return try {
            val outputStream = channelClient.getOutputStream(channel).await()

            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
            }

            channelClient.close(channel).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

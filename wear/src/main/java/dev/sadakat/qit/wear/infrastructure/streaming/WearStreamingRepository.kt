package dev.sadakat.qit.wear.infrastructure.streaming

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.StreamingStatus
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watch-side implementation of StreamingRepository
 * Handles receiving audio streams from phone via Wearable Channel API
 */
@Singleton
class WearStreamingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageClient: MessageClient,
    private val channelClient: ChannelClient,
    private val nodeClient: NodeClient
) : StreamingRepository {

    private val _streamingStatus = MutableStateFlow<StreamingStatus>(StreamingStatus.Idle)
    private var activeChannel: ChannelClient.Channel? = null
    private val audioBuffer = StreamingAudioBuffer()
    private var streamingJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Listener for incoming channels
    private val channelCallback = object : ChannelClient.ChannelCallback() {
        override fun onChannelOpened(channel: ChannelClient.Channel) {
            Log.d(TAG, "Channel opened: ${channel.path}")

            // Check if this is an audio stream channel
            if (channel.path.startsWith(WearPaths.AUDIO_STREAM)) {
                val songId = channel.path.removePrefix(WearPaths.AUDIO_STREAM)
                handleIncomingStream(channel, SongId.from(songId))
            }
        }

        override fun onChannelClosed(
            channel: ChannelClient.Channel,
            closeReason: Int,
            appSpecificErrorCode: Int
        ) {
            Log.d(TAG, "Channel closed: ${channel.path}, reason: $closeReason")

            if (channel.path == activeChannel?.path) {
                scope.launch {
                    audioBuffer.markComplete()

                    if (closeReason == ChannelClient.Channel.CLOSE_REASON_REMOTE_CLOSE) {
                        // Normal stream completion
                        Log.d(TAG, "Stream completed normally")
                    } else {
                        // Error occurred
                        _streamingStatus.value = StreamingStatus.Error(
                            "Stream closed unexpectedly: $closeReason"
                        )
                    }
                }
            }
        }

        override fun onInputClosed(
            channel: ChannelClient.Channel,
            closeReason: Int,
            appSpecificErrorCode: Int
        ) {
            Log.d(TAG, "Input closed: ${channel.path}")

            if (channel.path == activeChannel?.path) {
                scope.launch {
                    audioBuffer.markComplete()
                }
            }
        }
    }

    init {
        // Register channel callback to listen for incoming streams
        channelClient.registerChannelCallback(channelCallback)
    }

    override suspend fun requestStreamFromPhone(
        songId: SongId,
        quality: AudioQuality
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting stream from phone: songId=$songId, quality=$quality")

            // Clear any previous stream
            stopStreaming(songId)

            // Get connected nodes
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return@withContext Result.failure(Exception("No phone connected"))
            }

            // Prepare request message with songId and quality
            val requestData = "${songId.value}:${quality.name}".toByteArray()

            // Send stream request to phone
            val node = nodes.first()
            messageClient.sendMessage(
                node.id,
                WearPaths.AUDIO_STREAM + "request",
                requestData
            ).await()

            Log.d(TAG, "Stream request sent to phone")

            // Update status to buffering
            _streamingStatus.value = StreamingStatus.Buffering(songId, 0f)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request stream from phone", e)
            _streamingStatus.value = StreamingStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    override suspend fun stopStreaming(songId: SongId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stopping stream: $songId")

            // Cancel streaming job
            streamingJob?.cancel()
            streamingJob = null

            // Close active channel
            activeChannel?.let { channel ->
                try {
                    channelClient.close(channel).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing channel", e)
                }
            }
            activeChannel = null

            // Clear buffer
            audioBuffer.clear()

            // Update status
            _streamingStatus.value = StreamingStatus.Idle

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop streaming", e)
            Result.failure(e)
        }
    }

    override fun observeStreamingStatus(): Flow<StreamingStatus> {
        return _streamingStatus.asStateFlow()
    }

    override suspend fun getRecommendedQuality(): AudioQuality {
        // For watch, we generally want lower quality to save bandwidth
        // Could be enhanced to check connection quality
        return AudioQuality.MEDIUM
    }

    override suspend fun streamAudioToWatch(songId: SongId, quality: AudioQuality): Result<Unit> {
        // This is the phone-side implementation, not used on watch
        return Result.failure(UnsupportedOperationException("Not supported on watch"))
    }

    /**
     * Handle incoming audio stream from phone
     */
    private fun handleIncomingStream(channel: ChannelClient.Channel, songId: SongId) {
        streamingJob = scope.launch {
            try {
                Log.d(TAG, "Handling incoming stream for song: $songId")

                activeChannel = channel
                audioBuffer.clear()

                // Get input stream from channel
                val inputStream = channelClient.getInputStream(channel).await()

                // Read audio data into buffer
                val buffer = ByteArray(8192)
                var totalBytesRead = 0L
                var lastBufferingUpdate = 0L

                inputStream.use { input ->
                    while (isActive) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) {
                            Log.d(TAG, "Reached end of stream")
                            break
                        }

                        // Write to audio buffer
                        audioBuffer.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update buffering progress
                        val availableBytes = audioBuffer.availableBytes()
                        val targetBufferBytes = TARGET_BUFFER_BYTES
                        val progress = (availableBytes.toFloat() / targetBufferBytes).coerceIn(0f, 1f)

                        // Only update status every 100KB to avoid too many updates
                        if (totalBytesRead - lastBufferingUpdate > 100_000) {
                            _streamingStatus.value = StreamingStatus.Buffering(songId, progress)
                            lastBufferingUpdate = totalBytesRead
                            Log.d(TAG, "Buffering: ${availableBytes / 1024}KB, progress: ${(progress * 100).toInt()}%")
                        }

                        // Once we have enough buffered, mark as streaming
                        if (availableBytes >= targetBufferBytes &&
                            _streamingStatus.value is StreamingStatus.Buffering) {
                            _streamingStatus.value = StreamingStatus.Streaming(
                                songId,
                                AudioQuality.MEDIUM // TODO: Pass actual quality from request
                            )
                            Log.d(TAG, "Buffer ready, starting playback")
                        }
                    }
                }

                Log.d(TAG, "Stream completed: $totalBytesRead bytes received")
                audioBuffer.markComplete()

            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming stream", e)
                _streamingStatus.value = StreamingStatus.Error(e.message ?: "Stream error")
            }
        }
    }

    /**
     * Get the audio buffer for ExoPlayer integration
     */
    fun getAudioBuffer(): StreamingAudioBuffer = audioBuffer

    /**
     * Clean up resources
     */
    fun cleanup() {
        channelClient.unregisterChannelCallback(channelCallback)
        streamingJob?.cancel()
        scope.cancel()
    }

    companion object {
        private const val TAG = "WearStreamingRepo"
        // Target buffer size: ~3 seconds at 256kbps = 96KB
        private const val TARGET_BUFFER_BYTES = 96 * 1024
    }
}

package dev.sadakat.qit.wear.infrastructure.streaming

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.StreamingStatus
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watch-side implementation of StreamingRepository
 * Handles receiving audio streams from phone via Wearable Channel API
 *
 * Concurrency model:
 *  - [stateMutex] guards ALL mutations of activeChannel / activeSongId /
 *    streamingJob / currentRequestedQuality.
 *  - [activeChannel] is @Volatile so the binder-thread channel callbacks can
 *    do lock-free IDENTITY checks (a late close of an old channel must never
 *    be attributed to a new stream, even though both share the same path).
 *  - The audio buffers are a per-song registry (see [getOrCreateBuffer]) so
 *    two media items' DataSources never read the same queue.
 */
@Singleton
class WearStreamingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageClient: MessageClient,
    private val channelClient: ChannelClient,
    private val nodeClient: NodeClient
) : StreamingRepository {

    private val _streamingStatus = MutableStateFlow<StreamingStatus>(StreamingStatus.Idle)

    /**
     * The currently active channel. @Volatile: read from binder callbacks for
     * identity comparison only; mutations go through [stateMutex].
     */
    @Volatile
    private var activeChannel: ChannelClient.Channel? = null

    // Guarded by stateMutex:
    private var activeSongId: SongId? = null
    private var streamingJob: Job? = null
    private var currentRequestedQuality: AudioQuality? = null

    /**
     * Per-song audio buffers (P4). ExoPlayer keeps loaders for the previous
     * media item alive briefly after a switch; with ONE shared buffer the old
     * loader would steal the new song's opening bytes. Buffers are created
     * lazily by whichever side needs them first (DataSource.open or the
     * incoming channel) and removed when their stream is stopped.
     *
     * Guarded by [buffersLock] (plain monitor: resolved from ExoPlayer's
     * loader thread, which cannot suspend). Lock ordering: stateMutex is
     * ALWAYS acquired before buffersLock, never the other way around.
     */
    private val buffersLock = Any()
    private val buffers = mutableMapOf<String, StreamingAudioBuffer>()

    private val stateMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Listener for incoming channels
    private val channelCallback = object : ChannelClient.ChannelCallback() {
        override fun onChannelOpened(channel: ChannelClient.Channel) {
            Log.d(TAG, "Channel opened: ${channel.path}")

            // Check if this is an audio stream channel
            if (channel.path.startsWith(WearPaths.AUDIO_STREAM)) {
                val songId = channel.path.removePrefix(WearPaths.AUDIO_STREAM)
                if (songId.isNotBlank()) {
                    handleIncomingStream(channel, SongId.from(songId))
                }
            }
        }

        override fun onChannelClosed(
            channel: ChannelClient.Channel,
            closeReason: Int,
            appSpecificErrorCode: Int
        ) {
            Log.d(TAG, "Channel closed: ${channel.path}, reason: $closeReason")

            // IDENTITY check, not path: re-requesting the same song reuses the
            // channel path, so a late close of the OLD channel must not be
            // attributed to the new stream.
            if (channel !== activeChannel) return
            scope.launch { handleChannelClosed(channel, closeReason) }
        }

        override fun onInputClosed(
            channel: ChannelClient.Channel,
            closeReason: Int,
            appSpecificErrorCode: Int
        ) {
            Log.d(TAG, "Input closed: ${channel.path}, reason: $closeReason")

            if (channel !== activeChannel) return
            scope.launch { handleInputClosed(channel, closeReason) }
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

            // Stop any previous stream first (song switch): aborts its buffer,
            // closes its channel, removes it from the registry.
            stopStreaming(songId)

            // Get connected nodes
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                _streamingStatus.value = StreamingStatus.Error("No phone connected")
                return@withContext Result.failure(Exception("No phone connected"))
            }

            stateMutex.withLock {
                currentRequestedQuality = quality
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
        } catch (e: CancellationException) {
            // Propagate cancellation instead of swallowing it as a failure
            // result (the caller's scope is being torn down).
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request stream from phone", e)
            _streamingStatus.value = StreamingStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    override suspend fun stopStreaming(songId: SongId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stopping stream: $songId")

            // Snapshot the state under the lock, then act on the SNAPSHOT
            // outside it. Suspending in close(oldChannel).await() while
            // holding the lock would deadlock a concurrent
            // onChannelOpened(channelB) that is trying to register itself -
            // and a plain field-based close would clobber B's registration.
            data class StopSnapshot(
                val channel: ChannelClient.Channel?,
                val job: Job?,
                val songId: SongId?,
                val buffer: StreamingAudioBuffer?
            )

            val snapshot = stateMutex.withLock {
                val channel = activeChannel
                val job = streamingJob
                val song = activeSongId
                val buffer = song?.let { removeBufferLocked(it.value) }
                activeChannel = null
                streamingJob = null
                activeSongId = null
                StopSnapshot(channel, job, song, buffer)
            }

            snapshot.job?.cancel()
            // Wake parked readers immediately (P8): without this, a blocked
            // ExoPlayer loader thread would sit in read() for up to 30s.
            snapshot.buffer?.abort()
            snapshot.channel?.let { channel ->
                try {
                    channelClient.close(channel).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing channel", e)
                }
            }

            // Clear buffer
            snapshot.buffer?.clear()

            // Update status
            _streamingStatus.value = StreamingStatus.Idle

            Result.success(Unit)
        } catch (e: CancellationException) {
            // Propagate cancellation instead of swallowing it as a failure
            // result (the caller's scope is being torn down).
            throw e
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

    override suspend fun streamAudioToWatch(
        songId: SongId,
        quality: AudioQuality,
        sourceNodeId: String?
    ): Result<Unit> {
        // This is the phone-side implementation, not used on watch
        return Result.failure(UnsupportedOperationException("Not supported on watch"))
    }

    /**
     * Handle incoming audio stream from phone.
     *
     * Registers the new stream under [stateMutex] (superseding any previous
     * one) BEFORE its pump job can run, so a fast-completing job can never
     * remove a not-yet-registered entry.
     */
    private fun handleIncomingStream(channel: ChannelClient.Channel, songId: SongId) {
        scope.launch {
            stateMutex.withLock {
                // Supersede any previous stream: its job gets cancelled and
                // its buffer aborted (two concurrent pumps would interleave
                // two channels' data and corrupt playback).
                val previousJob = streamingJob
                val previousSongId = activeSongId
                activeChannel = channel
                activeSongId = songId
                val buffer = getOrCreateBuffer(songId.value)
                buffer.clear()

                val job = launch(start = CoroutineStart.LAZY) {
                    pumpStream(channel, songId, buffer)
                }
                streamingJob = job
                job.start()
                previousJob?.cancel()
                // Only retire the PREVIOUS song's buffer: if the same song is
                // re-requested, the fresh registration above already reused
                // (and cleared) its buffer.
                if (previousSongId != null && previousSongId != songId) {
                    removeBufferLocked(previousSongId.value)?.abort()
                }
            }
            Log.d(TAG, "Handling incoming stream for song: $songId (buffer registered)")
        }
    }

    /**
     * Reads the channel input stream into the song's audio buffer.
     * Runs as [streamingJob]; every read chunk is checked against the job's
     * isActive so a cancelled pump (song switch / stop) can never write stale
     * bytes of the old song into another buffer.
     */
    private suspend fun pumpStream(
        channel: ChannelClient.Channel,
        songId: SongId,
        audioBuffer: StreamingAudioBuffer
    ) {
        val pumpJob = coroutineContext[Job]
        try {
            Log.d(TAG, "Pumping stream for song: $songId")

            // Get input stream from channel
            val inputStream = channelClient.getInputStream(channel).await()

            // Read audio data into buffer
            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var lastBufferingUpdate = 0L

            inputStream.use { input ->
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) {
                        Log.d(TAG, "Reached end of stream")
                        break
                    }

                    // The pump may have been cancelled while blocked in read():
                    // these bytes belong to the OLD stream - drop them.
                    if (pumpJob?.isActive == false) {
                        Log.d(TAG, "Pump cancelled after read; dropping $bytesRead stale bytes")
                        break
                    }

                    // Write to audio buffer (throws if the buffer was aborted)
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
                        _streamingStatus.value is StreamingStatus.Buffering
                    ) {
                        val actualQuality = stateMutex.withLock {
                            currentRequestedQuality ?: AudioQuality.MEDIUM
                        }
                        _streamingStatus.value = StreamingStatus.Streaming(
                            songId,
                            actualQuality
                        )
                        Log.d(TAG, "Buffer ready, starting playback with quality: $actualQuality")
                    }
                }
            }

            if (pumpJob?.isActive != false) {
                Log.d(TAG, "Stream completed: $totalBytesRead bytes received")
                audioBuffer.markComplete()
            }
        } catch (e: CancellationException) {
            // Song switch or stopStreaming - the new stream owns the shared
            // state now; do not touch it.
            Log.d(TAG, "Stream pump cancelled for song: $songId")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming stream", e)
            stateMutex.withLock {
                if (activeChannel === channel) {
                    _streamingStatus.value = StreamingStatus.Error(e.message ?: "Stream error")
                }
            }
            // Leave the buffer INCOMPLETE and abort it: parked readers throw
            // so ExoPlayer surfaces the error instead of a clean EOF.
            audioBuffer.abort()
        } finally {
            if (pumpJob != null) {
                stateMutex.withLock {
                    // Remove by identity: a newer stream's job may already be
                    // registered under the same key.
                    if (streamingJob === pumpJob) {
                        streamingJob = null
                    }
                }
            }
        }
    }

    /**
     * onInputClosed: the phone finished writing (or the input died).
     *
     * Only CLOSE_REASON_NORMAL (0) and CLOSE_REASON_REMOTE_CLOSE (2) are
     * clean completions - anything else (e.g. DISCONNECTED = 1) must surface
     * as an error so ExoPlayer does not treat it as end-of-input.
     */
    private suspend fun handleInputClosed(channel: ChannelClient.Channel, closeReason: Int) {
        stateMutex.withLock {
            if (activeChannel !== channel) return
            val buffer = activeSongId?.let { getOrCreateBuffer(it.value) }
            when (closeReason) {
                CLOSE_REASON_NORMAL, CLOSE_REASON_REMOTE_CLOSE ->
                    buffer?.markComplete()
                else -> {
                    Log.w(TAG, "Stream input closed unexpectedly (reason=$closeReason)")
                    _streamingStatus.value = StreamingStatus.Error(
                        "Stream closed unexpectedly (reason=$closeReason)"
                    )
                    buffer?.abort()
                }
            }
        }
    }

    /**
     * onChannelClosed: same close-reason handling as [handleInputClosed].
     */
    private suspend fun handleChannelClosed(channel: ChannelClient.Channel, closeReason: Int) {
        stateMutex.withLock {
            if (activeChannel !== channel) return
            val buffer = activeSongId?.let { getOrCreateBuffer(it.value) }
            when (closeReason) {
                CLOSE_REASON_NORMAL, CLOSE_REASON_REMOTE_CLOSE -> {
                    Log.d(TAG, "Stream completed, closeReason=$closeReason")
                    buffer?.markComplete()
                }
                else -> {
                    Log.w(TAG, "Stream channel closed unexpectedly (reason=$closeReason)")
                    _streamingStatus.value = StreamingStatus.Error(
                        "Stream closed unexpectedly (reason=$closeReason)"
                    )
                    buffer?.abort()
                }
            }
        }
    }

    /**
     * Handles a stream-request failure negatively acknowledged by the phone
     * (StreamErrorMessage): aborts the pending buffer and surfaces the error
     * so the PlaybackManager stream monitor stops the player.
     */
    suspend fun handleStreamError(songId: SongId, reason: String) {
        Log.w(TAG, "Phone reported stream error for song ${songId.value}: $reason")
        val buffer = stateMutex.withLock {
            if (activeSongId == songId) {
                activeChannel = null
                streamingJob?.cancel()
                streamingJob = null
                activeSongId = null
            }
            removeBufferLocked(songId.value)
        }
        buffer?.abort()
        _streamingStatus.value = StreamingStatus.Error(
            reason.ifBlank { "Streaming failed on phone" }
        )
    }

    /**
     * Returns (creating if needed) the per-song audio buffer. Called from
     * both ExoPlayer's loader thread (via SchemeAwareDataSource) and the
     * stream pump; both sides therefore always end up with the SAME instance
     * no matter which one needs it first.
     */
    fun getOrCreateBuffer(songIdValue: String): StreamingAudioBuffer {
        synchronized(buffersLock) {
            return buffers.getOrPut(songIdValue) { StreamingAudioBuffer() }
        }
    }

    /** Removes (and returns) a song's buffer. MUST hold [stateMutex]. */
    private fun removeBufferLocked(songIdValue: String): StreamingAudioBuffer? {
        synchronized(buffersLock) {
            return buffers.remove(songIdValue)
        }
    }

    /**
     * Legacy accessor for the deprecated shared-buffer integration example
     * ([StreamingIntegrationExample]). Production code resolves per-song
     * buffers via [getOrCreateBuffer] (see SchemeAwareDataSource).
     */
    fun getAudioBuffer(): StreamingAudioBuffer = StreamingAudioBuffer()

    /**
     * Clean up resources
     */
    fun cleanup() {
        channelClient.unregisterChannelCallback(channelCallback)
        runBlocking {
            stateMutex.withLock {
                streamingJob?.cancel()
                streamingJob = null
                activeChannel = null
                activeSongId = null
                synchronized(buffersLock) {
                    buffers.values.forEach { it.abort() }
                    buffers.clear()
                }
            }
        }
        scope.cancel()
    }

    companion object {
        private const val TAG = "WearStreamingRepo"
        // Target buffer size: ~3 seconds at 256kbps = 96KB
        private const val TARGET_BUFFER_BYTES = 96 * 1024

        // ChannelClient close reasons (@IntDef-only in play-services-wearable,
        // hence duplicated here):
        // 0 = NORMAL, 1 = DISCONNECTED, 2 = REMOTE_CLOSE, 3 = LOCAL_CLOSE
        private const val CLOSE_REASON_NORMAL = 0
        private const val CLOSE_REASON_REMOTE_CLOSE = 2
    }
}

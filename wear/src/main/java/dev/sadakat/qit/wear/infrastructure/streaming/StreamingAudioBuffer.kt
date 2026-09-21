package dev.sadakat.qit.wear.infrastructure.streaming

import java.io.IOException
import java.util.ArrayDeque
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The buffer was aborted: the stream stopped, errored, or its DataSource was
 * closed. Propagated as an [IOException] so ExoPlayer raises onPlayerError
 * (or simply tears the loader down, when the abort came from close()).
 */
class StreamingBufferAbortedException(message: String = "Stream aborted") : IOException(message)

/**
 * A read waited longer than the timeout with no data, no completion and no
 * abort. This is a STALL, not a clean end-of-stream - previously it was
 * reported as EOF, which made the track silently "end" instead of erroring.
 */
class StreamingReadTimeoutException(
    message: String = "Timed out waiting for stream data"
) : IOException(message)

/**
 * Thread-safe streaming audio buffer.
 * Handles concurrent writes from the wearable channel and reads from ExoPlayer.
 *
 * Data is stored as a queue of chunks so reads never copy the whole buffer
 * (unlike a naive ByteArrayOutputStream approach which is O(n^2) over a song).
 *
 * The buffer also provides the stream's error/abort signalling:
 *  - [markComplete] = clean end-of-stream (read() then returns -1 once drained)
 *  - [abort]        = abnormal end (parked readers/writers throw
 *    [StreamingBufferAbortedException] promptly)
 *  - a read that times out throws [StreamingReadTimeoutException] instead of
 *    pretending to be EOF
 *  - [write] suspends while the buffer is above the high-water mark, so a
 *    paused player cannot force the whole song into watch RAM.
 */
class StreamingAudioBuffer {

    private class Chunk(val data: ByteArray, val length: Int)

    private val mutex = Mutex()
    private val chunks = ArrayDeque<Chunk>()
    private var queuedBytes = 0
    private var totalWrittenBytes = 0L
    private var readBytes = 0L
    private var isStreamComplete = false

    /** Aborted flag: @Volatile so non-suspending [abort] can set it. */
    @Volatile
    private var aborted = false

    companion object {
        private const val READ_TIMEOUT_MS = 30000L // 30 seconds timeout for waiting for data
        private const val POLL_INTERVAL_MS = 50L // Check for data every 50ms

        /**
         * High-water mark for buffered audio (~8MB). write() suspends while
         * queuedBytes is above this, naturally backpressuring the channel
         * pump instead of buffering a whole song in RAM.
         */
        private const val HIGH_WATER_BYTES = 8 * 1024 * 1024
        private const val BACKPRESSURE_POLL_MS = 50L
    }

    /**
     * Write audio data to buffer. Suspends while the buffer is at/above the
     * high-water mark (backpressure for the channel pump).
     *
     * @param data Audio data to write
     * @param offset Starting position in data array
     * @param length Number of bytes to write
     * @throws StreamingBufferAbortedException if the buffer was aborted
     */
    suspend fun write(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        require(length >= 0) { "length must be >= 0" }
        if (length == 0) return
        val copy = if (offset == 0 && length == data.size) data else data.copyOfRange(offset, offset + length)
        while (true) {
            mutex.withLock {
                if (aborted) throw StreamingBufferAbortedException()
                if (queuedBytes <= HIGH_WATER_BYTES) {
                    chunks.addLast(Chunk(copy, length))
                    queuedBytes += length
                    totalWrittenBytes += length
                    return
                }
            }
            // Buffer full: the player is not draining fast enough (probably
            // paused) - wait instead of buffering unboundedly.
            delay(BACKPRESSURE_POLL_MS)
        }
    }

    /**
     * Read audio data from buffer
     * BLOCKS until data is available or stream is complete
     *
     * @param output Destination array
     * @param offset Starting position in output array
     * @param length Maximum number of bytes to read
     * @return Number of bytes actually read, or -1 ONLY when the stream is
     *         complete and fully drained (clean EOF)
     * @throws StreamingReadTimeoutException if no data arrived within
     *         [READ_TIMEOUT_MS] (a stall, NOT an end-of-stream)
     * @throws StreamingBufferAbortedException if the buffer was aborted
     */
    suspend fun read(output: ByteArray, offset: Int = 0, length: Int = output.size): Int {
        val result = withTimeoutOrNull(READ_TIMEOUT_MS) {
            while (true) {
                val readCount = mutex.withLock {
                    if (aborted) throw StreamingBufferAbortedException()
                    if (queuedBytes > 0) {
                        var toRead = 0
                        while (toRead < length && chunks.isNotEmpty()) {
                            val chunk = chunks.first()
                            val remainingInChunk = chunk.length
                            if (remainingInChunk > length - toRead) {
                                // Only partially consume the chunk
                                val take = length - toRead
                                System.arraycopy(chunk.data, 0, output, offset + toRead, take)
                                toRead += take
                                chunks.removeFirst()
                                chunks.addFirst(Chunk(chunk.data.copyOfRange(take, chunk.length), chunk.length - take))
                                queuedBytes -= take
                            } else {
                                System.arraycopy(chunk.data, 0, output, offset + toRead, remainingInChunk)
                                toRead += remainingInChunk
                                chunks.removeFirst()
                                queuedBytes -= remainingInChunk
                            }
                        }
                        readBytes += toRead
                        toRead
                    } else if (isStreamComplete) {
                        -1
                    } else {
                        0 // signal: nothing available right now
                    }
                }

                if (readCount != 0) {
                    // Either actual data (>0) or end of stream (-1)
                    return@withTimeoutOrNull readCount
                }

                // Wait a bit before checking again (don't spin too fast)
                delay(POLL_INTERVAL_MS)
            }
            @Suppress("UNREACHABLE_CODE")
            0 // Should never reach here
        }

        // Timed out with no data and no completion: report it as an error,
        // never as a clean EOF (see StreamingReadTimeoutException).
        return result ?: throw StreamingReadTimeoutException()
    }

    /**
     * Get number of bytes available to read
     */
    suspend fun availableBytes(): Int {
        mutex.withLock {
            return queuedBytes
        }
    }

    /**
     * Get total bytes buffered (including already read)
     */
    suspend fun totalBytes(): Long {
        mutex.withLock {
            return totalWrittenBytes
        }
    }

    /**
     * Mark stream as complete (no more data will be written)
     */
    suspend fun markComplete() {
        mutex.withLock {
            isStreamComplete = true
        }
    }

    /**
     * Check if stream is complete
     */
    suspend fun isComplete(): Boolean {
        mutex.withLock {
            return isStreamComplete
        }
    }

    /**
     * Aborts the stream: parked readers AND writers wake up (within one poll
     * interval) and throw [StreamingBufferAbortedException]. Used when the
     * DataSource is closed (so blocked reads cannot pin ExoPlayer's loader
     * thread for up to 30s) and on stream errors.
     *
     * Non-suspending: called from ExoPlayer's loader thread via
     * StreamingAudioSource.close().
     */
    fun abort() {
        aborted = true
    }

    /**
     * Check if the buffer was aborted
     */
    fun isAborted(): Boolean {
        return aborted
    }

    /**
     * Clear buffer and reset read position
     */
    suspend fun clear() {
        mutex.withLock {
            chunks.clear()
            queuedBytes = 0
            totalWrittenBytes = 0
            readBytes = 0
            isStreamComplete = false
            aborted = false
        }
    }

    /**
     * Get buffering progress percentage (0.0 to 1.0)
     * @param targetBytes Target buffer size in bytes
     */
    suspend fun getBufferingProgress(targetBytes: Int): Float {
        mutex.withLock {
            if (targetBytes <= 0) return 0f
            return (queuedBytes.toFloat() / targetBytes.toFloat()).coerceIn(0f, 1f)
        }
    }
}

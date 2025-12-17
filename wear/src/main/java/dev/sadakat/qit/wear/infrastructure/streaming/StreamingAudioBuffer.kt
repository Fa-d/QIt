package dev.sadakat.qit.wear.infrastructure.streaming

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream

/**
 * Thread-safe circular buffer for streaming audio data
 * Handles concurrent writes from channel and reads from ExoPlayer
 */
class StreamingAudioBuffer {

    private val buffer = ByteArrayOutputStream()
    private val mutex = Mutex()
    private var readPosition = 0
    private var isStreamComplete = false

    companion object {
        private const val READ_TIMEOUT_MS = 30000L // 30 seconds timeout for waiting for data
        private const val POLL_INTERVAL_MS = 50L // Check for data every 50ms
    }

    /**
     * Write audio data to buffer
     * @param data Audio data to write
     * @param offset Starting position in data array
     * @param length Number of bytes to write
     */
    suspend fun write(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        mutex.withLock {
            buffer.write(data, offset, length)
        }
    }

    /**
     * Read audio data from buffer
     * BLOCKS until data is available or stream is complete
     * @param output Destination array
     * @param offset Starting position in output array
     * @param length Maximum number of bytes to read
     * @return Number of bytes actually read, or -1 if stream is complete and no more data
     */
    suspend fun read(output: ByteArray, offset: Int = 0, length: Int = output.size): Int {
        // Wait for data to be available with timeout
        val result = withTimeoutOrNull(READ_TIMEOUT_MS) {
            // Poll until data is available or stream is complete
            while (true) {
                mutex.withLock {
                    val available = buffer.size() - readPosition

                    // If we have data, read it
                    if (available > 0) {
                        val bytesToRead = minOf(length, available)
                        val bufferArray = buffer.toByteArray()
                        System.arraycopy(bufferArray, readPosition, output, offset, bytesToRead)
                        readPosition += bytesToRead
                        return@withTimeoutOrNull bytesToRead
                    }

                    // If stream is complete and no data, signal end
                    if (isStreamComplete) {
                        return@withTimeoutOrNull -1
                    }
                }

                // Wait a bit before checking again (don't spin too fast)
                delay(POLL_INTERVAL_MS)
            }
            0 // Should never reach here
        }

        // If timeout occurred, return -1 to signal error/end
        return result ?: -1
    }

    /**
     * Get number of bytes available to read
     */
    suspend fun availableBytes(): Int {
        mutex.withLock {
            return buffer.size() - readPosition
        }
    }

    /**
     * Get total bytes buffered (including already read)
     */
    suspend fun totalBytes(): Int {
        mutex.withLock {
            return buffer.size()
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
     * Clear buffer and reset read position
     */
    suspend fun clear() {
        mutex.withLock {
            buffer.reset()
            readPosition = 0
            isStreamComplete = false
        }
    }

    /**
     * Get buffering progress percentage (0.0 to 1.0)
     * @param targetBytes Target buffer size in bytes
     */
    suspend fun getBufferingProgress(targetBytes: Int): Float {
        mutex.withLock {
            val available = buffer.size() - readPosition
            return (available.toFloat() / targetBytes).coerceIn(0f, 1f)
        }
    }
}

package dev.sadakat.qit.wear.infrastructure.streaming

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
     * @param output Destination array
     * @param offset Starting position in output array
     * @param length Maximum number of bytes to read
     * @return Number of bytes actually read, or -1 if stream is complete and no more data
     */
    suspend fun read(output: ByteArray, offset: Int = 0, length: Int = output.size): Int {
        mutex.withLock {
            val available = buffer.size() - readPosition

            // If no data available
            if (available <= 0) {
                return if (isStreamComplete) -1 else 0
            }

            // Calculate how much we can actually read
            val bytesToRead = minOf(length, available)

            // Copy data from buffer
            val bufferArray = buffer.toByteArray()
            System.arraycopy(bufferArray, readPosition, output, offset, bytesToRead)

            readPosition += bytesToRead

            return bytesToRead
        }
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

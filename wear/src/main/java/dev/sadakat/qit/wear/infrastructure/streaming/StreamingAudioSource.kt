package dev.sadakat.qit.wear.infrastructure.streaming

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import kotlinx.coroutines.runBlocking

/**
 * Custom ExoPlayer DataSource that reads from streaming buffer
 * Provides audio data from phone stream to ExoPlayer for playback
 */
class StreamingAudioSource(
    private val audioBuffer: StreamingAudioBuffer,
    private val minimumBufferBytes: Int = MIN_BUFFER_BYTES
) : BaseDataSource(/* isNetwork = */ true) {

    private var dataSpec: DataSpec? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.opened = true

        transferInitializing(dataSpec)

        // For streaming, we don't know the length upfront
        bytesRemaining = C.LENGTH_UNSET.toLong()

        transferStarted(dataSpec)

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        }

        // Read from audio buffer (blocking call)
        return runBlocking {
            val bytesRead = audioBuffer.read(buffer, offset, length)

            when {
                bytesRead > 0 -> {
                    bytesTransferred(bytesRead)
                    bytesRead
                }
                bytesRead == -1 -> {
                    // Stream complete and no more data
                    C.RESULT_END_OF_INPUT
                }
                else -> {
                    // No data available yet, but stream not complete
                    // Return 0 to indicate we should try again
                    0
                }
            }
        }
    }

    override fun getUri(): Uri? {
        return dataSpec?.uri
    }

    override fun close() {
        dataSpec = null
        opened = false
        transferEnded()
    }

    /**
     * Check if buffer has enough data to start playback
     */
    suspend fun hasMinimumBuffer(): Boolean {
        return audioBuffer.availableBytes() >= minimumBufferBytes
    }

    /**
     * Get current buffering progress (0.0 to 1.0)
     */
    suspend fun getBufferingProgress(): Float {
        return audioBuffer.getBufferingProgress(minimumBufferBytes)
    }

    companion object {
        // Minimum buffer size before starting playback (2 seconds of high-quality audio)
        private const val MIN_BUFFER_BYTES = 256 * 1024 / 8 * 2 // ~64KB for 2 seconds at 256kbps
    }

    /**
     * Factory for creating StreamingAudioSource instances
     */
    class Factory(
        private val audioBuffer: StreamingAudioBuffer
    ) : DataSource.Factory {

        override fun createDataSource(): DataSource {
            return StreamingAudioSource(audioBuffer)
        }
    }
}

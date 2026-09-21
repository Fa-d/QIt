package dev.sadakat.qit.wear.infrastructure.streaming

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
import kotlinx.coroutines.runBlocking

/**
 * Custom ExoPlayer DataSource that reads from streaming buffer
 * Provides audio data from phone stream to ExoPlayer for playback
 */
@OptIn(UnstableApi::class)
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

        // The streaming buffer is a FIFO filled from byte 0 of the song; it
        // cannot serve byte-accurate seeks. Non-zero open positions only
        // occur for seeks - fail loudly instead of silently playing the
        // wrong bytes. (User seeks are disabled while streaming; retries
        // restart the stream from 0.)
        if (dataSpec.position != 0L) {
            throw IOException(
                "Seeking within a live stream is not supported (position=${dataSpec.position})"
            )
        }

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

        // Read from audio buffer (blocking call).
        //
        // Errors are propagated as IOExceptions (StreamingReadTimeoutException
        // for stalls, StreamingBufferAbortedException when the stream was
        // stopped/errored) so ExoPlayer raises onPlayerError -> the existing
        // handlePlaybackError / handleConnectionLoss paths run. A clean EOF
        // (-1) is only returned when the buffer is complete AND drained.
        val bytesRead = runBlocking {
            audioBuffer.read(buffer, offset, length)
        }

        return when {
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

    override fun getUri(): Uri? {
        return dataSpec?.uri
    }

    override fun close() {
        if (opened) {
            // Abort the buffer FIRST: a read may be parked on ExoPlayer's
            // loader thread for up to 30s; without this signal close() (and
            // therefore release()) cannot prompt the loader to shut down.
            audioBuffer.abort()
        }
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

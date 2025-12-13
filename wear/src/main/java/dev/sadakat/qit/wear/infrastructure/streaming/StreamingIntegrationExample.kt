package dev.sadakat.qit.wear.infrastructure.streaming

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.StreamingStatus
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Example integration of streaming components with ExoPlayer
 * This file demonstrates how to use WearStreamingRepository, StreamingAudioBuffer,
 * and StreamingAudioSource together for streaming playback.
 *
 * NOTE: This is an example/reference file, not production code.
 */
@Suppress("unused")
class StreamingIntegrationExample(
    private val context: Context,
    private val streamingRepository: StreamingRepository,
    private val wearStreamingRepository: WearStreamingRepository,
    private val coroutineScope: CoroutineScope
) {

    private var player: ExoPlayer? = null

    /**
     * Example 1: Stream a song from phone and play it
     */
    fun streamAndPlaySong(songId: SongId) {
        coroutineScope.launch {
            // Step 1: Request stream from phone
            val result = streamingRepository.requestStreamFromPhone(
                songId = songId,
                quality = AudioQuality.MEDIUM
            )

            if (result.isFailure) {
                // Handle error
                return@launch
            }

            // Step 2: Observe streaming status
            streamingRepository.observeStreamingStatus().collect { status ->
                when (status) {
                    is StreamingStatus.Idle -> {
                        // Waiting for stream
                    }

                    is StreamingStatus.Buffering -> {
                        // Show buffering progress: ${status.progress * 100}%
                        // When enough data is buffered, status will change to Streaming
                    }

                    is StreamingStatus.Streaming -> {
                        // Stream is ready, initialize player if not already done
                        if (player == null) {
                            initializePlayerForStreaming(songId)
                        }
                    }

                    is StreamingStatus.Error -> {
                        // Handle error: ${status.message}
                        stopPlayback()
                    }
                }
            }
        }
    }

    /**
     * Example 2: Initialize ExoPlayer with streaming data source
     */
    private fun initializePlayerForStreaming(songId: SongId) {
        // Get the audio buffer from repository
        val audioBuffer = wearStreamingRepository.getAudioBuffer()

        // Create DataSource factory that reads from buffer
        val dataSourceFactory = StreamingAudioSource.Factory(audioBuffer)

        // Build ExoPlayer with streaming data source
        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
            )
            .build()

        // Create media item
        // Note: URI is just a placeholder, actual data comes from buffer
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse("streaming://song/${songId.value}"))
            .setMediaId(songId.value)
            .build()

        // Set media item and prepare
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            // ExoPlayer will start playback when enough data is buffered
            playWhenReady = true
        }
    }

    /**
     * Example 3: Stop streaming and cleanup
     */
    fun stopPlayback() {
        coroutineScope.launch {
            // Get current song ID (would come from your state management)
            val currentSongId = SongId.from("current-song-id")

            // Stop streaming
            streamingRepository.stopStreaming(currentSongId)

            // Release player
            player?.release()
            player = null
        }
    }

    /**
     * Example 4: Monitor buffering and playback state
     */
    fun observeStreamingWithPlaybackControl(songId: SongId) {
        coroutineScope.launch {
            streamingRepository.observeStreamingStatus().collect { status ->
                when (status) {
                    is StreamingStatus.Buffering -> {
                        // Show loading indicator
                        val progress = (status.progress * 100).toInt()
                        // updateUI("Buffering $progress%")

                        // Pause playback if buffer is too low
                        if (status.progress < 0.3f && player?.isPlaying == true) {
                            player?.pause()
                        }
                    }

                    is StreamingStatus.Streaming -> {
                        // Hide loading indicator
                        // updateUI("Playing")

                        // Resume playback if paused
                        if (player?.playWhenReady == true && player?.isPlaying == false) {
                            player?.play()
                        }
                    }

                    is StreamingStatus.Error -> {
                        // Show error message
                        // updateUI("Error: ${status.message}")
                        stopPlayback()
                    }

                    is StreamingStatus.Idle -> {
                        // Reset UI
                    }
                }
            }
        }
    }

    /**
     * Example 5: Get recommended quality based on connection
     */
    suspend fun selectOptimalQuality(): AudioQuality {
        return streamingRepository.getRecommendedQuality()
    }

    /**
     * Example 6: Complete flow from request to playback
     */
    fun completeStreamingFlow(songId: SongId) {
        coroutineScope.launch {
            // 1. Get recommended quality
            val quality = streamingRepository.getRecommendedQuality()

            // 2. Request stream
            val result = streamingRepository.requestStreamFromPhone(songId, quality)

            if (result.isFailure) {
                // Handle error
                return@launch
            }

            // 3. Observe status and control playback
            streamingRepository.observeStreamingStatus().collect { status ->
                when (status) {
                    is StreamingStatus.Buffering -> {
                        // Initial buffering - wait for threshold
                        if (status.progress >= 0.5f && player == null) {
                            // Enough buffered to start
                            initializePlayerForStreaming(songId)
                        }
                    }

                    is StreamingStatus.Streaming -> {
                        // Ensure player is created and playing
                        if (player == null) {
                            initializePlayerForStreaming(songId)
                        }
                    }

                    is StreamingStatus.Error -> {
                        stopPlayback()
                    }

                    is StreamingStatus.Idle -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    /**
     * Cleanup when component is destroyed
     */
    fun cleanup() {
        player?.release()
        player = null
        wearStreamingRepository.cleanup()
    }
}

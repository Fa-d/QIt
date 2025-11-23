package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing the state of audio playback
 */
sealed class PlaybackState {

    object Idle : PlaybackState() {
        override fun toString() = "Idle"
    }

    data class Buffering(val progress: Float) : PlaybackState() {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1" }
        }
        override fun toString() = "Buffering ${(progress * 100).toInt()}%"
    }

    data class Ready(
        val currentPosition: Duration,
        val totalDuration: Duration
    ) : PlaybackState() {
        val progress: Float get() {
            if (totalDuration.milliseconds == 0L) return 0f
            return (currentPosition.milliseconds.toFloat() / totalDuration.milliseconds.toFloat()).coerceIn(0f, 1f)
        }

        override fun toString() = "Ready ${currentPosition.format()} / ${totalDuration.format()}"
    }

    data class Playing(
        val currentPosition: Duration,
        val totalDuration: Duration
    ) : PlaybackState() {
        val progress: Float get() {
            if (totalDuration.milliseconds == 0L) return 0f
            return (currentPosition.milliseconds.toFloat() / totalDuration.milliseconds.toFloat()).coerceIn(0f, 1f)
        }

        override fun toString() = "Playing ${currentPosition.format()} / ${totalDuration.format()}"
    }

    data class Paused(
        val currentPosition: Duration,
        val totalDuration: Duration
    ) : PlaybackState() {
        override fun toString() = "Paused ${currentPosition.format()} / ${totalDuration.format()}"
    }

    data class Ended(val totalDuration: Duration) : PlaybackState() {
        override fun toString() = "Ended"
    }

    data class Error(val message: String) : PlaybackState() {
        override fun toString() = "Error: $message"
    }

    fun isPlaying(): Boolean = this is Playing
    fun isPaused(): Boolean = this is Paused
    fun canPlay(): Boolean = this is Ready || this is Paused || this is Ended
}

package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing the download status of a song
 */
sealed class DownloadStatus {

    object NotDownloaded : DownloadStatus() {
        override fun toString() = "Not Downloaded"
    }

    data class Downloading(val progress: Float) : DownloadStatus() {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1" }
        }

        fun percentageString(): String = "${(progress * 100).toInt()}%"

        override fun toString() = "Downloading ${percentageString()}"
    }

    data class Downloaded(val localPath: String) : DownloadStatus() {
        override fun toString() = "Downloaded"
    }

    data class Failed(val error: String) : DownloadStatus() {
        override fun toString() = "Download Failed: $error"
    }

    data class Paused(val progress: Float) : DownloadStatus() {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1" }
        }

        override fun toString() = "Paused at ${(progress * 100).toInt()}%"
    }

    fun isDownloaded(): Boolean = this is Downloaded
    fun isInProgress(): Boolean = this is Downloading
    fun canResume(): Boolean = this is Paused || this is Failed
}

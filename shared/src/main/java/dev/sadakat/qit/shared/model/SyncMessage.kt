package dev.sadakat.qit.shared.model

sealed class SyncMessage {

    data class PlaylistSync(
        val playlists: List<Playlist>,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncMessage()

    data class SongSync(
        val songs: List<Song>,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncMessage()

    data class DownloadRequest(
        val songId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncMessage()

    data class DownloadProgress(
        val songId: String,
        val progress: Float, // 0.0 to 1.0
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncMessage()

    data class DownloadComplete(
        val songId: String,
        val success: Boolean,
        val errorMessage: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncMessage()

    data class PlaybackCommand(
        val command: PlaybackCommandType,
        val songId: String? = null,
        val position: Long? = null, // Position in milliseconds
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncMessage()

    data class ConnectionStatus(
        val isConnected: Boolean,
        val deviceName: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncMessage()
}

enum class PlaybackCommandType {
    PLAY,
    PAUSE,
    STOP,
    NEXT,
    PREVIOUS,
    SEEK
}

package dev.sadakat.qit.shared.constants

object WearPaths {
    // Data Layer paths for synchronized data
    const val PLAYLISTS_DATA = "/playlists"
    const val SONGS_DATA = "/songs"
    const val SETTINGS_DATA = "/settings"

    // Message paths for fire-and-forget messages
    const val REQUEST_PLAYLIST_SYNC = "/request/playlist_sync"
    const val REQUEST_SONG_SYNC = "/request/song_sync"
    const val REQUEST_DELTA_SYNC = "/request/delta_sync"
    const val REQUEST_FULL_SYNC = "/request/full_sync"
    const val PLAYLIST_SYNC = "/sync/playlists"
    const val SONG_SYNC = "/sync/songs"
    const val DOWNLOAD_REQUEST = "/download/request"
    const val DOWNLOAD_CANCEL = "/download/cancel"
    const val DOWNLOAD_START = "/download/start"
    const val DOWNLOAD_PROGRESS = "/download/progress"
    const val DOWNLOAD_COMPLETE = "/download/complete"
    const val PLAYBACK_COMMAND = "/playback/command"
    const val CONNECTION_STATUS = "/connection/status"
    const val WATCH_VERSION_ANNOUNCEMENT = "/watch/version"

    // Channel paths for streaming large data
    const val AUDIO_STREAM = "/stream/audio/"
    const val DOWNLOAD_CHANNEL = "/download/"

    /**
     * Message path used by the phone to negatively acknowledge a failed
     * audio-stream request (song missing, file unreadable, ...). Payload is a
     * [dev.sadakat.qit.shared.dto.StreamErrorMessage]. Without this the watch
     * would buffer for 30s and then silently "end" the track.
     */
    const val STREAM_AUDIO_ERROR = "/stream/audio/error"

    // Capability paths
    const val CAPABILITY_PHONE_APP = "qit_phone_app"
    const val CAPABILITY_WATCH_APP = "qit_watch_app"
}

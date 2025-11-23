package dev.sadakat.qit.service

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dev.sadakat.qit.shared.constants.WearPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service that listens for messages and data events from the WearOS watch
 */
class WatchDataService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        when (messageEvent.path) {
            WearPaths.REQUEST_PLAYLIST_SYNC -> {
                handlePlaylistSyncRequest(messageEvent.sourceNodeId)
            }
            WearPaths.REQUEST_SONG_SYNC -> {
                handleSongSyncRequest(messageEvent.sourceNodeId)
            }
            WearPaths.DOWNLOAD_REQUEST -> {
                val songId = String(messageEvent.data)
                handleDownloadRequest(messageEvent.sourceNodeId, songId)
            }
            WearPaths.PLAYBACK_COMMAND -> {
                val command = String(messageEvent.data)
                handlePlaybackCommand(command)
            }
        }
    }

    private fun handlePlaylistSyncRequest(nodeId: String) {
        serviceScope.launch {
            // TODO: Get playlists from database and sync to watch
            // val playlistRepository = ...
            // val playlists = playlistRepository.getAllPlaylists().first()
            // watchSyncRepository.syncPlaylistsToWatch(playlists)
        }
    }

    private fun handleSongSyncRequest(nodeId: String) {
        serviceScope.launch {
            // TODO: Get songs from database and sync to watch
            // val musicRepository = ...
            // val songs = musicRepository.getAllSongs().first()
            // watchSyncRepository.syncSongsToWatch(songs)
        }
    }

    private fun handleDownloadRequest(nodeId: String, songId: String) {
        serviceScope.launch {
            // TODO: Get song from database and send file to watch
            // val musicRepository = ...
            // val song = musicRepository.getSongById(songId)
            // song?.filePath?.let { filePath ->
            //     watchSyncRepository.sendAudioFileToWatch(songId, filePath)
            // }
        }
    }

    private fun handlePlaybackCommand(command: String) {
        serviceScope.launch {
            // TODO: Handle playback commands from watch
            // This could control local playback or relay commands
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

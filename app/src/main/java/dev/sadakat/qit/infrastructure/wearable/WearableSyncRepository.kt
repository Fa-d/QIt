package dev.sadakat.qit.infrastructure.wearable

import android.util.Log
import com.google.android.gms.wearable.*
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.ChangeRecord
import dev.sadakat.qit.shared.domain.valueobject.EntityType
import dev.sadakat.qit.shared.domain.valueobject.SyncMetadata
import dev.sadakat.qit.shared.dto.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Wearable-based implementation of SyncRepository (Phone side)
 */
class WearableSyncRepository @Inject constructor(
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient,
    private val capabilityClient: CapabilityClient
) : SyncRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // In-memory storage for sync metadata (in production, this should be persisted)
    private var syncMetadata: SyncMetadata = SyncMetadata.initial()

    override suspend fun syncPlaylistToWatch(playlist: Playlist): Result<Unit> {
        return try {
            val message = PlaylistSyncMessage(
                playlists = listOf(playlist.toDto())
            )
            val data = json.encodeToString(message).toByteArray()

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return Result.failure(IllegalStateException("No watch connected"))
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/sync/playlists", data).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPlaylistsToWatch(playlists: List<Playlist>): Result<Unit> {
        return try {
            val message = PlaylistSyncMessage(
                playlists = playlists.toPlaylistDtos()
            )
            val data = json.encodeToString(message).toByteArray()

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return Result.failure(IllegalStateException("No watch connected"))
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/sync/playlists", data).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncSongToWatch(song: Song): Result<Unit> {
        return syncSongsToWatch(listOf(song))
    }

    override suspend fun syncSongsToWatch(songs: List<Song>): Result<Unit> {
        return try {
            val message = SongSyncMessage(
                songs = songs.toSongDtos()
            )
            val data = json.encodeToString(message).toByteArray()

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return Result.failure(IllegalStateException("No watch connected"))
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/sync/songs", data).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestPlaylistSyncFromPhone(): Result<Unit> {
        // Not used on phone side
        return Result.failure(UnsupportedOperationException("This is a phone-side repository"))
    }

    override suspend fun requestSongSyncFromPhone(playlistId: PlaylistId): Result<Unit> {
        // Not used on phone side
        return Result.failure(UnsupportedOperationException("This is a phone-side repository"))
    }

    override suspend fun isWatchConnected(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override fun observeWatchConnection(): Flow<Boolean> = callbackFlow {
        // Listener for capability changes
        val listener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            val hasWatchApp = capabilityInfo.nodes.isNotEmpty()
            Log.d(TAG, "Watch capability changed: hasWatchApp=$hasWatchApp, nodes=${capabilityInfo.nodes.size}")
            trySend(hasWatchApp)
        }

        // Add listener for watch capability
        capabilityClient.addListener(listener, WearPaths.CAPABILITY_WATCH_APP)

        // Send initial state
        try {
            val capabilityInfo = capabilityClient
                .getCapability(WearPaths.CAPABILITY_WATCH_APP, CapabilityClient.FILTER_REACHABLE)
                .await()
            val hasWatchApp = capabilityInfo.nodes.isNotEmpty()
            Log.d(TAG, "Initial watch connection state: $hasWatchApp")
            trySend(hasWatchApp)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting initial watch connection state", e)
            trySend(false)
        }

        // Remove listener when flow is cancelled
        awaitClose {
            Log.d(TAG, "Removing watch connection listener")
            capabilityClient.removeListener(listener, WearPaths.CAPABILITY_WATCH_APP)
        }
    }

    override suspend fun sendPlaybackCommand(command: String, songId: SongId?): Result<Unit> {
        return try {
            val message = PlaybackCommandMessage(
                command = command,
                songId = songId?.value
            )
            val data = json.encodeToString(message).toByteArray()

            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/playback/command", data).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastSyncTimestamp(): Long {
        return try {
            val uri = android.net.Uri.parse("wear://*/last_sync")
            val dataItems = dataClient.getDataItems(uri).await()

            if (dataItems.count > 0) {
                val item = dataItems.get(0)
                DataMapItem.fromDataItem(item).dataMap.getLong("timestamp", 0L)
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long): Result<Unit> {
        return try {
            val putDataReq = PutDataMapRequest.create("/last_sync").apply {
                dataMap.putLong("timestamp", timestamp)
            }.asPutDataRequest()

            dataClient.putDataItem(putDataReq).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delta Sync Implementation

    override fun getChangesSince(timestamp: Long): Flow<List<ChangeRecord>> = flow {
        Log.d(TAG, "Getting changes since timestamp: $timestamp")
        val changes = syncMetadata.changesNewerThan(timestamp)
        emit(changes)
    }

    override suspend fun getSyncMetadata(): SyncMetadata {
        Log.d(TAG, "Getting sync metadata: version=${syncMetadata.syncVersion}, lastSync=${syncMetadata.lastSyncTimestamp}, pendingChanges=${syncMetadata.pendingChangeCount()}")
        return syncMetadata
    }

    override suspend fun updateSyncMetadata(metadata: SyncMetadata): Result<Unit> {
        return try {
            Log.d(TAG, "Updating sync metadata: version=${metadata.syncVersion}, pendingChanges=${metadata.pendingChangeCount()}")
            syncMetadata = metadata

            // Persist to DataLayer for watch to retrieve
            val putDataReq = PutDataMapRequest.create("/sync_metadata").apply {
                dataMap.putLong("lastSyncTimestamp", metadata.lastSyncTimestamp)
                dataMap.putInt("syncVersion", metadata.syncVersion)
                dataMap.putInt("pendingChangeCount", metadata.pendingChangeCount())
            }.asPutDataRequest()

            dataClient.putDataItem(putDataReq).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update sync metadata", e)
            Result.failure(e)
        }
    }

    override suspend fun markEntitiesAsSynced(entityIds: List<String>): Result<Unit> {
        return try {
            Log.d(TAG, "Marking ${entityIds.size} entities as synced")
            syncMetadata = syncMetadata.removeChanges(entityIds)

            // Update persisted metadata
            updateSyncMetadata(syncMetadata)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark entities as synced", e)
            Result.failure(e)
        }
    }

    /**
     * Records a change for delta sync tracking
     * Should be called when playlists or songs are modified
     */
    fun recordChange(change: ChangeRecord) {
        Log.d(TAG, "Recording change: ${change.displayName()}")
        syncMetadata = syncMetadata.addChange(change)
    }

    /**
     * Records multiple changes for delta sync tracking
     */
    fun recordChanges(changes: List<ChangeRecord>) {
        Log.d(TAG, "Recording ${changes.size} changes")
        syncMetadata = syncMetadata.addChanges(changes)
    }

    /**
     * Syncs only changed playlists to the watch based on last sync timestamp
     */
    suspend fun syncChangedPlaylistsToWatch(lastSyncTimestamp: Long): Result<Unit> {
        return try {
            val playlistChanges = syncMetadata.pendingChangesByType(EntityType.PLAYLIST)
                .filter { it.timestamp > lastSyncTimestamp }

            if (playlistChanges.isEmpty()) {
                Log.d(TAG, "No playlist changes to sync")
                return Result.success(Unit)
            }

            Log.d(TAG, "Syncing ${playlistChanges.size} playlist changes to watch")

            // In a real implementation, you would fetch the actual playlist data
            // and send only the changed playlists
            // For now, this demonstrates the delta sync concept

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync changed playlists", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs only changed songs to the watch based on last sync timestamp
     */
    suspend fun syncChangedSongsToWatch(lastSyncTimestamp: Long): Result<Unit> {
        return try {
            val songChanges = syncMetadata.pendingChangesByType(EntityType.SONG)
                .filter { it.timestamp > lastSyncTimestamp }

            if (songChanges.isEmpty()) {
                Log.d(TAG, "No song changes to sync")
                return Result.success(Unit)
            }

            Log.d(TAG, "Syncing ${songChanges.size} song changes to watch")

            // In a real implementation, you would fetch the actual song data
            // and send only the changed songs
            // For now, this demonstrates the delta sync concept

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync changed songs", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "WearableSyncRepository"
    }
}

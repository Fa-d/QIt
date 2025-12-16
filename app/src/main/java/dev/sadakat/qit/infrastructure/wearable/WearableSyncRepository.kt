package dev.sadakat.qit.infrastructure.wearable

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.wearable.*
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.ChangeRecord
import dev.sadakat.qit.shared.domain.valueobject.ConnectionDiagnostics
import dev.sadakat.qit.shared.domain.valueobject.EntityType
import dev.sadakat.qit.shared.domain.valueobject.SyncMetadata
import dev.sadakat.qit.shared.domain.valueobject.WatchAppStatus
import dev.sadakat.qit.shared.domain.valueobject.WatchNode
import dev.sadakat.qit.shared.dto.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_metadata")

/**
 * Wearable-based implementation of SyncRepository (Phone side)
 */
class WearableSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient,
    private val capabilityClient: CapabilityClient
) : SyncRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val lastSyncKey = longPreferencesKey("last_sync_timestamp")
    private val watchVersionKey = stringPreferencesKey("watch_app_version")
    private val syncDataStore = context.syncDataStore

    // Sync metadata with persistent storage
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

    override suspend fun getWatchAppStatus(): Result<WatchAppStatus> {
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(WearPaths.CAPABILITY_WATCH_APP, CapabilityClient.FILTER_ALL)
                .await()

            val nodes = capabilityInfo.nodes.map { node ->
                WatchNode(
                    nodeId = node.id,
                    displayName = node.displayName,
                    isNearby = node.isNearby
                )
            }

            // Get stored watch version
            val appVersion = getStoredWatchVersion()

            // Check Bluetooth status
            val bluetoothEnabled = isBluetoothEnabled()

            val diagnostics = ConnectionDiagnostics(
                bluetoothEnabled = bluetoothEnabled,
                hasCapability = nodes.isNotEmpty(),
                nodeCount = nodes.size,
                lastCheckTimestamp = System.currentTimeMillis(),
                errorMessage = null
            )

            val status = if (nodes.isEmpty()) {
                WatchAppStatus.notInstalled()
            } else {
                WatchAppStatus.installed(nodes, diagnostics, appVersion)
            }

            Log.d(TAG, "Watch app status: isInstalled=${status.isInstalled}, isConnected=${status.isConnected}, version=$appVersion")
            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get watch app status", e)

            val diagnostics = ConnectionDiagnostics(
                bluetoothEnabled = false,
                hasCapability = false,
                nodeCount = 0,
                lastCheckTimestamp = System.currentTimeMillis(),
                errorMessage = e.message
            )

            Result.success(WatchAppStatus.notInstalled().copy(
                connectionDiagnostics = diagnostics
            ))
        }
    }

    override fun observeWatchAppStatus(): Flow<WatchAppStatus> = callbackFlow {
        val listener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            val nodes = capabilityInfo.nodes.map { node ->
                WatchNode(
                    nodeId = node.id,
                    displayName = node.displayName,
                    isNearby = node.isNearby
                )
            }

            // Get stored watch version
            val appVersion = try {
                runBlocking {
                    syncDataStore.data.map { preferences ->
                        preferences[watchVersionKey]
                    }.first()
                }
            } catch (e: Exception) {
                null
            }

            val bluetoothEnabled = isBluetoothEnabled()

            val diagnostics = ConnectionDiagnostics(
                bluetoothEnabled = bluetoothEnabled,
                hasCapability = nodes.isNotEmpty(),
                nodeCount = nodes.size,
                lastCheckTimestamp = System.currentTimeMillis(),
                errorMessage = null
            )

            val status = if (nodes.isEmpty()) {
                WatchAppStatus.notInstalled()
            } else {
                WatchAppStatus.installed(nodes, diagnostics, appVersion)
            }

            Log.d(TAG, "Watch app status changed: isInstalled=${status.isInstalled}, isConnected=${status.isConnected}")
            trySend(status)
        }

        capabilityClient.addListener(listener, WearPaths.CAPABILITY_WATCH_APP)

        // Send initial state
        try {
            val initialStatus = getWatchAppStatus().getOrNull()
            if (initialStatus != null) {
                trySend(initialStatus)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting initial watch app status", e)
        }

        awaitClose {
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
            // First check persistent storage
            syncDataStore.data.map { preferences ->
                preferences[lastSyncKey] ?: 0L
            }.first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last sync timestamp", e)
            0L
        }
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long): Result<Unit> {
        return try {
            // Update persistent storage
            syncDataStore.edit { preferences ->
                preferences[lastSyncKey] = timestamp
            }

            // Also sync to wearable data layer
            val putDataReq = PutDataMapRequest.create("/last_sync").apply {
                dataMap.putLong("timestamp", timestamp)
            }.asPutDataRequest()

            dataClient.putDataItem(putDataReq).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last sync timestamp", e)
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

            // Extract changed playlist IDs
            val changedPlaylistIds = playlistChanges
                .map { PlaylistId.from(it.entityId) }
                .distinct()

            // Fetch actual playlists from repository
            val changedPlaylists = mutableListOf<Playlist>()
            // Note: In a real implementation, we'd need a method to get playlists by IDs
            // For now, we'll fetch all playlists and filter
            playlistRepository.getAllPlaylists().first().forEach { playlist ->
                if (playlist.id in changedPlaylistIds) {
                    changedPlaylists.add(playlist)
                }
            }

            if (changedPlaylists.isNotEmpty()) {
                // Sync the changed playlists to watch
                syncPlaylistsToWatch(changedPlaylists)
            }

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

            // Extract changed song IDs
            val changedSongIds = songChanges
                .map { SongId.from(it.entityId) }
                .distinct()

            // Fetch actual songs from repository
            val changedSongs = mutableListOf<Song>()
            changedSongIds.forEach { songId ->
                val result = musicRepository.getSongById(songId)
                if (result.isSuccess) {
                    result.getOrNull()?.let { song ->
                        changedSongs.add(song)
                    }
                }
            }

            if (changedSongs.isNotEmpty()) {
                // Sync the changed songs to watch
                syncSongsToWatch(changedSongs)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync changed songs", e)
            Result.failure(e)
        }
    }

    /**
     * Gets the stored watch app version from DataStore
     */
    private suspend fun getStoredWatchVersion(): String? {
        return try {
            syncDataStore.data.map { preferences ->
                preferences[watchVersionKey]
            }.first()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get stored watch version", e)
            null
        }
    }

    /**
     * Stores the watch app version in DataStore
     */
    suspend fun storeWatchVersion(version: String) {
        try {
            syncDataStore.edit { preferences ->
                preferences[watchVersionKey] = version
            }
            Log.d(TAG, "Stored watch app version: $version")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store watch version", e)
        }
    }

    /**
     * Checks if Bluetooth is enabled on the device
     */
    private fun isBluetoothEnabled(): Boolean {
        return try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter?.isEnabled ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check Bluetooth status", e)
            false
        }
    }

    companion object {
        private const val TAG = "WearableSyncRepository"
    }
}

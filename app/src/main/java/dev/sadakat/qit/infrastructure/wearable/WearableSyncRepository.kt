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
import kotlinx.coroutines.launch
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

    // Last known watch app version, kept in memory to avoid blocking reads
    // on the DataStore from capability-change listener callbacks.
    @Volatile
    private var cachedWatchVersion: String? = null

    override suspend fun syncPlaylistToWatch(playlist: Playlist): Result<Unit> {
        // Route through the chunked plural variant: a single playlist with a
        // very large songIds list must also stay under the message cap.
        return syncPlaylistsToWatch(listOf(playlist))
    }

    override suspend fun syncPlaylistsToWatch(
        playlists: List<Playlist>,
        fullSync: Boolean
    ): Result<Unit> {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return Result.failure(IllegalStateException("No watch connected"))
            }

            // Chunk by serialized byte budget (not count): a fixed count of
            // playlists can exceed the ~100KB MessageClient cap with real
            // paths/CJK metadata. Oversized playlists are split across
            // multiple PlaylistDtos with the same id; the watch merges
            // songIds for same-id chunks received within one burst.
            val current = mutableListOf<PlaylistDto>()
            var currentBytes = 0

            suspend fun flushChunk() {
                if (current.isEmpty()) return
                val message = PlaylistSyncMessage(playlists = current.toList(), fullSync = fullSync)
                val data = json.encodeToString(message).toByteArray()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, WearPaths.PLAYLIST_SYNC, data).await()
                }
                current.clear()
                currentBytes = 0
            }

            for (playlist in playlists) {
                val dto = playlist.toDto()
                val dtoBytes = json.encodeToString(dto).toByteArray().size
                if (dtoBytes <= MAX_MESSAGE_BYTES) {
                    if (currentBytes + dtoBytes > MAX_MESSAGE_BYTES) flushChunk()
                    current.add(dto)
                    currentBytes += dtoBytes
                } else {
                    // Single playlist alone exceeds the budget: split its
                    // songIds across continuation DTOs (same id).
                    val ids = dto.songIds
                    var index = 0
                    while (index < ids.size) {
                        val part = dto.copy(
                            songIds = ids.subList(
                                index,
                                minOf(index + SONG_IDS_PER_SPLIT_PART, ids.size)
                            )
                        )
                        val partBytes = json.encodeToString(part).toByteArray().size
                        if (currentBytes + partBytes > MAX_MESSAGE_BYTES) flushChunk()
                        current.add(part)
                        currentBytes += partBytes
                        index += SONG_IDS_PER_SPLIT_PART
                    }
                }
            }
            flushChunk()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncSongToWatch(song: Song): Result<Unit> {
        return syncSongsToWatch(listOf(song))
    }

    override suspend fun syncSongsToWatch(
        songs: List<Song>,
        fullSync: Boolean
    ): Result<Unit> {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return Result.failure(IllegalStateException("No watch connected"))
            }

            // MessageClient payloads are limited (~100KB). Chunk by serialized
            // byte budget so the chunk count adapts to the real payload size
            // (paths/CJK metadata can be several times larger than ASCII).
            val current = mutableListOf<SongDto>()
            var currentBytes = 0

            for (song in songs) {
                val dto = song.toDto()
                val dtoBytes = json.encodeToString(dto).toByteArray().size
                if (current.isNotEmpty() && currentBytes + dtoBytes > MAX_MESSAGE_BYTES) {
                    val message = SongSyncMessage(songs = current.toList(), fullSync = fullSync)
                    val data = json.encodeToString(message).toByteArray()
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, WearPaths.SONG_SYNC, data).await()
                    }
                    current.clear()
                    currentBytes = 0
                }
                current.add(dto)
                currentBytes += dtoBytes
            }
            if (current.isNotEmpty()) {
                val message = SongSyncMessage(songs = current.toList(), fullSync = fullSync)
                val data = json.encodeToString(message).toByteArray()
                for (node in nodes) {
                    messageClient.sendMessage(node.id, WearPaths.SONG_SYNC, data).await()
                }
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

    override suspend fun requestSongSyncFromPhone(playlistId: PlaylistId?): Result<Unit> {
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

        // Register the listener FIRST so no event is missed.
        capabilityClient.addListener(listener, WearPaths.CAPABILITY_WATCH_APP)

        // Fetch the initial value from a CHILD coroutine: if the collector is
        // cancelled while this producer is parked on getCapability().await(),
        // awaitClose would never run and the listener above would leak on
        // every screen visit. Suspending in awaitClose (below) instead means
        // cancellation always runs the cleanup.
        launch {
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
        }

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

            // Use the cached version - reading DataStore here would require
            // runBlocking on a binder callback thread (jank / possible ANR).
            val appVersion = cachedWatchVersion

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

        // Initial state fetched from a child coroutine so that cancellation
        // during the fetch still runs the awaitClose cleanup (see
        // observeWatchConnection).
        launch {
            try {
                val initialStatus = getWatchAppStatus().getOrNull()
                if (initialStatus != null) {
                    trySend(initialStatus)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting initial watch app status", e)
            }
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
                messageClient.sendMessage(node.id, WearPaths.PLAYBACK_COMMAND, data).await()
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
            val version = syncDataStore.data.map { preferences ->
                preferences[watchVersionKey]
            }.first()
            cachedWatchVersion = version ?: cachedWatchVersion
            version
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get stored watch version", e)
            cachedWatchVersion
        }
    }

    /**
     * Stores the watch app version in DataStore
     */
    suspend fun storeWatchVersion(version: String) {
        try {
            cachedWatchVersion = version
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

        /**
         * Serialized byte budget per sync message (~100KB MessageClient cap,
         * with a large safety margin).
         */
        private const val MAX_MESSAGE_BYTES = 64 * 1024

        /**
         * Song ids per continuation part when a single playlist's songIds
         * alone exceed [MAX_MESSAGE_BYTES]. MediaStore ids serialize to a
         * few dozen bytes each, so 1000 parts stay comfortably in budget.
         */
        private const val SONG_IDS_PER_SPLIT_PART = 1000
    }
}

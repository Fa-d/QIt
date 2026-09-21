package dev.sadakat.qit.wear.infrastructure.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sadakat.qit.shared.constants.WearPaths
import dev.sadakat.qit.shared.domain.entity.Playlist
import dev.sadakat.qit.shared.domain.entity.PlaylistId
import dev.sadakat.qit.shared.domain.entity.Song
import dev.sadakat.qit.shared.domain.entity.SongId
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.valueobject.AudioQuality
import dev.sadakat.qit.shared.domain.valueobject.ChangeRecord
import dev.sadakat.qit.shared.domain.valueobject.SyncMetadata
import dev.sadakat.qit.shared.domain.valueobject.WatchAppStatus
import dev.sadakat.qit.shared.dto.PlaybackCommandMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watch-side implementation of SyncRepository
 * Handles communication with phone for sync operations
 */
@Singleton
class WearSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient,
    private val capabilityClient: CapabilityClient,
    private val dataClient: DataClient
) : SyncRepository {

    private var lastSyncTimestamp: Long = 0L

    private val json = Json { ignoreUnknownKeys = true }

    // In-memory storage for sync metadata (in production, this should be persisted)
    private var syncMetadata: SyncMetadata = SyncMetadata.initial()

    companion object {
        private const val TAG = "WearSyncRepository"
        private const val PHONE_CAPABILITY = "qit_phone_app"
    }

    /**
     * Watch doesn't push playlists to phone - not applicable
     */
    override suspend fun syncPlaylistToWatch(playlist: Playlist): Result<Unit> {
        Log.d(TAG, "syncPlaylistToWatch not applicable on watch")
        return Result.success(Unit)
    }

    /**
     * Watch doesn't push playlists to phone - not applicable
     */
    override suspend fun syncPlaylistsToWatch(
        playlists: List<Playlist>,
        fullSync: Boolean
    ): Result<Unit> {
        Log.d(TAG, "syncPlaylistsToWatch not applicable on watch")
        return Result.success(Unit)
    }

    /**
     * Watch doesn't push songs to phone - not applicable
     */
    override suspend fun syncSongToWatch(song: Song): Result<Unit> {
        Log.d(TAG, "syncSongToWatch not applicable on watch")
        return Result.success(Unit)
    }

    /**
     * Watch doesn't push songs to phone - not applicable
     */
    override suspend fun syncSongsToWatch(songs: List<Song>, fullSync: Boolean): Result<Unit> {
        Log.d(TAG, "syncSongsToWatch not applicable on watch")
        return Result.success(Unit)
    }

    /**
     * Request playlist sync from phone
     */
    override suspend fun requestPlaylistSyncFromPhone(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting playlist sync from phone")

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No phone connected")
                return@withContext Result.failure(Exception("No phone connected"))
            }

            val node = nodes.first()
            messageClient.sendMessage(
                node.id,
                WearPaths.REQUEST_PLAYLIST_SYNC,
                byteArrayOf()
            ).await()

            Log.d(TAG, "Playlist sync request sent to phone")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request playlist sync", e)
            Result.failure(e)
        }
    }

    /**
     * Request songs for a playlist from phone.
     * A null playlistId requests all songs.
     */
    override suspend fun requestSongSyncFromPhone(playlistId: PlaylistId?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting song sync from phone (playlistId=${playlistId?.value ?: "all"})")

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No phone connected")
                return@withContext Result.failure(Exception("No phone connected"))
            }

            val node = nodes.first()
            messageClient.sendMessage(
                node.id,
                WearPaths.REQUEST_SONG_SYNC,
                playlistId?.value?.toByteArray() ?: byteArrayOf()
            ).await()

            Log.d(TAG, "Song sync request sent to phone")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request song sync", e)
            Result.failure(e)
        }
    }

    /**
     * Check if phone is connected
     */
    override suspend fun isWatchConnected(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check phone connection", e)
            false
        }
    }

    /**
     * Observe phone connection status
     */
    override fun observeWatchConnection(): Flow<Boolean> = callbackFlow {
        val listener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
            val hasPhone = capabilityInfo.nodes.isNotEmpty()
            trySend(hasPhone)
        }

        // Register the listener FIRST so no event is missed.
        capabilityClient.addListener(listener, PHONE_CAPABILITY)

        // Check initial state from a CHILD coroutine: if the collector is
        // cancelled while the producer is parked on getCapability().await(),
        // awaitClose would never run and the listener would leak on every
        // screen visit. Suspending in awaitClose (below) instead guarantees
        // the cleanup runs on cancellation.
        launch {
            try {
                val capabilityInfo = capabilityClient.getCapability(
                    PHONE_CAPABILITY,
                    CapabilityClient.FILTER_REACHABLE
                ).await()
                trySend(capabilityInfo.nodes.isNotEmpty())
            } catch (e: Exception) {
                trySend(false)
            }
        }

        awaitClose {
            capabilityClient.removeListener(listener, PHONE_CAPABILITY)
        }
    }

    /**
     * Get watch app status - not applicable on watch side
     */
    override suspend fun getWatchAppStatus(): Result<WatchAppStatus> {
        Log.d(TAG, "getWatchAppStatus not applicable on watch")
        return Result.success(WatchAppStatus.notInstalled())
    }

    /**
     * Observe watch app status - not applicable on watch side
     */
    override fun observeWatchAppStatus(): Flow<WatchAppStatus> = flow {
        Log.d(TAG, "observeWatchAppStatus not applicable on watch")
        emit(WatchAppStatus.notInstalled())
    }

    /**
     * Send playback command to phone.
     *
     * The payload must be a JSON [PlaybackCommandMessage] - the phone's
     * WatchDataService parses it as JSON.
     */
    override suspend fun sendPlaybackCommand(
        command: String,
        songId: SongId?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending playback command: $command, songId: $songId")

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return@withContext Result.failure(Exception("No phone connected"))
            }

            val message = PlaybackCommandMessage(
                command = command,
                songId = songId?.value
            )
            val commandData = json.encodeToString(message).toByteArray()

            val node = nodes.first()
            messageClient.sendMessage(
                node.id,
                WearPaths.PLAYBACK_COMMAND,
                commandData
            ).await()

            Log.d(TAG, "Playback command sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send playback command", e)
            Result.failure(e)
        }
    }

    /**
     * Get last sync timestamp
     */
    override suspend fun getLastSyncTimestamp(): Long {
        return lastSyncTimestamp
    }

    /**
     * Update last sync timestamp
     */
    override suspend fun updateLastSyncTimestamp(timestamp: Long): Result<Unit> {
        lastSyncTimestamp = timestamp
        return Result.success(Unit)
    }

    // Delta Sync Implementation

    override fun getChangesSince(timestamp: Long): Flow<List<ChangeRecord>> = flow {
        Log.d(TAG, "Getting changes since timestamp: $timestamp from phone")

        // Request changes from phone via DataLayer
        try {
            val uri = android.net.Uri.parse("wear://*/sync_metadata")
            val dataItems = dataClient.getDataItems(uri).await()

            if (dataItems.count > 0) {
                val item = dataItems.get(0)
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                val phoneLastSync = dataMap.getLong("lastSyncTimestamp", 0L)
                val phoneSyncVersion = dataMap.getInt("syncVersion", 0)

                Log.d(TAG, "Phone sync metadata: lastSync=$phoneLastSync, version=$phoneSyncVersion")

                // Emit changes from local metadata that are newer than the given timestamp
                val changes = syncMetadata.changesNewerThan(timestamp)
                emit(changes)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get changes from phone", e)
            emit(emptyList())
        }
    }

    override suspend fun getSyncMetadata(): SyncMetadata = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting sync metadata from watch storage")

        // Try to fetch metadata from phone via DataLayer
        try {
            val uri = android.net.Uri.parse("wear://*/sync_metadata")
            val dataItems = dataClient.getDataItems(uri).await()

            if (dataItems.count > 0) {
                val item = dataItems.get(0)
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                val phoneLastSync = dataMap.getLong("lastSyncTimestamp", 0L)
                val phoneSyncVersion = dataMap.getInt("syncVersion", 0)

                // Update local metadata if phone has newer version
                if (phoneSyncVersion > syncMetadata.syncVersion) {
                    Log.d(TAG, "Phone has newer sync version, updating local metadata")
                    syncMetadata = syncMetadata.copy(
                        lastSyncTimestamp = phoneLastSync,
                        syncVersion = phoneSyncVersion
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch metadata from phone", e)
        }

        syncMetadata
    }

    override suspend fun updateSyncMetadata(metadata: SyncMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating sync metadata: version=${metadata.syncVersion}, pendingChanges=${metadata.pendingChangeCount()}")
            syncMetadata = metadata
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update sync metadata", e)
            Result.failure(e)
        }
    }

    override suspend fun markEntitiesAsSynced(entityIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Marking ${entityIds.size} entities as synced")
            syncMetadata = syncMetadata.removeChanges(entityIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark entities as synced", e)
            Result.failure(e)
        }
    }

    /**
     * Requests delta sync from phone - only changes since last sync
     */
    suspend fun requestDeltaSyncFromPhone(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting delta sync from phone (since timestamp: $lastSyncTimestamp)")

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No phone connected")
                return@withContext Result.failure(Exception("No phone connected"))
            }

            val node = nodes.first()

            // Send last sync timestamp to phone so it knows what to send
            val timestampData = lastSyncTimestamp.toString().toByteArray()
            messageClient.sendMessage(
                node.id,
                WearPaths.REQUEST_DELTA_SYNC,
                timestampData
            ).await()

            Log.d(TAG, "Delta sync request sent to phone")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request delta sync", e)
            Result.failure(e)
        }
    }

    /**
     * Requests full sync from phone (all data)
     */
    suspend fun requestFullSyncFromPhone(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting full sync from phone")

            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No phone connected")
                return@withContext Result.failure(Exception("No phone connected"))
            }

            val node = nodes.first()
            messageClient.sendMessage(
                node.id,
                WearPaths.REQUEST_FULL_SYNC,
                byteArrayOf()
            ).await()

            Log.d(TAG, "Full sync request sent to phone")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request full sync", e)
            Result.failure(e)
        }
    }
}

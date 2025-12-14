# QIt Domain Services

## Overview

Domain services contain business logic that doesn't naturally fit within a single entity. They orchestrate complex operations across multiple entities and repositories.

## SyncCoordinator

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/SyncCoordinator.kt`

Handles phone-to-watch synchronization logic.

```kotlin
class SyncCoordinator(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    private val syncRepository: SyncRepository
) {
    /**
     * Full sync - send all playlists and songs to watch
     */
    suspend fun syncAllToWatch(): Result<SyncResult> {
        val playlists = playlistRepository.getAllPlaylists().first()
        val allSongIds = playlists.flatMap { it.getSongIds() }.distinct()
        val songs = musicRepository.getSongsByIds(allSongIds).getOrElse { emptyList() }

        return try {
            syncRepository.syncPlaylistsToWatch(playlists)
            syncRepository.syncSongsToWatch(songs)
            syncRepository.updateLastSyncTimestamp(System.currentTimeMillis())

            Result.success(SyncResult(
                playlistCount = playlists.size,
                songCount = songs.size,
                timestamp = System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delta sync - only sync changes since last sync
     */
    suspend fun performDeltaSync(): Result<SyncResult> {
        val lastSync = syncRepository.getLastSyncTimestamp()
        val changes = syncRepository.getChangesSince(lastSync).first()

        // Filter to modified playlists/songs
        val modifiedPlaylistIds = changes
            .filter { it.entityType == "playlist" }
            .map { PlaylistId.from(it.entityId) }

        val modifiedSongIds = changes
            .filter { it.entityType == "song" }
            .map { SongId.from(it.entityId) }

        // Sync only modified entities
        // ...

        return Result.success(SyncResult(...))
    }

    /**
     * Detect conflicts between phone and watch versions
     */
    suspend fun detectConflicts(
        phonePlaylists: List<Playlist>,
        watchPlaylists: List<Playlist>
    ): List<SyncConflict> {
        val conflicts = mutableListOf<SyncConflict>()

        phonePlaylists.forEach { phonePlaylist ->
            val watchPlaylist = watchPlaylists.find { it.id == phonePlaylist.id }
            if (watchPlaylist != null && watchPlaylist.updatedAt != phonePlaylist.updatedAt) {
                conflicts.add(SyncConflict(
                    entityId = phonePlaylist.id.value,
                    entityType = "playlist",
                    phoneVersion = phonePlaylist,
                    watchVersion = watchPlaylist
                ))
            }
        }

        return conflicts
    }
}

data class SyncResult(
    val playlistCount: Int,
    val songCount: Int,
    val timestamp: Long
)

data class SyncConflict(
    val entityId: String,
    val entityType: String,
    val phoneVersion: Any,
    val watchVersion: Any
)
```

## StreamingCoordinator

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/StreamingCoordinator.kt`

Determines optimal streaming strategy and manages playback.

```kotlin
class StreamingCoordinator(
    private val musicRepository: MusicRepository,
    private val streamingRepository: StreamingRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Determine best streaming strategy for a song
     */
    suspend fun determineStreamingStrategy(song: Song): StreamingStrategy {
        // Priority 1: Local playback if downloaded
        if (song.isAvailableOnWatch()) {
            val path = (song.downloadStatus as DownloadStatus.Downloaded).localPath
            return StreamingStrategy.Local(path)
        }

        // Priority 2: Check phone connection
        if (!syncRepository.isWatchConnected()) {
            return StreamingStrategy.Unavailable("Phone not connected")
        }

        // Priority 3: Get user preference
        val mode = settingsRepository.observeStreamingMode().first()
        val quality = streamingRepository.getRecommendedQuality()

        return when (mode) {
            StreamingMode.LOCAL_ONLY -> StreamingStrategy.Unavailable("Local only mode enabled")
            StreamingMode.STREAM_ONLY -> StreamingStrategy.RealTime(quality)
            StreamingMode.PREFER_LOCAL -> StreamingStrategy.RealTime(quality)
            StreamingMode.ADAPTIVE -> StreamingStrategy.Progressive(quality)
        }
    }

    /**
     * Initiate streaming for a song
     */
    suspend fun initiateStreaming(
        songId: SongId,
        strategy: StreamingStrategy
    ): Result<Unit> {
        return when (strategy) {
            is StreamingStrategy.Local -> Result.success(Unit) // No streaming needed
            is StreamingStrategy.RealTime -> {
                streamingRepository.requestStreamFromPhone(songId, strategy.quality)
            }
            is StreamingStrategy.Progressive -> {
                streamingRepository.requestStreamFromPhone(songId, strategy.quality)
            }
            is StreamingStrategy.Unavailable -> {
                Result.failure(Exception(strategy.reason))
            }
        }
    }

    /**
     * Handle connection loss during playback
     */
    suspend fun handleConnectionLoss(
        songId: SongId,
        currentStrategy: StreamingStrategy
    ): Result<StreamingStrategy> {
        val song = musicRepository.getSongById(songId).getOrNull()
            ?: return Result.failure(Exception("Song not found"))

        // If song is downloaded, switch to local
        if (song.isAvailableOnWatch()) {
            val path = (song.downloadStatus as DownloadStatus.Downloaded).localPath
            return Result.success(StreamingStrategy.Local(path))
        }

        // Otherwise, unavailable
        return Result.success(StreamingStrategy.Unavailable("Connection lost"))
    }

    /**
     * Stop streaming for a song
     */
    suspend fun stopStreaming(songId: SongId): Result<Unit> {
        return streamingRepository.stopStreaming(songId)
    }
}

sealed class StreamingStrategy {
    data class Local(val path: String) : StreamingStrategy()
    data class RealTime(val quality: AudioQuality) : StreamingStrategy()
    data class Progressive(val quality: AudioQuality) : StreamingStrategy()
    data class Unavailable(val reason: String) : StreamingStrategy()
}
```

## PlaylistOrchestrator

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/PlaylistOrchestrator.kt`

Orchestrates playlist operations with validation.

```kotlin
class PlaylistOrchestrator(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository
) {
    /**
     * Create a new playlist with validation
     */
    suspend fun createPlaylist(
        name: String,
        description: String? = null,
        songIds: List<SongId> = emptyList()
    ): Result<Playlist> {
        // Validate name
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        }

        // Create playlist
        val playlist = Playlist.create(name, description)
            .let { if (songIds.isNotEmpty()) it.addSongs(songIds) else it }

        // Save
        return playlistRepository.savePlaylist(playlist).map { playlist }
    }

    /**
     * Add songs to playlist with capacity check
     */
    suspend fun addSongsToPlaylist(
        playlistId: PlaylistId,
        songIds: List<SongId>
    ): Result<Playlist> {
        val playlist = playlistRepository.getPlaylistById(playlistId).getOrNull()
            ?: return Result.failure(Exception("Playlist not found"))

        // Check capacity
        if (!playlist.canAddMoreSongs()) {
            return Result.failure(Exception("Playlist is full (max 1000 songs)"))
        }

        // Verify songs exist
        val existingSongs = musicRepository.getSongsByIds(songIds).getOrElse { emptyList() }
        val validSongIds = existingSongs.map { it.id }

        // Add songs
        val updatedPlaylist = playlist.addSongs(validSongIds)

        // Save
        return playlistRepository.savePlaylist(updatedPlaylist).map { updatedPlaylist }
    }

    /**
     * Remove songs from playlist
     */
    suspend fun removeSongsFromPlaylist(
        playlistId: PlaylistId,
        songIds: List<SongId>
    ): Result<Playlist> {
        val playlist = playlistRepository.getPlaylistById(playlistId).getOrNull()
            ?: return Result.failure(Exception("Playlist not found"))

        var updatedPlaylist = playlist
        songIds.forEach { songId ->
            updatedPlaylist = updatedPlaylist.removeSong(songId)
        }

        return playlistRepository.savePlaylist(updatedPlaylist).map { updatedPlaylist }
    }
}
```

## ConflictResolver

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/ConflictResolver.kt`

Resolves sync conflicts between phone and watch.

```kotlin
class ConflictResolver {

    enum class Strategy {
        LAST_WRITE_WINS,  // Newer timestamp wins
        PHONE_WINS,       // Always use phone version
        WATCH_WINS,       // Always use watch version
        MANUAL            // Require user decision
    }

    /**
     * Resolve a single conflict
     */
    fun resolveConflict(
        conflict: SyncConflict,
        strategy: Strategy
    ): Any {
        return when (strategy) {
            Strategy.LAST_WRITE_WINS -> {
                val phoneTimestamp = getTimestamp(conflict.phoneVersion)
                val watchTimestamp = getTimestamp(conflict.watchVersion)
                if (phoneTimestamp >= watchTimestamp) conflict.phoneVersion
                else conflict.watchVersion
            }
            Strategy.PHONE_WINS -> conflict.phoneVersion
            Strategy.WATCH_WINS -> conflict.watchVersion
            Strategy.MANUAL -> conflict // Return conflict for user decision
        }
    }

    /**
     * Resolve multiple conflicts
     */
    fun resolveConflicts(
        conflicts: List<SyncConflict>,
        strategy: Strategy
    ): ConflictResolutionResult {
        val resolved = conflicts.map { conflict ->
            ResolvedConflict(
                entityId = conflict.entityId,
                entityType = conflict.entityType,
                resolvedVersion = resolveConflict(conflict, strategy),
                strategy = strategy
            )
        }
        return ConflictResolutionResult(resolved)
    }

    private fun getTimestamp(entity: Any): Long {
        return when (entity) {
            is Playlist -> entity.updatedAt
            is Song -> entity.dateAdded
            else -> 0L
        }
    }
}

data class ResolvedConflict(
    val entityId: String,
    val entityType: String,
    val resolvedVersion: Any,
    val strategy: ConflictResolver.Strategy
)

data class ConflictResolutionResult(
    val resolvedConflicts: List<ResolvedConflict>
)
```

## DomainEventPublisher

**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/event/DomainEventPublisher.kt`

Publishes domain events for cross-layer communication.

```kotlin
class DomainEventPublisher {
    private val _events = MutableSharedFlow<DomainEvent>()
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    suspend fun publish(event: DomainEvent) {
        _events.emit(event)
    }

    fun subscribe(): Flow<DomainEvent> = events
}

sealed class DomainEvent {
    data class SyncStarted(val timestamp: Long) : DomainEvent()
    data class SyncCompleted(val result: SyncResult) : DomainEvent()
    data class SyncFailed(val error: String) : DomainEvent()
    data class DownloadStarted(val songId: SongId) : DomainEvent()
    data class DownloadProgress(val songId: SongId, val progress: Float) : DomainEvent()
    data class DownloadCompleted(val songId: SongId, val path: String) : DomainEvent()
    data class DownloadFailed(val songId: SongId, val error: String) : DomainEvent()
    data class PlaybackStarted(val songId: SongId) : DomainEvent()
    data class PlaybackPaused(val songId: SongId) : DomainEvent()
    data class PlaybackStopped(val songId: SongId) : DomainEvent()
}
```

## Usage in Use Cases

```kotlin
class SyncAllToWatchUseCase @Inject constructor(
    private val syncCoordinator: SyncCoordinator,
    private val eventPublisher: DomainEventPublisher
) : BaseUseCase<Result<SyncResult>>() {

    override suspend fun invoke(): Result<SyncResult> {
        eventPublisher.publish(DomainEvent.SyncStarted(System.currentTimeMillis()))

        val result = syncCoordinator.syncAllToWatch()

        result.fold(
            onSuccess = { syncResult ->
                eventPublisher.publish(DomainEvent.SyncCompleted(syncResult))
            },
            onFailure = { error ->
                eventPublisher.publish(DomainEvent.SyncFailed(error.message ?: "Unknown error"))
            }
        )

        return result
    }
}
```

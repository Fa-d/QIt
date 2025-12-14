# QIt Repository Patterns

## Repository Architecture

```
Domain Layer (Interfaces)
    ↑ implemented by
Infrastructure Layer (Implementations)
```

All repositories are defined in domain layer as interfaces, implementations in infrastructure.

## Core Repository Interfaces

### MusicRepository
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/MusicRepository.kt`

```kotlin
interface MusicRepository {
    // Scanning
    suspend fun scanMusicLibrary(): Result<List<Song>>

    // Single song operations
    suspend fun getSongById(id: SongId): Result<Song?>
    suspend fun saveSong(song: Song): Result<Unit>
    suspend fun deleteSong(id: SongId): Result<Unit>

    // Batch operations
    suspend fun getSongsByIds(ids: List<SongId>): Result<List<Song>>
    suspend fun saveSongs(songs: List<Song>): Result<Unit>

    // Queries (Flow for reactive updates)
    fun getAllSongs(): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    fun getSongsByArtist(artist: String): Flow<List<Song>>
    fun getSongsByAlbum(album: String): Flow<List<Song>>
    fun observeSong(id: SongId): Flow<Song?>

    // Metadata queries
    fun getAllArtists(): Flow<List<String>>
    fun getAllAlbums(): Flow<List<String>>

    // Download status
    suspend fun updateDownloadStatus(id: SongId, downloadPath: String?): Result<Unit>
    fun getDownloadedSongs(): Flow<List<Song>>
}
```

### PlaylistRepository
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/PlaylistRepository.kt`

```kotlin
interface PlaylistRepository {
    // CRUD
    suspend fun getPlaylistById(id: PlaylistId): Result<Playlist?>
    suspend fun savePlaylist(playlist: Playlist): Result<Unit>
    suspend fun deletePlaylist(id: PlaylistId): Result<Unit>

    // Queries
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun observePlaylist(id: PlaylistId): Flow<Playlist?>
    fun searchPlaylists(query: String): Flow<List<Playlist>>

    // Song relationships
    suspend fun getSongsForPlaylist(playlistId: PlaylistId): Result<List<Song>>
    fun observeSongsForPlaylist(playlistId: PlaylistId): Flow<List<Song>>
    suspend fun getPlaylistsContainingSong(songId: SongId): Result<List<Playlist>>
}
```

### SyncRepository
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/SyncRepository.kt`

```kotlin
interface SyncRepository {
    // Sync operations
    suspend fun syncPlaylistToWatch(playlist: Playlist): Result<Unit>
    suspend fun syncPlaylistsToWatch(playlists: List<Playlist>): Result<Unit>
    suspend fun syncSongToWatch(song: Song): Result<Unit>
    suspend fun syncSongsToWatch(songs: List<Song>): Result<Unit>
    suspend fun requestPlaylistSyncFromPhone(): Result<Unit>

    // Connection
    suspend fun isWatchConnected(): Boolean
    fun observeWatchConnection(): Flow<Boolean>

    // Playback commands
    suspend fun sendPlaybackCommand(command: String, songId: SongId?): Result<Unit>

    // Sync metadata
    suspend fun getLastSyncTimestamp(): Long
    suspend fun updateLastSyncTimestamp(timestamp: Long): Result<Unit>

    // Delta sync
    fun getChangesSince(timestamp: Long): Flow<List<ChangeRecord>>
    suspend fun getSyncMetadata(): SyncMetadata
    suspend fun updateSyncMetadata(metadata: SyncMetadata): Result<Unit>
    suspend fun markEntitiesAsSynced(entityIds: List<String>): Result<Unit>
}
```

### StreamingRepository
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/StreamingRepository.kt`

```kotlin
interface StreamingRepository {
    suspend fun requestStreamFromPhone(songId: SongId, quality: AudioQuality): Result<Unit>
    suspend fun stopStreaming(songId: SongId): Result<Unit>
    fun observeStreamingStatus(): Flow<StreamingStatus>
    suspend fun getRecommendedQuality(): AudioQuality
}

sealed class StreamingStatus {
    object Idle : StreamingStatus()
    data class Buffering(val progress: Float) : StreamingStatus()
    data class Streaming(val songId: SongId) : StreamingStatus()
    data class Error(val message: String) : StreamingStatus()
}
```

### DownloadRepository
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/DownloadRepository.kt`

```kotlin
interface DownloadRepository {
    suspend fun downloadSong(songId: SongId, quality: AudioQuality): Result<Unit>
    suspend fun cancelDownload(songId: SongId): Result<Unit>
    suspend fun pauseDownload(songId: SongId): Result<Unit>
    suspend fun resumeDownload(songId: SongId): Result<Unit>
    fun observeDownloadProgress(songId: SongId): Flow<Float>
    fun observeAllDownloads(): Flow<List<DownloadInfo>>
    suspend fun getDownloadedSongPath(songId: SongId): String?
    suspend fun deleteDownloadedSong(songId: SongId): Result<Unit>
}
```

### SettingsRepository
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/SettingsRepository.kt`

```kotlin
interface SettingsRepository {
    fun observeStreamingMode(): Flow<StreamingMode>
    suspend fun setStreamingMode(mode: StreamingMode): Result<Unit>
    fun observeDownloadQuality(): Flow<AudioQuality>
    suspend fun setDownloadQuality(quality: AudioQuality): Result<Unit>
    fun observeAutoSyncEnabled(): Flow<Boolean>
    suspend fun setAutoSyncEnabled(enabled: Boolean): Result<Unit>
}
```

## Phone App Implementations

### RoomMusicRepository
**Location:** `/app/src/main/java/dev/sadakat/qit/infrastructure/persistence/repository/RoomMusicRepository.kt`

- Scans MediaStore for audio files
- Persists to Room database via `SongDao`
- Uses `SongMapper` for domain ↔ entity conversion
- Combines MediaStore data with Room storage

### RoomPlaylistRepository
**Location:** `/app/src/main/java/dev/sadakat/qit/infrastructure/persistence/repository/RoomPlaylistRepository.kt`

- CRUD operations via `PlaylistDao`
- Manages `PlaylistSongCrossRef` junction table
- Uses `PlaylistMapper` for conversion

### WearableSyncRepository
**Location:** `/app/src/main/java/dev/sadakat/qit/infrastructure/wearable/WearableSyncRepository.kt`

- Uses `DataClient` for persistent data sync
- Uses `MessageClient` for fire-and-forget messages
- Uses `ChannelClient` for audio streaming
- Serializes via `DtoMapper` + kotlinx.serialization

## Wear App Implementations

### WearMusicRepository
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/repository/WearMusicRepository.kt`

- Adapts to watch's local storage
- No MediaStore (watch doesn't support)
- In-memory filtering for searches

### WearStreamingRepository
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/WearStreamingRepository.kt`

- Receives audio streams via Channel API
- Buffers with `StreamingAudioBuffer`
- Reports status via StateFlow

### WearDownloadRepository
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/download/`

- Downloads songs from phone via messaging
- Manages local storage
- Uses `DownloadWorker` for background work

## Entity Mappers

### SongMapper
```kotlin
object SongMapper {
    fun SongEntity.toDomain(): Song
    fun Song.toEntity(): SongEntity
}
```

### PlaylistMapper
```kotlin
object PlaylistMapper {
    fun PlaylistEntity.toDomain(songIds: List<SongId>): Playlist
    fun Playlist.toEntity(): PlaylistEntity
}
```

## Room Database

### Phone Database
**Location:** `/app/src/main/java/dev/sadakat/qit/data/local/MusicDatabase.kt`

```kotlin
@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class],
    version = 1
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
}
```

### Wear Database
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/data/local/WearMusicDatabase.kt`

Similar structure optimized for watch storage.

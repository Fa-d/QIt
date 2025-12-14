# QIt Domain Entities and Value Objects

## Domain Entities

### Song Entity
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/entity/Song.kt`

```kotlin
data class Song(
    val id: SongId,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Duration,
    val filePath: String?,
    val uri: String?,
    val coverArtUri: String?,
    val fileSize: FileSize,
    val mimeType: String?,
    val bitrate: Int,
    val dateAdded: Long,
    val downloadStatus: DownloadStatus
)
```

**Key Methods:**
- `artistName()`, `albumName()` - Display-friendly with defaults
- `isAvailableOnWatch()` - Check if downloaded
- `estimatedSizeForQuality(quality)` - Calculate size for quality level
- `needsTranscoding(targetBitrate)` - Determine if re-encoding needed
- `markAsDownloaded(path)`, `markAsDownloading(progress)` - Update status immutably

**Factory:**
```kotlin
companion object {
    fun create(title: String, artist: String?, ...) = Song(
        id = SongId.generate(),
        dateAdded = System.currentTimeMillis(),
        downloadStatus = DownloadStatus.NotDownloaded,
        ...
    )
}
```

### Playlist Entity (Aggregate Root)
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/entity/Playlist.kt`

```kotlin
data class Playlist(
    val id: PlaylistId,
    val name: String,
    val description: String?,
    private val songIds: List<SongId>,  // Encapsulated
    val createdAt: Long,
    val updatedAt: Long,
    val coverArtUri: String?
)
```

**Key Methods:**
- `getSongIds()` - Returns immutable copy
- `addSong(songId)`, `removeSong(songId)` - Return new Playlist (immutable)
- `addSongs(ids)` - Batch add with deduplication
- `moveSong(from, to)` - Reorder songs
- `canAddMoreSongs()` - Capacity check (max 1000 songs)
- `updateMetadata(name, description, coverArtUri)` - Update with new timestamp

**Factory:**
```kotlin
companion object {
    fun create(name: String, description: String? = null) = Playlist(
        id = PlaylistId.generate(),
        name = name,
        songIds = emptyList(),
        createdAt = System.currentTimeMillis(),
        ...
    )
}
```

## Value Objects

### Type-Safe Identifiers
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/entity/`

```kotlin
data class SongId(val value: String) {
    companion object {
        fun generate() = SongId(UUID.randomUUID().toString())
        fun from(value: String) = SongId(value)
    }
}

data class PlaylistId(val value: String) {
    companion object {
        fun generate() = PlaylistId(UUID.randomUUID().toString())
        fun from(value: String) = PlaylistId(value)
    }
}
```

### Duration
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/Duration.kt`

```kotlin
data class Duration(val milliseconds: Long) {
    val seconds: Long get() = milliseconds / 1000
    val minutes: Long get() = seconds / 60
    val hours: Long get() = minutes / 60

    fun format(): String  // Returns "MM:SS" or "HH:MM:SS"
    operator fun plus(other: Duration): Duration

    companion object {
        fun fromMilliseconds(ms: Long) = Duration(ms)
        fun fromSeconds(s: Long) = Duration(s * 1000)
        fun fromMinutes(m: Long) = Duration(m * 60 * 1000)
    }
}
```

### FileSize
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/FileSize.kt`

```kotlin
data class FileSize(val bytes: Long) {
    val kilobytes: Double get() = bytes / 1024.0
    val megabytes: Double get() = kilobytes / 1024.0
    val gigabytes: Double get() = megabytes / 1024.0

    fun format(): String  // Returns "X.XX MB" etc.
    operator fun plus(other: FileSize): FileSize

    companion object {
        fun fromBytes(b: Long) = FileSize(b)
        fun fromKilobytes(kb: Long) = FileSize(kb * 1024)
        fun fromMegabytes(mb: Long) = FileSize(mb * 1024 * 1024)
    }
}
```

### AudioQuality
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/AudioQuality.kt`

```kotlin
enum class AudioQuality(val bitrate: Int, val displayName: String) {
    LOW(64, "Low (64 kbps)"),
    MEDIUM(128, "Medium (128 kbps)"),
    HIGH(256, "High (256 kbps)"),
    ORIGINAL(0, "Original Quality");

    fun estimateFileSize(duration: Duration): FileSize
    fun isTranscodingRequired(sourceBitrate: Int): Boolean
}
```

### DownloadStatus
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/DownloadStatus.kt`

```kotlin
sealed class DownloadStatus {
    object NotDownloaded : DownloadStatus()
    data class Downloading(val progress: Float) : DownloadStatus()
    data class Downloaded(val localPath: String) : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
    data class Paused(val progress: Float) : DownloadStatus()

    fun isDownloaded(): Boolean
    fun isInProgress(): Boolean
    fun canResume(): Boolean
}
```

### StreamingMode
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/StreamingMode.kt`

```kotlin
enum class StreamingMode {
    LOCAL_ONLY,      // Only play downloaded songs
    STREAM_ONLY,     // Always stream from phone
    PREFER_LOCAL,    // Local first, stream if not available
    ADAPTIVE         // Choose based on connection quality
}
```

## DTOs (Data Transfer Objects)
**Location:** `/shared/src/main/java/dev/sadakat/qit/shared/dto/`

```kotlin
@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val filePath: String?,
    val uri: String?,
    val coverArtUri: String?,
    val fileSizeBytes: Long,
    val mimeType: String?,
    val bitrate: Int,
    val dateAdded: Long
)

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val description: String?,
    val songIds: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val coverArtUri: String?
)
```

**DtoMapper:** Converts Domain ↔ DTO
```kotlin
object DtoMapper {
    fun Song.toDto(): SongDto
    fun Playlist.toDto(): PlaylistDto
    fun SongDto.toDomain(): Song
    fun PlaylistDto.toDomain(): Playlist
}
```

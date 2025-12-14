# WearOS Download Infrastructure

This package contains the infrastructure for downloading and managing music files on the WearOS watch.

## Components

### 1. WearDownloadRepository

**Location**: `WearDownloadRepository.kt`

The main repository implementation that handles receiving downloaded files from the phone via the Wearable Channel API.

**Key Features**:
- Downloads individual songs from phone to watch
- Batch downloads for entire playlists
- Progress tracking via StateFlow
- Download cancellation, pause, and resume
- Storage space checking before downloads
- Automatic cleanup when storage is low
- Channel-based file transfer using `ChannelClient`

**Usage**:
```kotlin
@Inject
lateinit var downloadRepository: DownloadRepository

// Download a song
suspend fun downloadSong(songId: SongId) {
    val result = downloadRepository.downloadSong(
        songId = songId,
        quality = AudioQuality.MEDIUM
    )

    if (result.isSuccess) {
        // Download initiated successfully
    }
}

// Observe download progress
downloadRepository.observeDownloadProgress(songId)
    .collect { progress ->
        // Update UI with progress (0.0 to 1.0)
        println("Download progress: ${(progress * 100).toInt()}%")
    }

// Cancel download
downloadRepository.cancelDownload(songId)

// Download playlist
downloadRepository.downloadPlaylist(playlistId, AudioQuality.MEDIUM)
```

**Implementation Details**:
- Uses `ChannelClient.ChannelCallback` to listen for incoming download channels
- Channels are identified by path: `/download/{songId}`
- Files are saved to `{filesDir}/downloads/{songId}.mp3`
- Progress updates are throttled to every 100KB to avoid excessive updates
- Automatically removes from active downloads on completion or error

### 2. DownloadWorker

**Location**: `DownloadWorker.kt`

A WorkManager worker for handling background downloads with notifications and retry logic.

**Key Features**:
- Background downloads even when app is closed
- Foreground service notification with progress
- Automatic retry on failure (up to 3 attempts)
- Configurable constraints (WiFi, charging, battery)
- Progress notifications (updated every 10%)
- Error handling and cleanup

**Usage**:
```kotlin
// Enqueue a download work
DownloadWorker.enqueueDownload(
    context = context,
    songId = songId,
    quality = AudioQuality.MEDIUM
)

// With constraints
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
    .setRequiresCharging(true)
    .setRequiresBatteryNotLow(true)
    .build()

DownloadWorker.enqueueDownload(
    context = context,
    songId = songId,
    quality = AudioQuality.HIGH,
    constraints = constraints
)

// Cancel a download
DownloadWorker.cancelDownload(context, songId)

// Observe work status
DownloadWorker.observeWorkInfo(context, songId)
    .collect { workInfos ->
        workInfos.forEach { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getInt(KEY_PROGRESS, 0)
                    println("Work progress: $progress%")
                }
                WorkInfo.State.SUCCEEDED -> println("Download completed")
                WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString(KEY_ERROR)
                    println("Download failed: $error")
                }
                else -> {}
            }
        }
    }
```

**Configuration**:
- Unique work name: `download_{songId}`
- Retry policy: Exponential backoff
- Max retry attempts: 3
- Notification channel: "download_channel"

### 3. StorageManager

**Location**: `../storage/StorageManager.kt`

Manages storage operations, LRU eviction, and cleanup strategies.

**Key Features**:
- Check available storage space
- Calculate total downloaded size
- LRU (Least Recently Used) eviction
- Cleanup by age
- Storage quota enforcement
- Orphaned file cleanup
- Storage statistics

**Usage**:
```kotlin
@Inject
lateinit var storageManager: StorageManager

// Check available storage
val available = storageManager.getAvailableStorage()
println("Available: ${available.format()}")

// Get total downloaded size
val downloaded = storageManager.getTotalDownloadedSize()
println("Downloaded: ${downloaded.format()}")

// Cleanup old downloads to free 50MB
val deletedCount = storageManager.cleanupOldDownloads(
    FileSize.fromMegabytes(50)
)
println("Deleted $deletedCount songs")

// Cleanup songs older than 30 days
val oldCount = storageManager.cleanupByAge(maxAgeDays = 30)

// Enforce 100MB quota
val quotaCount = storageManager.enforceStorageQuota(
    FileSize.fromMegabytes(100)
)

// Get storage statistics
val stats = storageManager.getStorageStats()
println(stats.format())

// Get list of downloaded songs
val downloadedSongs = storageManager.getDownloadedSongs()
downloadedSongs.forEach { songInfo ->
    println("${songInfo.songId}: ${songInfo.fileSize.format()}")
}

// Delete a song
storageManager.deleteSong(songId)

// Cleanup orphaned files (files without database entries)
val orphanedCount = storageManager.cleanupOrphanedFiles()
```

**Cleanup Strategies**:

1. **LRU Eviction**: Deletes oldest accessed files first
2. **Age-based**: Removes songs older than specified days
3. **Quota-based**: Ensures total size doesn't exceed limit
4. **Orphaned files**: Removes files without database entries

## Architecture

### Download Flow

```
1. User initiates download
   ↓
2. WearDownloadRepository.downloadSong()
   ↓
3. Check storage availability
   ↓
4. Send download request to phone via MessageClient
   ↓
5. Phone opens Channel and streams file
   ↓
6. ChannelCallback receives channel
   ↓
7. handleIncomingDownload() reads stream
   ↓
8. Save file to local storage
   ↓
9. Update progress via StateFlow
   ↓
10. Update database on completion
```

### Background Download Flow (with Worker)

```
1. User initiates background download
   ↓
2. Enqueue DownloadWorker
   ↓
3. Worker starts in background
   ↓
4. Show foreground notification
   ↓
5. Call downloadRepository.downloadSong()
   ↓
6. Monitor progress and update notification
   ↓
7. Handle completion/error
   ↓
8. Show final notification
```

### Storage Management Flow

```
Before Download:
1. Check available storage
2. If insufficient, run cleanup
3. If still insufficient, fail with error

During Download:
1. Stream file to disk
2. Update progress

After Download:
1. Update database
2. Remove from active downloads

Periodic Cleanup:
1. Check storage quota
2. Run LRU eviction if needed
3. Cleanup old files by age
4. Remove orphaned files
```

## Configuration

### Minimum Storage Requirements
- Minimum free space: 10 MB (before download)
- Additional free space: 50 MB (reserved)

### Estimated File Sizes
- Average song: 5 MB (for progress estimation)
- Low quality (64 kbps): ~2-3 MB
- Medium quality (128 kbps): ~4-5 MB
- High quality (256 kbps): ~8-10 MB

### Directories
- Download directory: `{filesDir}/downloads/`
- File naming: `{songId}.mp3`

## Dependencies

### Required Dependencies (wear/build.gradle.kts)
```kotlin
// WorkManager
implementation(libs.work.runtime.ktx)

// Hilt WorkManager
implementation(libs.hilt.work)
ksp(libs.hilt.compiler)

// Wearable API
implementation(libs.play.services.wearable)

// Coroutines
implementation(libs.kotlinx.coroutines.android)
implementation(libs.kotlinx.coroutines.play.services)
```

### Required Permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### WorkManager Configuration (WearApplication.kt)
```kotlin
@HiltAndroidApp
class WearApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

## Dependency Injection

### InfrastructureModule
```kotlin
@Binds
@Singleton
abstract fun bindDownloadRepository(
    impl: WearDownloadRepository
): DownloadRepository
```

## Error Handling

### Common Errors
1. **Insufficient Storage**: Automatically attempts cleanup, then fails if still insufficient
2. **No Phone Connected**: Fails immediately with clear error message
3. **Channel Closed Unexpectedly**: Cleans up partial downloads and removes from active list
4. **Download Cancelled**: Deletes partial file and updates database
5. **File I/O Error**: Logs error, cleans up, and notifies user

### Retry Strategy
- DownloadWorker retries up to 3 times with exponential backoff
- Manual retry available via `resumeDownload()`

## Testing

### Integration Testing
```kotlin
@Test
fun testDownloadFlow() = runTest {
    // Given
    val songId = SongId.generate()

    // When
    val result = downloadRepository.downloadSong(songId)

    // Then
    assertTrue(result.isSuccess)

    // Verify progress updates
    downloadRepository.observeDownloadProgress(songId)
        .first { it >= 1.0f }
}

@Test
fun testStorageCleanup() = runTest {
    // Given: Multiple downloaded songs
    // When: Run LRU cleanup
    val deleted = storageManager.cleanupOldDownloads(FileSize.fromMegabytes(50))

    // Then: Oldest songs should be deleted
    assertTrue(deleted > 0)
}
```

## Best Practices

1. **Always check storage before downloads**
   ```kotlin
   val available = downloadRepository.getAvailableStorage()
   if (available.bytes < estimatedSize.bytes) {
       // Handle insufficient storage
   }
   ```

2. **Use WorkManager for background downloads**
   ```kotlin
   // Prefer this for user-initiated downloads
   DownloadWorker.enqueueDownload(context, songId)
   ```

3. **Implement periodic cleanup**
   ```kotlin
   // Run daily or when app starts
   storageManager.cleanupByAge(30)
   storageManager.cleanupOrphanedFiles()
   ```

4. **Monitor download progress**
   ```kotlin
   downloadRepository.observeDownloadProgress(songId)
       .collect { progress ->
           updateUI(progress)
       }
   ```

5. **Handle errors gracefully**
   ```kotlin
   val result = downloadRepository.downloadSong(songId)
   result.onFailure { error ->
       showErrorMessage(error.message)
       // Offer retry option
   }
   ```

## Future Enhancements

1. **Resume Support**: Save partial downloads and resume from offset
2. **Parallel Downloads**: Download multiple songs simultaneously
3. **Smart Quality Selection**: Auto-select quality based on storage
4. **Download Scheduling**: Schedule downloads for specific times
5. **Download Analytics**: Track download success rates and errors
6. **Bandwidth Throttling**: Limit download speed to preserve battery
7. **Differential Updates**: Only download changed portions of files

## Troubleshooting

### Downloads Not Starting
- Check phone connection: `nodeClient.connectedNodes`
- Verify storage space: `getAvailableStorage()`
- Check for conflicting downloads: `getDownloadingQueue()`

### Downloads Failing
- Review logcat for errors: `adb logcat | grep WearDownloadRepo`
- Verify channel callback registration
- Check file permissions

### Storage Issues
- Run cleanup: `cleanupOldDownloads()`
- Check for orphaned files: `cleanupOrphanedFiles()`
- Review storage stats: `getStorageStats()`

### Worker Not Running
- Verify WorkManager configuration in Application class
- Check constraints are satisfied
- Review WorkInfo state and output data

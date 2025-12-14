# QIt Data Flow Patterns

## Phone-to-Watch Sync Flow

```
User Action: "Sync to Watch"
    ↓
ViewModel.syncMusicLibrary()
    ↓
SyncAllToWatchUseCase.invoke()
    ├─ Publishes SyncStarted event
    ├─ Calls SyncCoordinator.syncAllToWatch()
    │   ├─ Gets playlists from PlaylistRepository
    │   ├─ Gets songs from MusicRepository
    │   └─ Sends via WearableSyncRepository
    │       ├─ Maps Domain → DTO (DtoMapper)
    │       ├─ Serializes to JSON (kotlinx.serialization)
    │       └─ Sends via DataClient or MessageClient
    ├─ Publishes SyncCompleted event
    └─ Updates UI state via StateFlow

Watch Receives Data
    ↓
PhoneDataService (WearableListenerService)
    ├─ onDataChanged() or onMessageReceived()
    ├─ Deserializes JSON → DTO
    ├─ Maps DTO → Domain entities
    └─ Saves to WearMusicDatabase
```

## Audio Streaming Flow

```
User selects song on watch
    ↓
PlaySongUseCase.invoke(songId)
    ├─ StreamingCoordinator.determineStreamingStrategy()
    │   ├─ Check if downloaded locally → Local
    │   ├─ Check if phone connected → RealTime/Progressive
    │   └─ Neither available → Unavailable
    └─ If streaming needed:
        ├─ WearStreamingRepository.requestStreamFromPhone()
        │   └─ Send request via MessageClient
        ↓
Phone receives request (WatchDataService)
        ├─ Opens ChannelClient stream
        └─ Starts sending audio file bytes
        ↓
Watch receives stream
        ├─ WearStreamingRepository receives via Channel callback
        ├─ Writes to StreamingAudioBuffer (circular buffer)
        ├─ Updates StreamingStatus (Buffering → Streaming)
        └─ ExoPlayer reads from StreamingAudioSource
            └─ Plays audio
```

## Download Flow

```
User requests download on watch
    ↓
DownloadSongUseCase.invoke(songId, quality)
    ├─ Creates DownloadWorker work request
    ├─ Enqueues with WorkManager
    └─ Returns immediately
    ↓
DownloadWorker.doWork()
    ├─ Shows notification (progress)
    ├─ Requests file from phone via MessageClient
    ├─ Receives file chunks via Channel
    ├─ Writes to local storage
    ├─ Updates MusicRepository download status
    └─ Shows completion notification

Progress updates
    ├─ WorkManager progress data
    ├─ DownloadRepository.observeDownloadProgress()
    └─ ViewModel observes and updates UI
```

## UI State Flow (MVVM)

```
Repository (Flow<T>)
    ↓ collected by
Use Case
    ↓ returns Flow to
ViewModel (StateFlow<UiState>)
    ↓ collected by
Compose UI (@Composable)
    ↓ triggers user action
ViewModel.onAction()
    ↓ calls
Use Case.invoke()
    ↓ updates
Repository
```

**Example:**
```kotlin
// ViewModel
private val _uiState = MutableStateFlow(MusicLibraryUiState())
val uiState: StateFlow<MusicLibraryUiState> = _uiState.asStateFlow()

init {
    viewModelScope.launch {
        getAllSongsUseCase().collect { songs ->
            _uiState.update { it.copy(songs = songs, isLoading = false) }
        }
    }
}

// Compose
@Composable
fun MusicLibraryScreen(viewModel: MusicLibraryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        CircularProgressIndicator()
    } else {
        LazyColumn {
            items(uiState.songs) { song -> SongItem(song) }
        }
    }
}
```

## Wearable Communication Channels

### DataClient (Persistent Data)
- **Purpose:** Guaranteed delivery, survives disconnection
- **Usage:** Playlists, songs (sync metadata)
- **Path:** `/playlists`, `/songs`

### MessageClient (Fire-and-Forget)
- **Purpose:** Best-effort delivery
- **Usage:** Playback commands, download progress
- **Path:** `/sync/playlists`, `/download/request`

### ChannelClient (Streaming)
- **Purpose:** Reliable bi-directional stream
- **Usage:** Audio file transfer
- **Path:** `/stream/audio/{songId}`

### CapabilityClient (Discovery)
- **Purpose:** Detect companion app
- **Usage:** Check if phone/watch app installed

## Sync Paths (WearPaths Constants)

```kotlin
// /shared/src/main/java/dev/sadakat/qit/shared/constants/WearPaths.kt
object WearPaths {
    const val PLAYLISTS = "/playlists"
    const val SONGS = "/songs"
    const val SYNC_PLAYLISTS = "/sync/playlists"
    const val SYNC_SONGS = "/sync/songs"
    const val DOWNLOAD_REQUEST = "/download/request"
    const val DOWNLOAD_PROGRESS = "/download/progress"
    const val STREAM_AUDIO = "/stream/audio/"
    const val CONNECTION_STATUS = "/connection/status"
}
```

## Delta Sync Strategy

```
On manual sync:
    ├─ Get lastSyncTimestamp from preferences
    ├─ Query changes since timestamp from SyncMetadata
    │   └─ ChangeRecord: entityId, entityType, operation, timestamp
    ├─ Filter to only modified playlists/songs
    ├─ Send only changed entities
    └─ Update lastSyncTimestamp
```

## Domain Events

```kotlin
// Event types
sealed class DomainEvent {
    data class SyncStarted(val timestamp: Long) : DomainEvent()
    data class SyncCompleted(val result: SyncResult) : DomainEvent()
    data class SyncFailed(val error: String) : DomainEvent()
    data class DownloadStarted(val songId: SongId) : DomainEvent()
    data class DownloadCompleted(val songId: SongId) : DomainEvent()
    data class PlaybackStarted(val songId: SongId) : DomainEvent()
}

// Publisher
class DomainEventPublisher {
    private val _events = MutableSharedFlow<DomainEvent>()
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    suspend fun publish(event: DomainEvent) {
        _events.emit(event)
    }
}
```

## Error Handling Pattern

All repository methods return `Result<T>`:
```kotlin
suspend fun syncPlaylistToWatch(playlist: Playlist): Result<Unit>

// Usage
val result = syncRepository.syncPlaylistToWatch(playlist)
result.fold(
    onSuccess = { /* success handling */ },
    onFailure = { error -> /* error handling */ }
)
```

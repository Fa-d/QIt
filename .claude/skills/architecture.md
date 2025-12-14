# QIt Clean Architecture

## Layer Dependencies (Dependency Inversion)

```
Presentation Layer (ViewModels, Compose UI)
    ↓ depends on
Application Layer (Use Cases)
    ↓ depends on
Domain Layer (Entities, Interfaces, Services)
    ↑ implemented by
Infrastructure Layer (Room, Wearable API, DataStore)
```

**Key Principle:** Domain layer has ZERO dependencies on outer layers.

## Layer Details

### 1. Domain Layer (`shared/domain/`)
Pure business logic - zero Android dependencies.

**Components:**
- **Entities:** Rich models with behavior (`Song`, `Playlist`)
- **Value Objects:** Immutable, self-validating (`SongId`, `Duration`, `FileSize`, `AudioQuality`, `DownloadStatus`)
- **Repository Interfaces:** Data access contracts
- **Domain Services:** `SyncCoordinator`, `StreamingCoordinator`, `PlaylistOrchestrator`
- **Domain Events:** `SyncStarted`, `SyncCompleted`, `DownloadCompleted`

### 2. Application Layer (`*/application/`)
Use cases - application-specific orchestration.

**Phone Use Cases:**
- `ScanMusicLibraryUseCase`, `GetAllSongsUseCase`, `SearchSongsUseCase`
- `CreatePlaylistUseCase`, `AddSongToPlaylistUseCase`, `DeletePlaylistUseCase`
- `SyncAllToWatchUseCase`, `PerformDeltaSyncUseCase`

**Wear Use Cases:**
- `DownloadSongUseCase`, `DownloadPlaylistUseCase`, `CancelDownloadUseCase`
- `PlaySongUseCase` (determines local vs. streaming)
- `GetStorageInfoUseCase`

**Base Classes:**
```kotlin
abstract class BaseUseCase<Type> {
    abstract suspend operator fun invoke(): Type
}

abstract class UseCaseWithParams<Params, Type> {
    abstract suspend operator fun invoke(params: Params): Type
}
```

### 3. Infrastructure Layer (`*/infrastructure/`)
Implementation details - frameworks, databases, network.

**Phone Infrastructure:**
- `RoomMusicRepository`: MediaStore scanning + Room
- `RoomPlaylistRepository`: Playlist CRUD
- `WearableSyncRepository`: Phone-watch sync
- `BasicStreamingRepository`: Audio streaming

**Wear Infrastructure:**
- `WearMusicRepository`: Local song storage
- `WearStreamingRepository`: Incoming audio streams
- `WearDownloadRepository`: Download management
- `StorageManager`: Watch storage management

### 4. Presentation Layer (`*/presentation/`)
UI - ViewModels and Jetpack Compose screens.

**Pattern:** MVVM with StateFlow
```kotlin
@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val scanMusicLibraryUseCase: ScanMusicLibraryUseCase,
    private val getAllSongsUseCase: GetAllSongsUseCase
) : ViewModel() {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
}
```

## Design Patterns Used

1. **Repository Pattern** - Abstracts data access
2. **Use Case Pattern** - Single responsibility actions
3. **Dependency Inversion** - Interfaces in domain, implementations in infrastructure
4. **Value Object Pattern** - Type-safe, immutable objects
5. **Aggregate Pattern** - Playlist as aggregate root
6. **Factory Pattern** - `Song.create()`, `SongId.generate()`
7. **Strategy Pattern** - Streaming strategies, conflict resolution
8. **MVVM** - ViewModel + StateFlow + Compose
9. **DTO Pattern** - Domain ↔ Network serialization

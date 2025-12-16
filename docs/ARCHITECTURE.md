# QIt - WearOS Music Streaming App

## Architecture Overview

This app follows **Domain-Driven Design (DDD)** and **Clean Architecture** principles with a clear separation of concerns.

### Module Structure

```
QIt/
├── app/           # Phone application (Android)
├── wear/          # Watch application (WearOS)
└── shared/        # Shared domain logic and models
```

## Clean Architecture Layers

### 1. Domain Layer (`shared/domain/`)
**Pure business logic - no Android dependencies**

- **Entities**: `Song`, `Playlist` - Rich models with behavior
- **Value Objects**: `Duration`, `FileSize`, `AudioQuality`, `DownloadStatus`, `PlaybackState`, `StreamingMode`
- **Repository Interfaces**: Contracts for data access (defined in domain, implemented in infrastructure)
- **Domain Services**: `PlaylistOrchestrator`, `SyncCoordinator`, `StreamingCoordinator`
- **Domain Events**: Event-driven communication between layers

**Key Features:**
- Type-safe IDs (`SongId`, `PlaylistId`)
- Business rules enforced in entities (e.g., max 1000 songs per playlist)
- Immutable value objects with formatting and validation
- Rich behavior (e.g., `Song.estimatedSizeForQuality()`)

### 2. Application Layer (`app/application/` & `wear/application/`)
**Use cases - application-specific business rules**

**Phone App Use Cases:**
- Music: `ScanMusicLibraryUseCase`, `GetAllSongsUseCase`, `SearchSongsUseCase`
- Playlist: `CreatePlaylistUseCase`, `AddSongToPlaylistUseCase`, `GetAllPlaylistsUseCase`, `DeletePlaylistUseCase`
- Sync: `SyncAllToWatchUseCase`, `SyncPlaylistToWatchUseCase`, `PerformDeltaSyncUseCase`

**Wear App Use Cases:**
- Sync: `RequestPlaylistSyncUseCase`
- Playlist: `GetAllPlaylistsUseCase`, `GetPlaylistSongsUseCase`
- Download: `DownloadSongUseCase`, `DownloadPlaylistUseCase`, `CancelDownloadUseCase`, `GetDownloadedSongsUseCase`
- Playback: `PlaySongUseCase` (determines local vs streaming)
- Storage: `GetStorageInfoUseCase`

### 3. Infrastructure Layer (`app/infrastructure/` & `wear/infrastructure/`)
**Implementation details - frameworks and tools**

**Phone App:**
- `RoomMusicRepository` - MediaStore scanning + Room database
- `RoomPlaylistRepository` - Playlist CRUD operations
- `DataStoreSettingsRepository` - User preferences (quality, auto-sync, etc.)
- `WearableSyncRepository` - Phone-watch communication via Wearable Data Layer
- `BasicStreamingRepository` - Audio streaming via ChannelClient

**Mappers:**
- `SongMapper` - Domain `Song` ↔ Room `SongEntity`
- `PlaylistMapper` - Domain `Playlist` ↔ Room `PlaylistEntity`
- `DtoMapper` - Domain models ↔ DTOs (for network serialization)

### 4. Presentation Layer (`app/presentation/` & `wear/presentation/`)
**UI - ViewModels and Compose screens**

- **ViewModels**: Use use cases, expose state as `StateFlow`
- **Screens**: Jetpack Compose UI
- **Navigation**: Navigation Compose

**Example (Phone):**
```kotlin
@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val scanMusicLibraryUseCase: ScanMusicLibraryUseCase,
    private val getAllSongsUseCase: GetAllSongsUseCase
) : ViewModel() {
    // Uses use cases, not repositories directly
}
```

## Data Flow

### Phone → Watch Sync
```
1. User action (e.g., "Sync to Watch")
   ↓
2. ViewModel calls SyncAllToWatchUseCase
   ↓
3. Use case coordinates:
   - SyncCoordinator gets playlists from PlaylistRepository
   - Maps to DTOs and serializes to JSON
   - SyncRepository sends via MessageClient to watch
   ↓
4. Watch receives via PhoneDataService
   ↓
5. Deserializes JSON, maps to domain entities
   ↓
6. Saves to watch database
```

### Audio Streaming (MVP Goal)
```
1. User selects song on watch
   ↓
2. PlaySongUseCase determines strategy:
   - Local if downloaded
   - Stream if not downloaded + phone connected
   ↓
3. StreamingCoordinator requests stream from phone
   ↓
4. Phone opens ChannelClient, streams audio file
   ↓
5. Watch receives stream, plays with ExoPlayer
```

## Technology Stack

- **Language**: Kotlin
- **DI**: Hilt (Dagger)
- **Database**: Room
- **Settings**: DataStore
- **Serialization**: kotlinx.serialization
- **Async**: Coroutines + Flow
- **UI**: Jetpack Compose (Material3 for phone, Wear Compose for watch)
- **Playback**: Media3 (ExoPlayer + MediaSession)
- **Phone-Watch**: Wearable Data Layer (MessageClient, DataClient, ChannelClient)

## Current Status (MVP)

### ✅ Completed
1. **Domain Layer**: Complete with 17 files (entities, value objects, repositories, services, events)
2. **Application Layer**: 21 use cases across both apps
3. **Infrastructure (Phone)**:
   - RoomMusicRepository
   - RoomPlaylistRepository
   - DataStoreSettingsRepository
   - WearableSyncRepository
   - BasicStreamingRepository
4. **DI Setup**: Hilt modules configured
5. **DTO Layer**: JSON serialization with kotlinx.serialization
6. **ViewModels**: Template created (MusicLibraryViewModel uses use cases)

### 🚧 Remaining for MVP

#### Phone App:
1. Update remaining ViewModels (PlaylistViewModel, WatchSyncViewModel)
2. Implement WatchDataService handlers (currently TODOs)
3. Add file streaming logic to BasicStreamingRepository

#### Wear App:
1. Create similar infrastructure:
   - Mappers (SongMapper, PlaylistMapper)
   - Repositories (RoomMusicRepository, RoomPlaylistRepository, etc.)
2. Update ViewModels to use use cases
3. Implement PhoneDataService handlers
4. Initialize MusicPlaybackService with MediaSession

#### Both:
5. Test full flow: Scan → Create Playlist → Sync → Stream → Play
6. Fix any build errors

## Next Steps for Full Feature Set

After MVP works:
1. **Download Management**: WorkManager, queue system, storage cleanup
2. **Quality Selection**: UI for quality settings, transcoding with MediaCodec
3. **Progressive Download**: Fallback streaming mode
4. **Auto-sync**: Background sync when phone connects
5. **Error Handling**: Retry logic, user feedback
6. **UI Polish**: Loading states, empty states, error messages
7. **Testing**: Unit, integration, and UI tests

## File Count

**Total Created: ~65 files**
- Domain: 17 files
- Application: 21 files
- Infrastructure: 10 files
- DTOs: 5 files
- DI/Config: 12 files

## Build Instructions

1. **Sync Gradle**: Open project in Android Studio, sync Gradle
2. **Build Phone App**: `./gradlew :app:assembleDebug`
3. **Build Wear App**: `./gradlew :wear:assembleDebug`
4. **Install Both**: Install on phone and paired watch

## Key Design Patterns

- **Repository Pattern**: Clean data access abstraction
- **Use Case Pattern**: Single responsibility for business operations
- **Factory Pattern**: Entity creation methods
- **Observer Pattern**: Domain events, Flow-based reactivity
- **Strategy Pattern**: Streaming modes (real-time vs progressive vs local)
- **Aggregate Pattern**: Playlist enforces consistency of song references
- **Value Object Pattern**: Immutable, self-validating objects

## Dependencies Inversion

```
Presentation Layer (ViewModels)
    ↓ depends on
Application Layer (Use Cases)
    ↓ depends on
Domain Layer (Entities, Repository Interfaces, Services)
    ↑ implemented by
Infrastructure Layer (Room, Wearable, DataStore)
```

**Key Principle**: Domain layer has ZERO dependencies on outer layers. Infrastructure implements domain interfaces.

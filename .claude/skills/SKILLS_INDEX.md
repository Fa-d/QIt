# QIt Claude Skills Index

## Quick Reference

This directory contains Claude skills for the QIt project - a phone-to-watch music streaming app.

## Available Skills

| Skill File | Description | Key Topics |
|------------|-------------|------------|
| `project-overview.md` | High-level project overview | Modules, tech stack, namespace |
| `architecture.md` | Clean Architecture details | Layers, patterns, MVVM |
| `domain-entities.md` | Domain entities and value objects | Song, Playlist, SongId, Duration, FileSize |
| `repositories.md` | Repository patterns | Interfaces, implementations, Room |
| `dependency-injection.md` | Hilt DI setup | Modules, bindings, scopes |
| `data-flow.md` | Data flow patterns | Sync flow, streaming, UI state |
| `ui-compose.md` | Jetpack Compose UI | Screens, ViewModels, components |
| `file-locations.md` | File location reference | All important file paths |
| `build-config.md` | Build configuration | Gradle, dependencies, commands |
| `wearable-communication.md` | Phone-watch communication | DataClient, MessageClient, ChannelClient |
| `media-playback.md` | Audio playback | ExoPlayer, MediaSession, streaming |
| `domain-services.md` | Domain services | SyncCoordinator, StreamingCoordinator |

## Quick Lookups

### Module Namespaces
- Phone: `dev.sadakat.qit`
- Wear: `dev.sadakat.qit.wear`
- Shared: `dev.sadakat.qit.shared`

### Key Entry Points
- Phone: `QItApplication`, `MainActivity`
- Wear: `WearApplication`, `MainActivity`

### Core Domain Entities
- `Song` - Music track with metadata
- `Playlist` - Collection of songs (aggregate root)
- `SongId`, `PlaylistId` - Type-safe identifiers
- `Duration`, `FileSize` - Value objects
- `AudioQuality`, `DownloadStatus` - Enums/sealed classes

### Key Repositories
- `MusicRepository` - Song CRUD + scanning
- `PlaylistRepository` - Playlist CRUD
- `SyncRepository` - Phone-watch sync
- `StreamingRepository` - Audio streaming
- `DownloadRepository` - Download management

### Key Services
- `SyncCoordinator` - Sync orchestration
- `StreamingCoordinator` - Playback strategy
- `PlaylistOrchestrator` - Playlist operations

### Build Commands
```bash
./gradlew build                    # Build all
./gradlew :app:installDebug        # Install phone app
./gradlew :wear:installDebug       # Install wear app
./gradlew lint                     # Run lint
```

### Common File Patterns

**Use Cases:** `/*/application/usecase/*/`
**Repositories:** `/*/infrastructure/*/`
**ViewModels:** `/*/presentation/viewmodel/`
**Screens:** `/*/presentation/screens/`
**DI Modules:** `/*/di/`

## When to Use Which Skill

| Task | Skill to Read |
|------|---------------|
| Understanding project structure | `project-overview.md` |
| Adding new domain entity | `domain-entities.md` |
| Creating new repository | `repositories.md` |
| Adding new use case | `architecture.md` |
| Modifying UI | `ui-compose.md` |
| Phone-watch communication | `wearable-communication.md` |
| Audio playback changes | `media-playback.md` |
| Finding file locations | `file-locations.md` |
| Build/dependency issues | `build-config.md` |
| DI configuration | `dependency-injection.md` |
| Data flow questions | `data-flow.md` |
| Sync/streaming logic | `domain-services.md` |

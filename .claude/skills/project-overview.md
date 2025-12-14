# QIt Project Overview

## What is QIt?
QIt is a phone-to-watch music streaming Android application built with Clean Architecture and Domain-Driven Design (DDD).

## Module Structure

```
QIt/
├── app/                 # Phone Application (Android)
├── wear/                # Wear OS Application
├── shared/              # Shared Domain Logic (Android Library)
├── gradle/libs.versions.toml  # Centralized dependencies
└── build.gradle.kts     # Root build config
```

## Module Responsibilities

### app/ (Phone App)
- Scans device music library via MediaStore
- Manages playlists and songs
- Syncs data to watch via Wearable Data Layer
- Streams audio to watch in real-time

### wear/ (Wear OS App)
- Receives synced playlists/songs from phone
- Downloads songs for offline playback
- Streams audio from phone when not downloaded
- Plays music via Media3/ExoPlayer

### shared/ (Domain Library)
- Pure business logic (NO Android dependencies)
- Domain entities: Song, Playlist
- Value objects: SongId, PlaylistId, Duration, FileSize, AudioQuality
- Repository interfaces
- Domain services: SyncCoordinator, StreamingCoordinator

## Package Organization

Each module follows this structure:
```
module/
├── application/        # Use cases
├── di/                 # Hilt DI modules
├── infrastructure/     # Repository implementations
├── presentation/       # ViewModels, Compose screens
├── data/               # Room entities, DAOs
└── service/            # Background services
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0.0 |
| UI | Jetpack Compose |
| DI | Hilt 2.51.1 |
| Database | Room 2.6.1 |
| Audio | Media3/ExoPlayer 1.5.0 |
| Phone-Watch | Wearable Data Layer 18.1.0 |
| Settings | DataStore |
| Serialization | kotlinx.serialization |
| Min SDK | API 26 |
| Target SDK | API 36 |

## Namespace
- Phone app: `dev.sadakat.qit`
- Wear app: `dev.sadakat.qit.wear`
- Shared: `dev.sadakat.qit.shared`

# QIt File Locations Reference

## Shared Module (Domain Layer)

### Entities
| File | Path |
|------|------|
| Song Entity | `/shared/src/main/java/dev/sadakat/qit/shared/domain/entity/Song.kt` |
| Playlist Entity | `/shared/src/main/java/dev/sadakat/qit/shared/domain/entity/Playlist.kt` |
| SongId | `/shared/src/main/java/dev/sadakat/qit/shared/domain/entity/SongId.kt` |
| PlaylistId | `/shared/src/main/java/dev/sadakat/qit/shared/domain/entity/PlaylistId.kt` |

### Value Objects
| File | Path |
|------|------|
| Duration | `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/Duration.kt` |
| FileSize | `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/FileSize.kt` |
| AudioQuality | `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/AudioQuality.kt` |
| DownloadStatus | `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/DownloadStatus.kt` |
| StreamingMode | `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/StreamingMode.kt` |
| PlaybackState | `/shared/src/main/java/dev/sadakat/qit/shared/domain/valueobject/PlaybackState.kt` |

### Repository Interfaces
| File | Path |
|------|------|
| MusicRepository | `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/MusicRepository.kt` |
| PlaylistRepository | `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/PlaylistRepository.kt` |
| SyncRepository | `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/SyncRepository.kt` |
| StreamingRepository | `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/StreamingRepository.kt` |
| DownloadRepository | `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/DownloadRepository.kt` |
| SettingsRepository | `/shared/src/main/java/dev/sadakat/qit/shared/domain/repository/SettingsRepository.kt` |

### Domain Services
| File | Path |
|------|------|
| SyncCoordinator | `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/SyncCoordinator.kt` |
| StreamingCoordinator | `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/StreamingCoordinator.kt` |
| PlaylistOrchestrator | `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/PlaylistOrchestrator.kt` |
| ConflictResolver | `/shared/src/main/java/dev/sadakat/qit/shared/domain/service/ConflictResolver.kt` |

### Domain Events
| File | Path |
|------|------|
| DomainEvent | `/shared/src/main/java/dev/sadakat/qit/shared/domain/event/DomainEvent.kt` |
| DomainEventPublisher | `/shared/src/main/java/dev/sadakat/qit/shared/domain/event/DomainEventPublisher.kt` |

### DTOs
| File | Path |
|------|------|
| SongDto | `/shared/src/main/java/dev/sadakat/qit/shared/dto/SongDto.kt` |
| PlaylistDto | `/shared/src/main/java/dev/sadakat/qit/shared/dto/PlaylistDto.kt` |
| DtoMapper | `/shared/src/main/java/dev/sadakat/qit/shared/dto/DtoMapper.kt` |

### Constants
| File | Path |
|------|------|
| WearPaths | `/shared/src/main/java/dev/sadakat/qit/shared/constants/WearPaths.kt` |

---

## Phone App Module

### Application Entry Point
| File | Path |
|------|------|
| QItApplication | `/app/src/main/java/dev/sadakat/qit/QItApplication.kt` |
| MainActivity | `/app/src/main/java/dev/sadakat/qit/MainActivity.kt` |
| AndroidManifest | `/app/src/main/AndroidManifest.xml` |

### Use Cases
| File | Path |
|------|------|
| ScanMusicLibraryUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/music/ScanMusicLibraryUseCase.kt` |
| GetAllSongsUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/music/GetAllSongsUseCase.kt` |
| SearchSongsUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/music/SearchSongsUseCase.kt` |
| CreatePlaylistUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/playlist/CreatePlaylistUseCase.kt` |
| AddSongToPlaylistUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/playlist/AddSongToPlaylistUseCase.kt` |
| GetAllPlaylistsUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/playlist/GetAllPlaylistsUseCase.kt` |
| DeletePlaylistUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/playlist/DeletePlaylistUseCase.kt` |
| SyncAllToWatchUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/sync/SyncAllToWatchUseCase.kt` |
| SyncPlaylistToWatchUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/sync/SyncPlaylistToWatchUseCase.kt` |
| PerformDeltaSyncUseCase | `/app/src/main/java/dev/sadakat/qit/application/usecase/sync/PerformDeltaSyncUseCase.kt` |

### DI Modules
| File | Path |
|------|------|
| RepositoryModule | `/app/src/main/java/dev/sadakat/qit/di/RepositoryModule.kt` |
| DatabaseModule | `/app/src/main/java/dev/sadakat/qit/di/DatabaseModule.kt` |
| DomainModule | `/app/src/main/java/dev/sadakat/qit/di/DomainModule.kt` |

### Infrastructure (Repositories)
| File | Path |
|------|------|
| RoomMusicRepository | `/app/src/main/java/dev/sadakat/qit/infrastructure/persistence/repository/RoomMusicRepository.kt` |
| RoomPlaylistRepository | `/app/src/main/java/dev/sadakat/qit/infrastructure/persistence/repository/RoomPlaylistRepository.kt` |
| WearableSyncRepository | `/app/src/main/java/dev/sadakat/qit/infrastructure/wearable/WearableSyncRepository.kt` |
| BasicStreamingRepository | `/app/src/main/java/dev/sadakat/qit/infrastructure/wearable/BasicStreamingRepository.kt` |
| DataStoreSettingsRepository | `/app/src/main/java/dev/sadakat/qit/infrastructure/datastore/DataStoreSettingsRepository.kt` |

### Database
| File | Path |
|------|------|
| MusicDatabase | `/app/src/main/java/dev/sadakat/qit/data/local/MusicDatabase.kt` |
| SongEntity | `/app/src/main/java/dev/sadakat/qit/data/local/entity/SongEntity.kt` |
| PlaylistEntity | `/app/src/main/java/dev/sadakat/qit/data/local/entity/PlaylistEntity.kt` |
| PlaylistSongCrossRef | `/app/src/main/java/dev/sadakat/qit/data/local/entity/PlaylistSongCrossRef.kt` |
| SongDao | `/app/src/main/java/dev/sadakat/qit/data/local/dao/SongDao.kt` |
| PlaylistDao | `/app/src/main/java/dev/sadakat/qit/data/local/dao/PlaylistDao.kt` |

### Mappers
| File | Path |
|------|------|
| SongMapper | `/app/src/main/java/dev/sadakat/qit/infrastructure/persistence/mapper/SongMapper.kt` |
| PlaylistMapper | `/app/src/main/java/dev/sadakat/qit/infrastructure/persistence/mapper/PlaylistMapper.kt` |

### Presentation
| File | Path |
|------|------|
| MusicLibraryViewModel | `/app/src/main/java/dev/sadakat/qit/presentation/viewmodel/MusicLibraryViewModel.kt` |
| PlaylistViewModel | `/app/src/main/java/dev/sadakat/qit/presentation/viewmodel/PlaylistViewModel.kt` |
| SyncViewModel | `/app/src/main/java/dev/sadakat/qit/presentation/viewmodel/SyncViewModel.kt` |
| MusicLibraryScreen | `/app/src/main/java/dev/sadakat/qit/presentation/screens/MusicLibraryScreen.kt` |
| PlaylistListScreen | `/app/src/main/java/dev/sadakat/qit/presentation/screens/PlaylistListScreen.kt` |
| WatchSyncScreen | `/app/src/main/java/dev/sadakat/qit/presentation/screens/WatchSyncScreen.kt` |
| QItNavGraph | `/app/src/main/java/dev/sadakat/qit/presentation/navigation/QItNavGraph.kt` |

### Services
| File | Path |
|------|------|
| WatchDataService | `/app/src/main/java/dev/sadakat/qit/service/WatchDataService.kt` |

### Theme
| File | Path |
|------|------|
| Theme.kt | `/app/src/main/java/dev/sadakat/qit/ui/theme/Theme.kt` |
| Color.kt | `/app/src/main/java/dev/sadakat/qit/ui/theme/Color.kt` |
| Type.kt | `/app/src/main/java/dev/sadakat/qit/ui/theme/Type.kt` |

---

## Wear App Module

### Application Entry Point
| File | Path |
|------|------|
| WearApplication | `/wear/src/main/java/dev/sadakat/qit/wear/WearApplication.kt` |
| MainActivity | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/MainActivity.kt` |
| AndroidManifest | `/wear/src/main/AndroidManifest.xml` |

### Use Cases
| File | Path |
|------|------|
| RequestPlaylistSyncUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/sync/RequestPlaylistSyncUseCase.kt` |
| GetAllPlaylistsUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/playlist/GetAllPlaylistsUseCase.kt` |
| GetPlaylistSongsUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/playlist/GetPlaylistSongsUseCase.kt` |
| DownloadSongUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/download/DownloadSongUseCase.kt` |
| DownloadPlaylistUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/download/DownloadPlaylistUseCase.kt` |
| CancelDownloadUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/download/CancelDownloadUseCase.kt` |
| GetDownloadedSongsUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/download/GetDownloadedSongsUseCase.kt` |
| PlaySongUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/playback/PlaySongUseCase.kt` |
| GetStorageInfoUseCase | `/wear/src/main/java/dev/sadakat/qit/wear/application/usecase/storage/GetStorageInfoUseCase.kt` |

### DI Modules
| File | Path |
|------|------|
| RepositoryModule | `/wear/src/main/java/dev/sadakat/qit/wear/di/RepositoryModule.kt` |
| DatabaseModule | `/wear/src/main/java/dev/sadakat/qit/wear/di/DatabaseModule.kt` |
| DomainModule | `/wear/src/main/java/dev/sadakat/qit/wear/di/DomainModule.kt` |

### Infrastructure (Repositories)
| File | Path |
|------|------|
| WearMusicRepository | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/repository/WearMusicRepository.kt` |
| WearPlaylistRepositoryImpl | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/repository/WearPlaylistRepositoryImpl.kt` |
| WearSyncRepository | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/sync/WearSyncRepository.kt` |
| WearSettingsRepository | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/settings/WearSettingsRepository.kt` |

### Streaming Infrastructure
| File | Path |
|------|------|
| WearStreamingRepository | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/WearStreamingRepository.kt` |
| StreamingAudioBuffer | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/StreamingAudioBuffer.kt` |
| StreamingAudioSource | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/StreamingAudioSource.kt` |
| StreamingIntegrationExample | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/streaming/StreamingIntegrationExample.kt` |

### Download Infrastructure
| File | Path |
|------|------|
| WearDownloadRepository | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/download/WearDownloadRepository.kt` |
| DownloadWorker | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/download/DownloadWorker.kt` |

### Storage
| File | Path |
|------|------|
| StorageManager | `/wear/src/main/java/dev/sadakat/qit/wear/infrastructure/storage/StorageManager.kt` |

### Database
| File | Path |
|------|------|
| WearMusicDatabase | `/wear/src/main/java/dev/sadakat/qit/wear/data/local/WearMusicDatabase.kt` |
| SongEntity | `/wear/src/main/java/dev/sadakat/qit/wear/data/local/entity/SongEntity.kt` |
| PlaylistEntity | `/wear/src/main/java/dev/sadakat/qit/wear/data/local/entity/PlaylistEntity.kt` |

### Presentation
| File | Path |
|------|------|
| PlaylistViewModel | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/viewmodel/PlaylistViewModel.kt` |
| PlaybackViewModel | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/viewmodel/PlaybackViewModel.kt` |
| DownloadViewModel | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/viewmodel/DownloadViewModel.kt` |
| PlaylistListScreen | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/PlaylistListScreen.kt` |
| SongListScreen | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/SongListScreen.kt` |
| SongListWithDownloadsScreen | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/SongListWithDownloadsScreen.kt` |
| PlaybackScreen | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/PlaybackScreen.kt` |
| DownloadsScreen | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/screens/DownloadsScreen.kt` |
| WearNavGraph | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/navigation/WearNavGraph.kt` |

### Components
| File | Path |
|------|------|
| DownloadComponents | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/components/DownloadComponents.kt` |
| SyncStatusComponents | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/components/SyncStatusComponents.kt` |

### Services
| File | Path |
|------|------|
| MusicPlaybackService | `/wear/src/main/java/dev/sadakat/qit/wear/service/MusicPlaybackService.kt` |
| PhoneDataService | `/wear/src/main/java/dev/sadakat/qit/wear/service/PhoneDataService.kt` |

### Playback
| File | Path |
|------|------|
| PlaybackController | `/wear/src/main/java/dev/sadakat/qit/wear/playback/PlaybackController.kt` |

### Theme
| File | Path |
|------|------|
| Theme.kt | `/wear/src/main/java/dev/sadakat/qit/wear/presentation/theme/Theme.kt` |

---

## Build Configuration

| File | Path |
|------|------|
| Root build.gradle.kts | `/build.gradle.kts` |
| App build.gradle.kts | `/app/build.gradle.kts` |
| Wear build.gradle.kts | `/wear/build.gradle.kts` |
| Shared build.gradle.kts | `/shared/build.gradle.kts` |
| Version Catalog | `/gradle/libs.versions.toml` |
| Settings | `/settings.gradle.kts` |

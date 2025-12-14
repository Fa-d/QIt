# QIt Dependency Injection (Hilt)

## Setup

### Application Classes

**Phone App:**
```kotlin
// /app/src/main/java/dev/sadakat/qit/QItApplication.kt
@HiltAndroidApp
class QItApplication : Application()
```

**Wear App:**
```kotlin
// /wear/src/main/java/dev/sadakat/qit/wear/WearApplication.kt
@HiltAndroidApp
class WearApplication : Application()
```

### Gradle Configuration

```kotlin
// app/build.gradle.kts or wear/build.gradle.kts
plugins {
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
}
```

## Phone App Hilt Modules

### RepositoryModule
**Location:** `/app/src/main/java/dev/sadakat/qit/di/RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        impl: RoomMusicRepository
    ): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: RoomPlaylistRepository
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: WearableSyncRepository
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: DataStoreSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindStreamingRepository(
        impl: BasicStreamingRepository
    ): StreamingRepository

    companion object {
        @Provides
        @Singleton
        fun provideDataClient(@ApplicationContext context: Context): DataClient =
            Wearable.getDataClient(context)

        @Provides
        @Singleton
        fun provideMessageClient(@ApplicationContext context: Context): MessageClient =
            Wearable.getMessageClient(context)

        @Provides
        @Singleton
        fun provideChannelClient(@ApplicationContext context: Context): ChannelClient =
            Wearable.getChannelClient(context)

        @Provides
        @Singleton
        fun provideNodeClient(@ApplicationContext context: Context): NodeClient =
            Wearable.getNodeClient(context)

        @Provides
        @Singleton
        fun provideCapabilityClient(@ApplicationContext context: Context): CapabilityClient =
            Wearable.getCapabilityClient(context)
    }
}
```

### DatabaseModule
**Location:** `/app/src/main/java/dev/sadakat/qit/di/DatabaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusicDatabase =
        Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database"
        ).build()

    @Provides
    fun provideSongDao(database: MusicDatabase): SongDao =
        database.songDao()

    @Provides
    fun providePlaylistDao(database: MusicDatabase): PlaylistDao =
        database.playlistDao()
}
```

### DomainModule
**Location:** `/app/src/main/java/dev/sadakat/qit/di/DomainModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideSyncCoordinator(
        playlistRepository: PlaylistRepository,
        musicRepository: MusicRepository,
        syncRepository: SyncRepository
    ): SyncCoordinator = SyncCoordinator(
        playlistRepository = playlistRepository,
        musicRepository = musicRepository,
        syncRepository = syncRepository
    )

    @Provides
    @Singleton
    fun provideStreamingCoordinator(
        musicRepository: MusicRepository,
        streamingRepository: StreamingRepository,
        syncRepository: SyncRepository,
        settingsRepository: SettingsRepository
    ): StreamingCoordinator = StreamingCoordinator(
        musicRepository = musicRepository,
        streamingRepository = streamingRepository,
        syncRepository = syncRepository,
        settingsRepository = settingsRepository
    )

    @Provides
    @Singleton
    fun providePlaylistOrchestrator(
        playlistRepository: PlaylistRepository,
        musicRepository: MusicRepository
    ): PlaylistOrchestrator = PlaylistOrchestrator(
        playlistRepository = playlistRepository,
        musicRepository = musicRepository
    )

    @Provides
    @Singleton
    fun provideDomainEventPublisher(): DomainEventPublisher =
        DomainEventPublisher()
}
```

## Wear App Hilt Modules

### WearRepositoryModule
**Location:** `/wear/src/main/java/dev/sadakat/qit/wear/di/RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class WearRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        impl: WearMusicRepository
    ): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: WearPlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: WearSyncRepository
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindStreamingRepository(
        impl: WearStreamingRepository
    ): StreamingRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: WearDownloadRepository
    ): DownloadRepository
}
```

## ViewModel Injection

```kotlin
@HiltViewModel
class MusicLibraryViewModel @Inject constructor(
    private val scanMusicLibraryUseCase: ScanMusicLibraryUseCase,
    private val getAllSongsUseCase: GetAllSongsUseCase
) : ViewModel() {
    // ViewModel implementation
}
```

## Compose Integration

```kotlin
@Composable
fun MusicLibraryScreen(
    viewModel: MusicLibraryViewModel = hiltViewModel()
) {
    // Screen implementation
}
```

## Use Case Injection

Use cases are automatically provided by Hilt constructor injection:

```kotlin
class ScanMusicLibraryUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) : BaseUseCase<Result<List<Song>>>() {
    override suspend fun invoke(): Result<List<Song>> =
        musicRepository.scanMusicLibrary()
}
```

## WorkManager Integration (Wear)

```kotlin
// wear/build.gradle.kts
dependencies {
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
}
```

```kotlin
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, workerParams) {
    // Worker implementation
}
```

## Scope Summary

| Scope | Lifecycle | Usage |
|-------|-----------|-------|
| `@Singleton` | App lifetime | Repositories, Domain services |
| `@ActivityRetainedScoped` | Activity retained | ViewModel dependencies |
| `@ViewModelScoped` | ViewModel lifetime | ViewModel-specific |
| `@ActivityScoped` | Activity lifetime | Activity-specific |

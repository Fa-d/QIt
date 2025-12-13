package dev.sadakat.qit.wear.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.service.StreamingCoordinator
import dev.sadakat.qit.wear.application.usecase.download.CancelDownloadUseCase
import dev.sadakat.qit.wear.application.usecase.download.DownloadPlaylistUseCase
import dev.sadakat.qit.wear.application.usecase.download.DownloadSongUseCase
import dev.sadakat.qit.wear.application.usecase.download.GetDownloadedSongsUseCase
import dev.sadakat.qit.wear.application.usecase.playback.PlaySongUseCase
import dev.sadakat.qit.wear.application.usecase.playlist.GetAllPlaylistsUseCase
import dev.sadakat.qit.wear.application.usecase.playlist.GetPlaylistSongsUseCase
import dev.sadakat.qit.wear.application.usecase.storage.GetStorageInfoUseCase
import dev.sadakat.qit.wear.application.usecase.sync.RequestPlaylistSyncUseCase
import javax.inject.Singleton

/**
 * Hilt module providing application layer use cases for wear
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    // Playlist Use Cases
    @Provides
    @Singleton
    fun provideGetAllPlaylistsUseCase(
        playlistRepository: PlaylistRepository
    ): GetAllPlaylistsUseCase {
        return GetAllPlaylistsUseCase(playlistRepository)
    }

    @Provides
    @Singleton
    fun provideGetPlaylistSongsUseCase(
        playlistRepository: PlaylistRepository
    ): GetPlaylistSongsUseCase {
        return GetPlaylistSongsUseCase(playlistRepository)
    }

    // Download Use Cases
    @Provides
    @Singleton
    fun provideGetDownloadedSongsUseCase(
        musicRepository: MusicRepository
    ): GetDownloadedSongsUseCase {
        return GetDownloadedSongsUseCase(musicRepository)
    }

    @Provides
    @Singleton
    fun provideDownloadSongUseCase(
        downloadRepository: DownloadRepository,
        settingsRepository: SettingsRepository,
        eventPublisher: DomainEventPublisher
    ): DownloadSongUseCase {
        return DownloadSongUseCase(downloadRepository, settingsRepository, eventPublisher)
    }

    @Provides
    @Singleton
    fun provideDownloadPlaylistUseCase(
        downloadRepository: DownloadRepository,
        playlistRepository: PlaylistRepository,
        settingsRepository: SettingsRepository,
        eventPublisher: DomainEventPublisher
    ): DownloadPlaylistUseCase {
        return DownloadPlaylistUseCase(
            downloadRepository,
            playlistRepository,
            settingsRepository,
            eventPublisher
        )
    }

    @Provides
    @Singleton
    fun provideCancelDownloadUseCase(
        downloadRepository: DownloadRepository,
        eventPublisher: DomainEventPublisher
    ): CancelDownloadUseCase {
        return CancelDownloadUseCase(downloadRepository, eventPublisher)
    }

    // Playback Use Cases
    @Provides
    @Singleton
    fun providePlaySongUseCase(
        musicRepository: MusicRepository,
        streamingCoordinator: StreamingCoordinator,
        eventPublisher: DomainEventPublisher
    ): PlaySongUseCase {
        return PlaySongUseCase(musicRepository, streamingCoordinator, eventPublisher)
    }

    // Storage Use Cases
    @Provides
    @Singleton
    fun provideGetStorageInfoUseCase(
        downloadRepository: DownloadRepository
    ): GetStorageInfoUseCase {
        return GetStorageInfoUseCase(downloadRepository)
    }

    // Sync Use Cases
    @Provides
    @Singleton
    fun provideRequestPlaylistSyncUseCase(
        syncRepository: SyncRepository,
        eventPublisher: DomainEventPublisher
    ): RequestPlaylistSyncUseCase {
        return RequestPlaylistSyncUseCase(syncRepository, eventPublisher)
    }
}

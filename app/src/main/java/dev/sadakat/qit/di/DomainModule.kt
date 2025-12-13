package dev.sadakat.qit.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.shared.domain.event.DomainEventPublisher
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import dev.sadakat.qit.shared.domain.service.PlaylistOrchestrator
import dev.sadakat.qit.shared.domain.service.StreamingCoordinator
import dev.sadakat.qit.shared.domain.service.SyncCoordinator
import javax.inject.Singleton

/**
 * Hilt module providing domain layer dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideDomainEventPublisher(): DomainEventPublisher {
        return DomainEventPublisher.getInstance()
    }

    @Provides
    @Singleton
    fun providePlaylistOrchestrator(
        playlistRepository: PlaylistRepository,
        musicRepository: MusicRepository
    ): PlaylistOrchestrator {
        return PlaylistOrchestrator(playlistRepository, musicRepository)
    }

    @Provides
    @Singleton
    fun provideSyncCoordinator(
        playlistRepository: PlaylistRepository,
        musicRepository: MusicRepository,
        syncRepository: SyncRepository
    ): SyncCoordinator {
        return SyncCoordinator(playlistRepository, musicRepository, syncRepository)
    }

    @Provides
    @Singleton
    fun provideStreamingCoordinator(
        streamingRepository: StreamingRepository,
        syncRepository: SyncRepository,
        settingsRepository: SettingsRepository
    ): StreamingCoordinator {
        return StreamingCoordinator(streamingRepository, syncRepository, settingsRepository)
    }
}

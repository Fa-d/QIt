package dev.sadakat.qit.wear.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.wear.infrastructure.repository.WearMusicRepository
import dev.sadakat.qit.wear.infrastructure.repository.WearPlaylistRepositoryImpl
import dev.sadakat.qit.wear.infrastructure.streaming.WearStreamingRepository
import javax.inject.Singleton

/**
 * Hilt module providing infrastructure layer implementations
 * Binds domain repository interfaces to wear implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InfrastructureModule {

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
    abstract fun bindStreamingRepository(
        impl: WearStreamingRepository
    ): StreamingRepository
}

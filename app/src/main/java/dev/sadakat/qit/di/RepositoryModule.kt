package dev.sadakat.qit.di

import android.content.Context
import com.google.android.gms.wearable.Wearable
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.infrastructure.persistence.repository.DataStoreSettingsRepository
import dev.sadakat.qit.infrastructure.persistence.repository.RoomMusicRepository
import dev.sadakat.qit.infrastructure.persistence.repository.RoomPlaylistRepository
import dev.sadakat.qit.infrastructure.wearable.BasicStreamingRepository
import dev.sadakat.qit.infrastructure.wearable.WearableDownloadRepository
import dev.sadakat.qit.infrastructure.wearable.WearableSyncRepository
import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.repository.MusicRepository
import dev.sadakat.qit.shared.domain.repository.PlaylistRepository
import dev.sadakat.qit.shared.domain.repository.SettingsRepository
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.shared.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Scope for application-lifetime background work (media transfers, syncs).
 *
 * The WearableListenerService's own scope is cancelled in onDestroy, and GMS
 * destroys listener services when idle - minutes-long downloads/streams must
 * therefore NOT run there.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Hilt module providing repository implementations
 */
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
    abstract fun bindSettingsRepository(
        impl: DataStoreSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: WearableSyncRepository
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindStreamingRepository(
        impl: BasicStreamingRepository
    ): StreamingRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: WearableDownloadRepository
    ): DownloadRepository

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Provides
        @Singleton
        fun provideDataClient(@ApplicationContext context: Context) =
            Wearable.getDataClient(context)

        @Provides
        @Singleton
        fun provideMessageClient(@ApplicationContext context: Context) =
            Wearable.getMessageClient(context)

        @Provides
        @Singleton
        fun provideNodeClient(@ApplicationContext context: Context) =
            Wearable.getNodeClient(context)

        @Provides
        @Singleton
        fun provideChannelClient(@ApplicationContext context: Context) =
            Wearable.getChannelClient(context)

        @Provides
        @Singleton
        fun provideCapabilityClient(@ApplicationContext context: Context) =
            Wearable.getCapabilityClient(context)
    }
}

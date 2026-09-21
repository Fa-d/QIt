package dev.sadakat.qit.wear.di

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.wear.data.local.dao.PlaylistDao
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.data.repository.PhoneSyncRepository
import dev.sadakat.qit.wear.data.repository.PlaylistRepository
import dev.sadakat.qit.wear.infrastructure.storage.StorageManager
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Scope for application-lifetime background work (syncs, nack handling).
 *
 * The WearableListenerService's own scope is cancelled in onDestroy, and GMS
 * unbinds and destroys listener services as soon as callbacks return - work
 * whose message was already consumed must therefore NOT run there (mirror of
 * the app's dev.sadakat.qit.di.ApplicationScope).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun providePlaylistRepository(
        playlistDao: PlaylistDao,
        songDao: SongDao
    ): PlaylistRepository {
        return PlaylistRepository(playlistDao, songDao)
    }

    @Provides
    @Singleton
    fun providePhoneSyncRepository(
        @ApplicationContext context: Context
    ): PhoneSyncRepository {
        return PhoneSyncRepository(context)
    }

    
    // Wearable API Clients
    @Provides
    @Singleton
    fun provideMessageClient(
        @ApplicationContext context: Context
    ): MessageClient {
        return Wearable.getMessageClient(context)
    }

    @Provides
    @Singleton
    fun provideChannelClient(
        @ApplicationContext context: Context
    ): ChannelClient {
        return Wearable.getChannelClient(context)
    }

    @Provides
    @Singleton
    fun provideNodeClient(
        @ApplicationContext context: Context
    ): NodeClient {
        return Wearable.getNodeClient(context)
    }

    @Provides
    @Singleton
    fun provideCapabilityClient(
        @ApplicationContext context: Context
    ): CapabilityClient {
        return Wearable.getCapabilityClient(context)
    }

    @Provides
    @Singleton
    fun provideDataClient(
        @ApplicationContext context: Context
    ): DataClient {
        return Wearable.getDataClient(context)
    }
}

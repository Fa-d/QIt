package dev.sadakat.qit.wear.di

import android.content.Context
import com.google.android.gms.wearable.ChannelClient
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

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
}

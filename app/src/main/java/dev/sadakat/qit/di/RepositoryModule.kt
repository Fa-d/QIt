package dev.sadakat.qit.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.data.local.dao.PlaylistDao
import dev.sadakat.qit.data.local.dao.SongDao
import dev.sadakat.qit.data.repository.MusicRepository
import dev.sadakat.qit.data.repository.PlaylistRepository
import dev.sadakat.qit.data.repository.WatchSyncRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMusicRepository(
        @ApplicationContext context: Context,
        songDao: SongDao
    ): MusicRepository {
        return MusicRepository(context, songDao)
    }

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
    fun provideWatchSyncRepository(
        @ApplicationContext context: Context
    ): WatchSyncRepository {
        return WatchSyncRepository(context)
    }
}

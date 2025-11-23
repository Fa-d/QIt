package dev.sadakat.qit.wear.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.wear.data.local.WearMusicDatabase
import dev.sadakat.qit.wear.data.local.dao.PlaylistDao
import dev.sadakat.qit.wear.data.local.dao.SongDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWearMusicDatabase(
        @ApplicationContext context: Context
    ): WearMusicDatabase {
        return WearMusicDatabase.getDatabase(context)
    }

    @Provides
    fun providePlaylistDao(database: WearMusicDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideSongDao(database: WearMusicDatabase): SongDao {
        return database.songDao()
    }
}

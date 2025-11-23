package dev.sadakat.qit.wear.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.wear.playback.PlaybackManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    fun providePlaybackManager(
        @ApplicationContext context: Context
    ): PlaybackManager {
        return PlaybackManager(context)
    }
}

package dev.sadakat.qit.wear.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.shared.domain.repository.StreamingRepository
import dev.sadakat.qit.wear.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackScope

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    @PlaybackScope
    fun providePlaybackCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @Provides
    @Singleton
    fun providePlaybackManager(
        @ApplicationContext context: Context,
        streamingRepository: StreamingRepository,
        exoPlayer: ExoPlayer,
        @PlaybackScope coroutineScope: CoroutineScope
    ): PlaybackManager {
        return PlaybackManager(context, streamingRepository, exoPlayer, coroutineScope)
    }
}

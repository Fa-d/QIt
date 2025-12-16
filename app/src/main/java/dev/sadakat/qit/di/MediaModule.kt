package dev.sadakat.qit.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.session.MediaSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.player.MusicSessionCallback
import dev.sadakat.qit.playback.AudioFocusManager
import dev.sadakat.qit.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention

/**
 * Dagger module for providing media-related dependencies
 */
@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    @PlaybackScope
    fun providePlaybackCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @Provides
    @Singleton
    fun provideAudioFocusManager(
        @ApplicationContext context: Context
    ): AudioFocusManager {
        return AudioFocusManager(context)
    }

    @Provides
    @Singleton
    fun provideTrackSelector(@ApplicationContext context: Context): DefaultTrackSelector {
        return DefaultTrackSelector(context)
    }

    @Provides
    @Singleton
    fun provideLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideDataSourceFactory(
        @ApplicationContext context: Context
    ): DataSource.Factory {
        return DefaultDataSource.Factory(context)
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        trackSelector: DefaultTrackSelector,
        loadControl: LoadControl
    ): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    @Provides
    @Singleton
    fun providePlaybackManager(
        @ApplicationContext context: Context,
        audioFocusManager: AudioFocusManager,
        @PlaybackScope coroutineScope: CoroutineScope
    ): PlaybackManager {
        return PlaybackManager(context, audioFocusManager, coroutineScope)
    }

    @Provides
    @Singleton
    fun provideMusicSessionCallback(
        playbackManager: PlaybackManager,
        @PlaybackScope coroutineScope: CoroutineScope
    ): MusicSessionCallback {
        return MusicSessionCallback(playbackManager, coroutineScope)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackScope
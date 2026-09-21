package dev.sadakat.qit.wear.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.wear.infrastructure.streaming.SchemeAwareDataSource
import dev.sadakat.qit.wear.infrastructure.streaming.WearStreamingRepository
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideTrackSelector(@ApplicationContext context: Context): DefaultTrackSelector {
        return DefaultTrackSelector(context)
    }

    /**
     * Provides the single app-wide ExoPlayer.
     *
     * It is wired with a [SchemeAwareDataSource] factory so the same player can
     * handle local files (file://), content URIs and live audio streamed from
     * the phone (streaming://). Both [dev.sadakat.qit.wear.playback.PlaybackManager]
     * and [dev.sadakat.qit.wear.service.MusicPlaybackService] share this instance,
     * so UI controls and the MediaSession always act on the same player.
     */
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        trackSelector: DefaultTrackSelector,
        wearStreamingRepository: WearStreamingRepository
    ): ExoPlayer {
        val dataSourceFactory = SchemeAwareDataSource.Factory(
            context,
            wearStreamingRepository::getOrCreateBuffer
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)
            )
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}

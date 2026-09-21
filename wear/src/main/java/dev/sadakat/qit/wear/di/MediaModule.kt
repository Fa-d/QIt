package dev.sadakat.qit.wear.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.shared.quran.audio.QuranCache
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    /**
     * The app-wide player, shared by the UI and [dev.sadakat.qit.wear.service.QuranPlaybackService].
     * Reads downloaded audio from [QuranCache] and streams the rest.
     */
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        quranCache: QuranCache
    ): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(quranCache.playbackDataSourceFactory)
            )
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}

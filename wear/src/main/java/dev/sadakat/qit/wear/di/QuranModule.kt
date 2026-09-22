package dev.sadakat.qit.wear.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.core.data.audio.AssetAudioTimings
import dev.sadakat.qit.core.data.audio.MediaSurahDownloads
import dev.sadakat.qit.core.data.audio.QuranCache
import dev.sadakat.qit.core.data.listening.RoomListeningHistory
import dev.sadakat.qit.core.data.player.ExoQuranPlayer
import dev.sadakat.qit.core.data.settings.DataStoreQuranSettings
import dev.sadakat.qit.core.data.text.AssetQuranText
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.AudioTimings
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object QuranModule {

    @Provides
    @Singleton
    fun provideQuranCache(@ApplicationContext context: Context): QuranCache = QuranCache.get(context)

    @Provides
    @Singleton
    fun provideQuranText(@ApplicationContext context: Context): QuranText = AssetQuranText(context)

    @Provides
    @Singleton
    fun provideQuranSettings(@ApplicationContext context: Context): QuranSettings = DataStoreQuranSettings(context)

    @Provides
    @Singleton
    fun provideSurahDownloads(@ApplicationContext context: Context, quranCache: QuranCache): SurahDownloads =
        MediaSurahDownloads(context, quranCache)

    @Provides
    @Singleton
    fun provideAudioTimings(@ApplicationContext context: Context): AudioTimings = AssetAudioTimings(context)

    @Provides
    @Singleton
    fun provideListeningHistory(@ApplicationContext context: Context): ListeningHistory =
        RoomListeningHistory.create(context)

    @Provides
    @Singleton
    @Suppress("LongParameterList") // The player's collaborators, each one injected.
    fun provideExoQuranPlayer(
        @ApplicationContext context: Context,
        exoPlayer: ExoPlayer,
        quranText: QuranText,
        settings: QuranSettings,
        timings: AudioTimings,
        history: ListeningHistory,
    ): ExoQuranPlayer = ExoQuranPlayer(
        context = context,
        exoPlayer = exoPlayer,
        quranText = quranText,
        settings = settings,
        timings = timings,
        history = history,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    )

    @Provides
    fun provideQuranPlayer(player: ExoQuranPlayer): QuranPlayer = player
}

package dev.sadakat.qit.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.core.data.audio.MediaSurahDownloads
import dev.sadakat.qit.core.data.audio.QuranCache
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import dev.sadakat.qit.core.data.player.ExoQuranPlayer
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.data.settings.DataStoreQuranSettings
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.data.text.AssetQuranText
import dev.sadakat.qit.core.domain.repository.QuranText
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
    fun provideSurahDownloads(
        @ApplicationContext context: Context,
        quranCache: QuranCache
    ): SurahDownloads = MediaSurahDownloads(context, quranCache)

    @Provides
    @Singleton
    fun provideExoQuranPlayer(
        @ApplicationContext context: Context,
        exoPlayer: ExoPlayer,
        quranText: QuranText,
        settings: QuranSettings
    ): ExoQuranPlayer = ExoQuranPlayer(
        context,
        exoPlayer,
        quranText,
        settings,
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    )

    @Provides
    fun provideQuranPlayer(player: ExoQuranPlayer): QuranPlayer = player
}

package dev.sadakat.qit.wear.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.shared.quran.audio.MediaSurahDownloads
import dev.sadakat.qit.shared.quran.audio.QuranCache
import dev.sadakat.qit.shared.quran.audio.SurahDownloads
import dev.sadakat.qit.shared.quran.player.ExoQuranPlayer
import dev.sadakat.qit.shared.quran.player.QuranPlayer
import dev.sadakat.qit.shared.quran.settings.DataStoreQuranSettings
import dev.sadakat.qit.shared.quran.settings.QuranSettings
import dev.sadakat.qit.shared.quran.text.AssetQuranText
import dev.sadakat.qit.shared.quran.text.QuranText
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
    fun provideQuranPlayer(
        @ApplicationContext context: Context,
        exoPlayer: ExoPlayer,
        quranText: QuranText,
        settings: QuranSettings
    ): QuranPlayer = ExoQuranPlayer(
        context,
        exoPlayer,
        quranText,
        settings,
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    )
}

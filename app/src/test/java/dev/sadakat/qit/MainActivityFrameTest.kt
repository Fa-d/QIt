package dev.sadakat.qit

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qit.core.data.audio.AssetAudioTimings
import dev.sadakat.qit.core.data.audio.MediaSurahDownloads
import dev.sadakat.qit.core.data.audio.QuranCache
import dev.sadakat.qit.core.data.listening.RoomListeningHistory
import dev.sadakat.qit.core.data.player.ExoQuranPlayer
import dev.sadakat.qit.core.data.settings.DataStoreQuranSettings
import dev.sadakat.qit.core.data.text.AssetQuranText
import dev.sadakat.qit.core.data.text.AssetWordMeanings
import dev.sadakat.qit.core.designsystem.skin.QItSkins
import dev.sadakat.qit.core.designsystem.skin.QItStyle
import dev.sadakat.qit.core.designsystem.skin.QItTone
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.AudioTimings
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import dev.sadakat.qit.core.domain.repository.WordMeanings
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.di.QuranModule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton

@HiltAndroidTest
@UninstallModules(QuranModule::class)
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class MainActivityFrameTest {

    @get:Rule
    val hilt = HiltAndroidRule(this)

    private val settings = HeldReadingPrefs(ReadingPrefs(themeMode = ThemeMode.DARK))

    @Test
    fun `the first composed frame keeps the window the settings painted`() {
        // Robolectric's day mode is the light system: onCreate paints the dark page of the saved
        // dark theme, and the first composed frame — still on the state-in placeholder
        // (SYSTEM/MUSHAF, not ready) — must not repaint it with the bright paper.
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create().start().resume().visible().get()

        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(QItSkins.of(QItStyle.MUSHAF, QItTone.DARK).colors.background.toArgb(), pageColor(activity))

        // Once the settings reach the ViewModel the effect applies them, and later tone changes
        // still repaint: guarding the placeholder frame must not disable the effect.
        settings.release.complete(Unit)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(QItSkins.of(QItStyle.MUSHAF, QItTone.DARK).colors.background.toArgb(), pageColor(activity))

        runBlocking { settings.updateReadingPrefs { it.copy(themeMode = ThemeMode.SEPIA) } }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(QItSkins.of(QItStyle.MUSHAF, QItTone.SEPIA).colors.background.toArgb(), pageColor(activity))
    }

    private fun pageColor(activity: MainActivity): Int =
        (shadowOf(activity.window).backgroundDrawable as ColorDrawable).color

    /** Only [QuranSettings] is swapped for [settings]; the rest is [QuranModule], unchanged. */
    @Module
    @InstallIn(SingletonComponent::class)
    inner class TestQuranModule {

        @Provides
        @Singleton
        fun provideQuranSettings(): QuranSettings = settings

        @Provides
        @Singleton
        fun provideQuranCache(@ApplicationContext context: Context): QuranCache = QuranModule.provideQuranCache(context)

        @Provides
        @Singleton
        fun provideQuranText(@ApplicationContext context: Context): QuranText = QuranModule.provideQuranText(context)

        @Provides
        @Singleton
        fun provideSurahDownloads(@ApplicationContext context: Context, quranCache: QuranCache): SurahDownloads =
            QuranModule.provideSurahDownloads(context, quranCache)

        @Provides
        @Singleton
        fun provideAudioTimings(@ApplicationContext context: Context): AudioTimings =
            QuranModule.provideAudioTimings(context)

        @Provides
        @Singleton
        fun provideWordMeanings(@ApplicationContext context: Context): WordMeanings =
            QuranModule.provideWordMeanings(context)

        @Provides
        @Singleton
        fun provideListeningHistory(@ApplicationContext context: Context): ListeningHistory =
            QuranModule.provideListeningHistory(context)

        @Provides
        @Singleton
        @Suppress("LongParameterList") // The player's collaborators, each one injected.
        fun provideExoQuranPlayer(
            @ApplicationContext context: Context,
            exoPlayer: ExoPlayer,
            quranText: QuranText,
            quranSettings: QuranSettings,
            timings: AudioTimings,
            history: ListeningHistory,
        ): ExoQuranPlayer = ExoQuranPlayer(
            context = context,
            exoPlayer = exoPlayer,
            quranText = quranText,
            settings = quranSettings,
            timings = timings,
            history = history,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        )

        @Provides
        fun provideQuranPlayer(player: ExoQuranPlayer): QuranPlayer = player
    }
}

/**
 * Serves the saved prefs to `onCreate`'s synchronous read, then holds them back from the
 * ViewModel until [release]: the first composed frame runs on the state-in placeholder, exactly
 * as on a cold start before the DataStore emission has arrived.
 */
private class HeldReadingPrefs private constructor(
    private val saved: FakeQuranSettings,
    val release: CompletableDeferred<Unit>,
) : QuranSettings by saved {

    constructor(prefs: ReadingPrefs) : this(FakeQuranSettings(readingPrefs = prefs), CompletableDeferred())

    private val readByOnCreate = AtomicBoolean(false)

    override val readingPrefs: Flow<ReadingPrefs> = flow {
        if (readByOnCreate.compareAndSet(false, true)) {
            emit(saved.readingPrefs.value)
        } else {
            release.await()
            emitAll(saved.readingPrefs)
        }
    }
}

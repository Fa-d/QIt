package dev.sadakat.qit.wear.service

import android.app.Application
import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.data.player.ExoQuranPlayer
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.testing.FakeQuranSettings
import dev.sadakat.qit.core.testing.FakeQuranText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * ExoQuranPlayer resolves the app's MediaSessionService by intent filter and starts it so playback
 * outlives the UI. Only this module's manifest declares that service, so the service start is
 * asserted here rather than in :core:data.
 */
@RunWith(AndroidJUnit4::class)
class ExoQuranPlayerServiceStartTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settings = FakeQuranSettings()
    private val text = FakeQuranText()
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var player: ExoQuranPlayer

    @Before
    fun setUp() {
        exoPlayer = TestExoPlayerBuilder(context).build()
        player = ExoQuranPlayer(context, exoPlayer, text, settings, CoroutineScope(Dispatchers.Main))
    }

    @After
    fun tearDown() {
        exoPlayer.release()
    }

    @Test
    fun `play starts the media session service so playback survives the app going to the background`() {
        player.play(2, fromAyah = 1, mode = RecitationMode.ARABIC_ONLY)

        runMainLooperUntil { player.nowPlaying.value != null }

        assertMediaSessionServiceStarted()
    }

    @Test
    fun `restoreLast that resumes playback starts the media session service too`() {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ONLY)

        player.restoreLast(playWhenReady = true)

        runMainLooperUntil { player.nowPlaying.value != null }

        assertMediaSessionServiceStarted()
    }

    @Test
    fun `restoreLast without playback does not start the service`() {
        settings.lastPosition.value = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ONLY)

        player.restoreLast(playWhenReady = false)

        runMainLooperUntil { player.nowPlaying.value != null }

        assertNull(shadowOf(context as Application).peekNextStartedService())
    }

    private fun assertMediaSessionServiceStarted() {
        val started = requireNotNull(shadowOf(context as Application).nextStartedService) {
            "the media session service was not started"
        }
        assertEquals("androidx.media3.session.MediaSessionService", started.action)
        assertEquals("dev.sadakat.qit", started.component?.packageName ?: started.`package`)
    }
}

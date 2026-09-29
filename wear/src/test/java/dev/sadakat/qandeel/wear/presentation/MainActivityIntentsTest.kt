package dev.sadakat.qandeel.wear.presentation

import android.app.Application
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.player.NowPlaying
import dev.sadakat.qandeel.core.testing.FakeQuranPlayer
import dev.sadakat.qandeel.wear.presentation.WearIntents.EXTRA_OPEN_NOW_PLAYING
import dev.sadakat.qandeel.wear.presentation.WearIntents.EXTRA_RESUME_PLAYBACK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class MainActivityIntentsTest {

    @Test
    fun `the tile extra asks for Now playing`() {
        assertTrue(wantsNowPlaying(Bundle().apply { putBoolean(EXTRA_OPEN_NOW_PLAYING, true) }))
    }

    @Test
    fun `a plain launch opens the hub`() {
        assertFalse(wantsNowPlaying(null))
        assertFalse(wantsNowPlaying(Bundle()))
        assertFalse(wantsNowPlaying(Bundle().apply { putBoolean(EXTRA_OPEN_NOW_PLAYING, false) }))
    }

    @Test
    fun `the tile's resume extra asks to resume`() {
        assertTrue(wantsResume(Bundle().apply { putBoolean(EXTRA_RESUME_PLAYBACK, true) }))
        assertFalse(wantsResume(Bundle().apply { putBoolean(EXTRA_OPEN_NOW_PLAYING, true) }))
        assertFalse(wantsResume(null))
    }

    @Test
    fun `resuming re-queues the saved position when nothing is queued`() {
        val player = FakeQuranPlayer()

        resumePlayback(player)

        assertEquals(1, player.restoreCalls)
    }

    @Test
    fun `resuming plays what's paused and leaves what's playing alone`() {
        val player = FakeQuranPlayer()
        player.nowPlaying.value =
            NowPlaying(18, 10, Track.ARABIC, RecitationMode.ARABIC_ONLY, isPlaying = false, isBuffering = false)

        resumePlayback(player)
        assertEquals(true, player.nowPlaying.value?.isPlaying)

        resumePlayback(player)
        assertEquals(true, player.nowPlaying.value?.isPlaying)
        assertEquals(0, player.restoreCalls)
    }
}

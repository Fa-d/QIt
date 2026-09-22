package dev.sadakat.qit.wear.presentation

import android.app.Application
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.wear.presentation.WearIntents.EXTRA_OPEN_NOW_PLAYING
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
}

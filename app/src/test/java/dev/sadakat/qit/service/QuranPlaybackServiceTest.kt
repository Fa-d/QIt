package dev.sadakat.qit.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class QuranPlaybackServiceTest {

    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun `the service registers its session so Media3 posts the media notification`() {
        // Media3 only adds a session (and with it the notification + foreground handling) when a
        // controller connects. The app drives its player directly, so nothing ever connects:
        // found on a Galaxy Watch, where playback ran with no notification or controls.
        val service = Robolectric.buildService(QuranPlaybackService::class.java).create().get()

        assertEquals(1, service.sessions.size)
    }
}

package dev.sadakat.qit.core.data.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class QuranDownloadServiceTest {

    @Test
    fun `the system can create the service`() {
        // Regression: a field initialised from the Service's context crashed at construction on a
        // Pixel 7, before Android attaches the base context ("Unable to create service").
        val controller = Robolectric.buildService(QuranDownloadService::class.java).create()

        assertNotNull(controller.get())

        controller.destroy()
    }

    @After
    fun tearDown() {
        releaseProcessWideQuranCache()
    }
}

package dev.sadakat.qandeel.core.data.audio

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadProgress
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.domain.audio.DownloadBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class DownloadNotificationsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val names = mapOf(18 to "Al-Kahf")

    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE).toString()

    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT).toString()

    private fun progress(batch: DownloadBatch, waiting: Boolean = false) =
        DownloadNotifications.progress(context, batch, names, waiting, contentIntent = null)

    @Test
    fun `one surah shows its name and its whole progress`() {
        val notification = progress(DownloadBatch(listOf(18), 0.64f))

        assertEquals("Downloading Al-Kahf", notification.title())
        assertEquals("64%", notification.text())
        assertEquals(100, notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertEquals(64, notification.extras.getInt(Notification.EXTRA_PROGRESS))
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test
    fun `several surahs are counted`() {
        assertEquals("Downloading 3 surahs", progress(DownloadBatch(listOf(18, 36, 67), 0.1f)).title())
    }

    @Test
    fun `a surah whose name isn't known yet is numbered`() {
        assertEquals("Downloading Surah 36", progress(DownloadBatch(listOf(36), 0f)).title())
    }

    @Test
    fun `without a network it says it is waiting`() {
        assertEquals(
            "Waiting for a network connection",
            progress(DownloadBatch(listOf(18), 0.5f), waiting = true).text(),
        )
    }

    @Test
    fun `a batch of no surah is indeterminate`() {
        val notification = progress(DownloadBatch(emptyList(), 1f))
        assertEquals("Downloading surah audio", notification.title())
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
    }

    @Test
    fun `a finished surah is ready offline, or says it failed`() {
        val done = DownloadNotifications.finished(context, 18, names, failed = false, contentIntent = null)
        val failed = DownloadNotifications.finished(context, 18, names, failed = true, contentIntent = null)

        assertEquals("Al-Kahf is ready offline", done.title())
        assertEquals("Al-Kahf didn't finish downloading", failed.title())
        assertTrue(done.flags and Notification.FLAG_AUTO_CANCEL != 0)
    }

    @Test
    fun `a finished surah is posted when notifications are allowed`() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notification = DownloadNotifications.finished(context, 18, names, failed = false, contentIntent = null)

        DownloadNotifications.postFinished(context, 18, notification)

        val manager = context.getSystemService(NotificationManager::class.java)
        assertEquals("Al-Kahf is ready offline", shadowOf(manager).getNotification(7418).title())
    }

    @Test
    fun `the batch is built from the queued and running files only`() {
        val downloads = listOf(
            download("ar/8", Download.STATE_DOWNLOADING, percent = 50f),
            download("ar/9", Download.STATE_QUEUED),
            download("ar/10", Download.STATE_REMOVING),
            download("ar/11", Download.STATE_COMPLETED, percent = 100f),
        )

        val batch = QuranDownloadService.batchOf(downloads)

        assertEquals(listOf(2), batch.surahs)
        assertEquals((287 - 0.5f - 1f) / 287, batch.progress, 0.0001f)
    }

    @Test
    fun `a file's surahs are those it belongs to alone`() {
        assertEquals(listOf(2), QuranDownloadService.surahsOf("ar/8"))
        assertEquals(emptyList<Int>(), QuranDownloadService.surahsOf("ar/1")) // the shared basmala
    }

    private fun download(id: String, state: Int, percent: Float? = null): Download {
        val request = DownloadRequest.Builder(id, Uri.parse("https://example.com/$id.mp3")).build()
        val progress = DownloadProgress().also { if (percent != null) it.percentDownloaded = percent }
        return Download(request, state, 0, 0, 1_000, Download.STOP_REASON_NONE, Download.FAILURE_REASON_NONE, progress)
    }
}

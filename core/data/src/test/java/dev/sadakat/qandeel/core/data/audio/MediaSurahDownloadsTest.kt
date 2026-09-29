package dev.sadakat.qandeel.core.data.audio

import android.app.Application
import android.os.Looper
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.WritableDownloadIndex
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.test.utils.DownloadBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.domain.audio.FileDownloadState
import dev.sadakat.qandeel.core.domain.audio.QuranAudioUrls
import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState
import dev.sadakat.qandeel.core.domain.repository.stateOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class MediaSurahDownloadsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val cache = QuranCache.get(context)
    private val downloads = MediaSurahDownloads(context, cache)

    @Before
    fun setUp() {
        // Keep the tests hermetic: queued tasks must never reach the network. The manager starts
        // paused, but the DownloadService started by download() resumes it — requirements that
        // Robolectric never satisfies (isDeviceIdleMode is false by default) are the real guard.
        cache.downloadManager.requirements =
            Requirements(Requirements.NETWORK or Requirements.DEVICE_IDLE)
        clearIndex()
    }

    /**
     * Releases this test's process-wide [QuranCache] singleton: Robolectric hands every test a
     * fresh Application but keeps the singleton, and the surviving DownloadManager cannot
     * unregister its RequirementsWatcher receiver from a later test's environment.
     */
    @After
    fun tearDown() {
        releaseProcessWideQuranCache()
    }

    @Test
    fun `queued stopped downloading and restarting files are active`() {
        val activeStates = intArrayOf(
            Download.STATE_QUEUED,
            Download.STATE_STOPPED,
            Download.STATE_DOWNLOADING,
            Download.STATE_RESTARTING,
        )
        for (state in activeStates) {
            val download = DownloadBuilder("ar/1").setState(state).build()
            assertEquals(FileDownloadState.ACTIVE, fileDownloadStateOf(download))
        }
    }

    @Test
    fun `completed and failed files map to their own states`() {
        val completed = DownloadBuilder("ar/1").setState(Download.STATE_COMPLETED).build()
        assertEquals(FileDownloadState.COMPLETED, fileDownloadStateOf(completed))
        val failed = DownloadBuilder("ar/1")
            .setState(Download.STATE_FAILED)
            .setFailureReason(Download.FAILURE_REASON_UNKNOWN)
            .build()
        assertEquals(FileDownloadState.FAILED, fileDownloadStateOf(failed))
    }

    @Test
    fun `a file being removed is untracked`() {
        val removing = DownloadBuilder("ar/1").setState(Download.STATE_REMOVING).build()
        assertNull(fileDownloadStateOf(removing))
    }

    @Test
    fun `download queues every missing file and reports the surah as downloading`() {
        downloads.download(1, listOf(Track.ARABIC))

        await("every surah 1 file queued") { indexIds() == expectedIds(1, Track.ARABIC) }
        await("surah 1 downloading 0 of 7") {
            downloads.states.value.stateOf(1, Track.ARABIC) == SurahDownloadState.Downloading(0, 7)
        }
    }

    @Test
    fun `download skips files that are already completed`() {
        val done = QuranAudioUrls.verse(Track.ARABIC, 1)
        seedCompleted(done.id, done.url)
        // Constructed after seeding so the initial index load counts the completed file.
        val downloads = MediaSurahDownloads(context, cache)

        downloads.download(1, listOf(Track.ARABIC))

        // The seeded row survives untouched and the other six files are queued.
        await("every surah 1 file known") { indexIds() == expectedIds(1, Track.ARABIC) }
        await("surah 1 downloading 1 of 7") {
            downloads.states.value.stateOf(1, Track.ARABIC) == SurahDownloadState.Downloading(1, 7)
        }
    }

    @Test
    fun `remove keeps a shared basmala that another tracked surah needs`() {
        downloads.download(1, listOf(Track.ARABIC))
        await("every surah 1 file queued") { indexIds() == expectedIds(1, Track.ARABIC) }
        await("surah 1 tracked") { downloads.states.value.stateOf(1, Track.ARABIC) != SurahDownloadState.NotDownloaded }

        downloads.download(2, listOf(Track.ARABIC))
        await("every surah 1 and 2 file queued") {
            indexIds() == expectedIds(1, Track.ARABIC) + expectedIds(2, Track.ARABIC)
        }
        await("surah 2 tracked") { downloads.states.value.stateOf(2, Track.ARABIC) != SurahDownloadState.NotDownloaded }

        downloads.remove(2, listOf(Track.ARABIC))

        // "ar/1" survives because surah 1 still needs it; every other surah 2 file is gone.
        await("only surah 1 files remain") { indexIds() == expectedIds(1, Track.ARABIC) }
        await("surah 2 untracked") {
            downloads.states.value.stateOf(2, Track.ARABIC) == SurahDownloadState.NotDownloaded
        }
    }

    @Test
    fun `remove deletes every file including the shared basmala when no other surah is tracked`() {
        downloads.download(1, listOf(Track.ARABIC))
        await("every surah 1 file queued") { indexIds() == expectedIds(1, Track.ARABIC) }
        await("surah 1 tracked") { downloads.states.value.stateOf(1, Track.ARABIC) != SurahDownloadState.NotDownloaded }

        downloads.remove(1, listOf(Track.ARABIC))

        // "ar/1" is shared with 112 surahs, but none of them is tracked, so it goes too.
        await("index empty") { indexIds().isEmpty() }
        await("surah 1 untracked") {
            downloads.states.value.stateOf(1, Track.ARABIC) == SurahDownloadState.NotDownloaded
        }
    }

    /** The manager works on background threads; drain the main looper until [condition] holds. */
    private fun await(where: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.currentTimeMillis() < deadline) { diagnostics(where) }
            Thread.sleep(20)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Dumps the manager state so a timeout says *why* it timed out. */
    private fun diagnostics(where: String): String {
        val byState = cache.downloadManager.downloadIndex.getDownloads().use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add("${cursor.download.request.id}=${cursor.download.state}")
                }
            }
        }
        return "$where timed out. manager idle=${cache.downloadManager.isIdle} " +
            "paused=${cache.downloadManager.downloadsPaused} index=$byState states=${downloads.states.value}"
    }

    private fun indexIds(): Set<String> = cache.downloadManager.downloadIndex.getDownloads().use { cursor ->
        buildSet {
            while (cursor.moveToNext()) add(cursor.download.request.id)
        }
    }

    private fun expectedIds(surah: Int, track: Track): Set<String> =
        QuranAudioUrls.surahFiles(surah, track).mapTo(mutableSetOf()) { it.id }

    /** Writes a terminal row straight into the index, without going through the manager. */
    private fun seedCompleted(id: String, url: String) {
        val index = cache.downloadManager.downloadIndex as WritableDownloadIndex
        index.putDownload(DownloadBuilder(id).setUri(url).setState(Download.STATE_COMPLETED).build())
    }

    private fun clearIndex() {
        val ids = indexIds()
        for (id in ids) cache.downloadManager.removeDownload(id)
        await("index empty") { indexIds().isEmpty() }
    }

    companion object {
        private const val AWAIT_TIMEOUT_MS = 30_000L
    }
}

package dev.sadakat.qit.wear.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.data.link.ListeningResetMessage
import dev.sadakat.qit.core.data.link.QuranDownloadMessage
import dev.sadakat.qit.core.data.link.WearPaths
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.ListeningCounts
import dev.sadakat.qit.core.testing.FakeListeningHistory
import dev.sadakat.qit.core.testing.FakeSurahDownloads
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuranMessageServiceTest {

    private val downloads = FakeSurahDownloads()
    private val history = FakeListeningHistory()

    @Test
    fun `a valid download message downloads the requested surah and tracks`() {
        val payload = QuranDownloadMessage.of(2, listOf(Track.ARABIC, Track.BANGLA)).toBytes()

        handleQuranMessage(WearPaths.QURAN_DOWNLOAD, payload, downloads)

        assertEquals(listOf(2 to listOf(Track.ARABIC, Track.BANGLA)), downloads.downloadRequests)
    }

    @Test
    fun `messages on other paths are ignored`() {
        val payload = QuranDownloadMessage.of(2, listOf(Track.ARABIC)).toBytes()

        handleQuranMessage("/quran/other", payload, downloads)

        assertTrue(downloads.downloadRequests.isEmpty())
    }

    @Test
    fun `garbage bytes never crash and never download`() {
        handleQuranMessage(WearPaths.QURAN_DOWNLOAD, "not json at all".encodeToByteArray(), downloads)

        assertTrue(downloads.downloadRequests.isEmpty())
    }

    @Test
    fun `an out-of-range surah is ignored`() {
        val payload = QuranDownloadMessage(surah = 200, trackCodes = listOf("ar")).toBytes()

        handleQuranMessage(WearPaths.QURAN_DOWNLOAD, payload, downloads)

        assertTrue(downloads.downloadRequests.isEmpty())
    }

    @Test
    fun `a message without any known track is ignored`() {
        val payload = QuranDownloadMessage(surah = 2, trackCodes = listOf("xx")).toBytes()

        handleQuranMessage(WearPaths.QURAN_DOWNLOAD, payload, downloads)

        assertTrue(downloads.downloadRequests.isEmpty())
    }

    @Test
    fun `a reset newer than the watch's own forgets what it heard`() = runTest {
        history.recordHeard(AyahRef(1, 1), atMs = 1_000)
        history.lastResetAt.value = 1_000

        handleListeningReset(ListeningResetMessage(resetAt = 2_000).toBytes(), history)

        assertEquals(2_000, history.lastResetAt.value)
        assertEquals(ListeningCounts.EMPTY, history.counts.value)
    }

    @Test
    fun `a reset the watch already counted from is ignored`() = runTest {
        history.recordHeard(AyahRef(1, 1), atMs = 1_000)
        history.lastResetAt.value = 2_000

        handleListeningReset(ListeningResetMessage(resetAt = 2_000).toBytes(), history)
        handleListeningReset(ListeningResetMessage(resetAt = 1_000).toBytes(), history)

        assertEquals(2_000, history.lastResetAt.value)
        assertEquals(1, history.counts.value.count(1, 1))
    }

    @Test
    fun `garbage bytes never crash and never reset`() = runTest {
        handleListeningReset("not json at all".encodeToByteArray(), history)

        assertEquals(0L, history.lastResetAt.value)
    }
}

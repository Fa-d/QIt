package dev.sadakat.qit.wear.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.data.link.QuranDownloadMessage
import dev.sadakat.qit.core.data.link.WearPaths
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.testing.FakeSurahDownloads
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuranMessageServiceTest {

    private val downloads = FakeSurahDownloads()

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
}

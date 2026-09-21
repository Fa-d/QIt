package dev.sadakat.qit.core.domain.repository

import dev.sadakat.qit.core.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class SurahDownloadStateTest {

    private val map = mapOf(
        2 to mapOf(
            Track.ARABIC to SurahDownloadState.Downloading(4, 7),
            Track.BANGLA to SurahDownloadState.Failed(1, 8),
        ),
        3 to mapOf(Track.ARABIC to SurahDownloadState.Downloaded),
    )

    @Test
    fun `a missing pair is not downloaded`() {
        val empty = emptyMap<Int, Map<Track, SurahDownloadState>>()
        assertEquals(SurahDownloadState.NotDownloaded, empty.stateOf(2, Track.ARABIC))
        assertEquals(SurahDownloadState.NotDownloaded, map.stateOf(2, Track.ENGLISH))
    }

    @Test
    fun `stateOf returns the pair's own state`() {
        assertEquals(SurahDownloadState.Downloading(4, 7), map.stateOf(2, Track.ARABIC))
        assertEquals(SurahDownloadState.Downloaded, map.stateOf(3, Track.ARABIC))
    }

    @Test
    fun `combining tracks yields downloaded only when every track is downloaded`() {
        assertEquals(
            SurahDownloadState.Downloaded,
            mapOf(3 to mapOf(Track.ARABIC to SurahDownloadState.Downloaded)).stateOf(3, listOf(Track.ARABIC)),
        )
        // Downloaded plus never-requested is not fully available offline.
        assertEquals(
            SurahDownloadState.NotDownloaded,
            map.stateOf(3, listOf(Track.ARABIC, Track.BANGLA)),
        )
    }

    @Test
    fun `combining tracks yields not downloaded when no track was requested`() {
        assertEquals(
            SurahDownloadState.NotDownloaded,
            emptyMap<Int, Map<Track, SurahDownloadState>>().stateOf(2, listOf(Track.ARABIC, Track.BANGLA)),
        )
    }

    @Test
    fun `combining tracks prefers downloading and sums the file counts`() {
        assertEquals(
            SurahDownloadState.Downloading(5, 15),
            map.stateOf(2, listOf(Track.ARABIC, Track.BANGLA)),
        )
    }

    @Test
    fun `combining failed tracks without downloads sums the file counts`() {
        val failedOnly = mapOf(
            4 to mapOf(
                Track.ARABIC to SurahDownloadState.Failed(2, 5),
                Track.BANGLA to SurahDownloadState.Failed(3, 9),
            ),
        )
        assertEquals(
            SurahDownloadState.Failed(5, 14),
            failedOnly.stateOf(4, listOf(Track.ARABIC, Track.BANGLA)),
        )
        // A downloaded track never hides another track's failure.
        val withDownloaded =
            failedOnly + (4 to (failedOnly.getValue(4) + (Track.ENGLISH to SurahDownloadState.Downloaded)))
        assertEquals(
            SurahDownloadState.Failed(5, 14),
            withDownloaded.stateOf(4, listOf(Track.ARABIC, Track.BANGLA, Track.ENGLISH)),
        )
    }

    @Test
    fun `downloading progress is the completed share of the total`() {
        assertEquals(0.25f, SurahDownloadState.Downloading(1, 4).progress)
        assertEquals(0f, SurahDownloadState.Downloading(0, 0).progress)
        assertEquals(1f, SurahDownloadState.Downloading(4, 4).progress)
    }
}

package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.audio.QuranAudioUrls.surahFiles
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloadState.Downloaded
import dev.sadakat.qit.core.domain.repository.SurahDownloadState.Downloading
import dev.sadakat.qit.core.domain.repository.SurahDownloadState.Failed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadAggregationTest {

    private val surahOneIds = surahFiles(1, Track.ARABIC).map { it.id }
    private val surahTwoIds = surahFiles(2, Track.ARABIC).map { it.id }

    @Test
    fun `a pair with only a basmala file is untracked`() {
        // "ar/1" is shared by every surah's basmala; on its own it must not look like progress.
        assertNull(DownloadAggregation.stateOf(2, Track.ARABIC, mapOf("ar/1" to FileDownloadState.COMPLETED)))
        assertNull(DownloadAggregation.stateOf(2, Track.ARABIC, emptyMap()))
        assertNull(DownloadAggregation.stateOf(2, Track.ARABIC, mapOf("en/8" to FileDownloadState.COMPLETED)))
    }

    @Test
    fun `downloading another surah does not make al-fatiha look tracked`() {
        // "ar/1" is Al-Fatiha's first verse AND every other surah's basmala.
        val activeSurahTwo = surahTwoIds.associateWith { FileDownloadState.ACTIVE }
        val doneSurahTwo = surahTwoIds.associateWith { FileDownloadState.COMPLETED }
        assertNull(DownloadAggregation.stateOf(1, Track.ARABIC, activeSurahTwo))
        assertNull(DownloadAggregation.stateOf(1, Track.ARABIC, doneSurahTwo))
        assertEquals(Downloaded, DownloadAggregation.stateOf(2, Track.ARABIC, doneSurahTwo))
    }

    @Test
    fun `all files completed means downloaded`() {
        val files = surahOneIds.associateWith { FileDownloadState.COMPLETED }
        assertEquals(Downloaded, DownloadAggregation.stateOf(1, Track.ARABIC, files))
    }

    @Test
    fun `an active file means downloading, counting completed files`() {
        val files = surahOneIds.mapIndexed { index, id ->
            id to when (index) {
                in 0..2 -> FileDownloadState.COMPLETED
                in 3..4 -> FileDownloadState.ACTIVE
                else -> FileDownloadState.FAILED
            }
        }.toMap()
        assertEquals(Downloading(3, 7), DownloadAggregation.stateOf(1, Track.ARABIC, files))
    }

    @Test
    fun `without active files a failing pair is failed, counting completed files`() {
        val files = (surahOneIds - QuranAudioUrls.verse(Track.ARABIC, 1).id)
            .associateWith { FileDownloadState.COMPLETED } +
            (QuranAudioUrls.verse(Track.ARABIC, 1).id to FileDownloadState.FAILED)
        assertEquals(Failed(6, 7), DownloadAggregation.stateOf(1, Track.ARABIC, files))
    }

    @Test
    fun `a missing file keeps the pair from being downloaded`() {
        // The basmala is absent even though every verse file is completed.
        val verseFiles = surahTwoIds.drop(1).associateWith { FileDownloadState.COMPLETED }
        assertEquals(Failed(286, 287), DownloadAggregation.stateOf(2, Track.ARABIC, verseFiles))
    }

    @Test
    fun `the arabic basmala belongs to surah 1 and every surah with a basmala prefix`() {
        val sharers = DownloadAggregation.pairsContaining("ar/1")
        assertEquals(113, sharers.size)
        assertTrue(sharers.contains(1 to Track.ARABIC))
        assertTrue(sharers.contains(2 to Track.ARABIC))
        assertFalse(sharers.contains(9 to Track.ARABIC))
        // The file is never shared across tracks.
        assertEquals(113, sharers.count { it.second == Track.ARABIC })
    }

    @Test
    fun `a verse or intro file belongs to exactly one pair`() {
        assertEquals(listOf(2 to Track.ARABIC), DownloadAggregation.pairsContaining("ar/8"))
        assertEquals(listOf(2 to Track.BANGLA), DownloadAggregation.pairsContaining("bn/intro/2"))
        assertEquals(emptyList<Pair<Int, Track>>(), DownloadAggregation.pairsContaining("unknown/1"))
    }
}

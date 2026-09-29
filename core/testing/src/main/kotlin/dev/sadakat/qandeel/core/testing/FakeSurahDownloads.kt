package dev.sadakat.qandeel.core.testing

import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState
import dev.sadakat.qandeel.core.domain.repository.SurahDownloads
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** [SurahDownloads] whose state tests drive with [setState]; records requests. */
class FakeSurahDownloads : SurahDownloads {

    override val states = MutableStateFlow<Map<Int, Map<Track, SurahDownloadState>>>(emptyMap())

    val downloadRequests = mutableListOf<Pair<Int, List<Track>>>()
    val removeRequests = mutableListOf<Pair<Int, List<Track>>>()

    override fun download(surah: Int, tracks: List<Track>) {
        downloadRequests += surah to tracks
    }

    override fun remove(surah: Int, tracks: List<Track>) {
        removeRequests += surah to tracks
    }

    fun setState(surah: Int, track: Track, state: SurahDownloadState) {
        states.update { all -> all + (surah to (all[surah].orEmpty() + (track to state))) }
    }
}

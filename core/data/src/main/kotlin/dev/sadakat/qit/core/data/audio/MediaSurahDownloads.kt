package dev.sadakat.qit.core.data.audio

import android.content.Context
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import kotlinx.coroutines.flow.StateFlow

/** [SurahDownloads] backed by [QuranCache.downloadManager] and [QuranDownloadService]. */
class MediaSurahDownloads(
    private val context: Context,
    private val quranCache: QuranCache
) : SurahDownloads {

    override val states: StateFlow<Map<Int, Map<Track, SurahDownloadState>>>
        get() = TODO("W2a")

    override fun download(surah: Int, tracks: List<Track>) {
        TODO("W2a")
    }

    override fun remove(surah: Int, tracks: List<Track>) {
        TODO("W2a")
    }
}

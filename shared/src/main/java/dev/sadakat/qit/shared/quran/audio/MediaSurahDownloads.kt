package dev.sadakat.qit.shared.quran.audio

import android.content.Context
import dev.sadakat.qit.shared.quran.model.Track
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

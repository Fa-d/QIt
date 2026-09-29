package dev.sadakat.qandeel.core.domain.repository

import dev.sadakat.qandeel.core.domain.audio.QuranAudioUrls
import dev.sadakat.qandeel.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

sealed interface SurahDownloadState {
    data object NotDownloaded : SurahDownloadState

    data class Downloading(val completedFiles: Int, val totalFiles: Int) : SurahDownloadState {
        val progress: Float get() = if (totalFiles == 0) 0f else completedFiles.toFloat() / totalFiles
    }

    data object Downloaded : SurahDownloadState

    /** Some files failed for good; [completedFiles] of [totalFiles] are on disk. Downloading again retries. */
    data class Failed(val completedFiles: Int, val totalFiles: Int) : SurahDownloadState
}

/** Downloaded audio, per surah and track. Files are those of [QuranAudioUrls.surahFiles]. */
interface SurahDownloads {

    /** surah -> track -> state. Surah/track pairs never requested are absent (= [SurahDownloadState.NotDownloaded]). */
    val states: StateFlow<Map<Int, Map<Track, SurahDownloadState>>>

    /**
     * Queues every file of [surah] for each of [tracks]. Already downloaded files are skipped.
     * Safe to call from any thread.
     */
    fun download(surah: Int, tracks: List<Track>)

    /** Cancels and deletes [surah]'s files for [tracks]. */
    fun remove(surah: Int, tracks: List<Track>)
}

fun Map<Int, Map<Track, SurahDownloadState>>.stateOf(surah: Int, track: Track): SurahDownloadState =
    this[surah]?.get(track) ?: SurahDownloadState.NotDownloaded

/** Combined state of several tracks of one surah, e.g. all tracks of a recitation mode. */
fun Map<Int, Map<Track, SurahDownloadState>>.stateOf(surah: Int, tracks: List<Track>): SurahDownloadState {
    val states = tracks.map { stateOf(surah, it) }
    if (states.all { it is SurahDownloadState.Downloaded }) return SurahDownloadState.Downloaded
    if (states.all { it is SurahDownloadState.NotDownloaded }) return SurahDownloadState.NotDownloaded
    var done = 0
    var total = 0
    for (state in states) {
        when (state) {
            is SurahDownloadState.Downloading -> {
                done += state.completedFiles
                total += state.totalFiles
            }

            is SurahDownloadState.Failed -> {
                done += state.completedFiles
                total += state.totalFiles
            }

            else -> {}
        }
    }
    return when {
        states.any { it is SurahDownloadState.Downloading } -> SurahDownloadState.Downloading(done, total)

        states.any { it is SurahDownloadState.Failed } -> SurahDownloadState.Failed(done, total)

        // Some tracks downloaded, others never requested: not fully available offline.
        else -> SurahDownloadState.NotDownloaded
    }
}

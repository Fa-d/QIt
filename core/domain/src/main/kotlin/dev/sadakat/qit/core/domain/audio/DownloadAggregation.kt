package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.SurahDownloadState

/** State of one downloaded file, independent of the download library. */
enum class FileDownloadState { ACTIVE, COMPLETED, FAILED }

/** Derives per-surah download state from per-file download state. Pure logic. */
object DownloadAggregation {

    /**
     * State of [surah]/[track] given the known files (keyed by [QuranAudioUrls.AudioFile.id]).
     * Null when the pair is not tracked, i.e. none of its *verse* files (basmala excluded) is known.
     */
    fun stateOf(surah: Int, track: Track, files: Map<String, FileDownloadState>): SurahDownloadState? = TODO("W2a")

    /** Every surah/track pair whose files include [fileId] (the shared basmala files belong to many). */
    fun pairsContaining(fileId: String): List<Pair<Int, Track>> = TODO("W2a")
}

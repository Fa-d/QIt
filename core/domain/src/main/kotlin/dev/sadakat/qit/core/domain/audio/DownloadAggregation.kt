package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.repository.SurahDownloadState

/** State of one downloaded file, independent of the download library. */
enum class FileDownloadState { ACTIVE, COMPLETED, FAILED }

/** Derives per-surah download state from per-file download state. Pure logic. */
object DownloadAggregation {

    /** Every file of every surah/track pair; the verse files of each pair are globally unique, only
     * the Arabic/English basmala ("ar/1"/"en/1") is shared across surahs. */
    private val filesByPair: Map<Pair<Int, Track>, List<QuranAudioUrls.AudioFile>> by lazy {
        buildMap {
            for (surah in 1..QuranMeta.SURAH_COUNT) {
                for (track in Track.entries) {
                    put(surah to track, QuranAudioUrls.surahFiles(surah, track))
                }
            }
        }
    }

    /** The ids that prove a pair is tracked: its verse files, basmala excluded. */
    private val verseIdsByPair: Map<Pair<Int, Track>, Set<String>> by lazy {
        filesByPair.mapValues { (pair, files) ->
            val verseIds = files.mapTo(mutableSetOf()) { it.id }
            QuranAudioUrls.basmala(pair.second, pair.first)?.id?.let(verseIds::remove)
            verseIds
        }
    }

    private val pairsByFileId: Map<String, List<Pair<Int, Track>>> by lazy {
        val index = mutableMapOf<String, MutableList<Pair<Int, Track>>>()
        for ((pair, files) in filesByPair) {
            for (file in files) index.getOrPut(file.id) { mutableListOf() }.add(pair)
        }
        index
    }

    /**
     * State of [surah]/[track] given the known files (keyed by [QuranAudioUrls.AudioFile.id]).
     * Null when the pair is not tracked, i.e. none of its *verse* files (basmala excluded) is known.
     */
    fun stateOf(surah: Int, track: Track, files: Map<String, FileDownloadState>): SurahDownloadState? {
        val pairFiles = filesByPair[surah to track] ?: return null
        val verseIds = verseIdsByPair.getValue(surah to track)
        if (files.keys.none { it in verseIds }) return null
        var completed = 0
        var active = false
        for (file in pairFiles) {
            when (files[file.id]) {
                FileDownloadState.COMPLETED -> completed++
                FileDownloadState.ACTIVE -> active = true
                FileDownloadState.FAILED -> {}
                null -> {} // A missing file counts as not completed.
            }
        }
        return when {
            completed == pairFiles.size -> SurahDownloadState.Downloaded
            active -> SurahDownloadState.Downloading(completed, pairFiles.size)
            else -> SurahDownloadState.Failed(completed, pairFiles.size)
        }
    }

    /** Every surah/track pair whose files include [fileId] (the shared basmala files belong to many). */
    fun pairsContaining(fileId: String): List<Pair<Int, Track>> = pairsByFileId[fileId].orEmpty()
}

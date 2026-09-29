package dev.sadakat.qandeel.core.domain.audio

import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState

/** State of one downloaded file, independent of the download library. */
enum class FileDownloadState { ACTIVE, COMPLETED, FAILED }

/** A batch of downloads as a whole: the [surahs] it covers and how much of their files is on disk, 0..1. */
data class DownloadBatch(val surahs: List<Int>, val progress: Float)

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

    /**
     * The ids that prove a pair is tracked: files that belong to this pair only. That excludes the
     * shared basmala ("ar/1" is also Al-Fatiha's first verse), so downloading Al-Baqarah does not make
     * Al-Fatiha look half-downloaded.
     */
    private val ownIdsByPair: Map<Pair<Int, Track>, Set<String>> by lazy {
        filesByPair.mapValues { (_, files) ->
            files.mapNotNullTo(mutableSetOf()) { file -> file.id.takeIf { pairsByFileId.getValue(it).size == 1 } }
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
     * Null when the pair is not tracked, i.e. none of the files only it uses is known.
     */
    fun stateOf(surah: Int, track: Track, files: Map<String, FileDownloadState>): SurahDownloadState? {
        val pairFiles = filesByPair[surah to track] ?: return null
        val ownIds = ownIdsByPair.getValue(surah to track)
        if (files.keys.none { it in ownIds }) return null
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

    /**
     * Progress of the downloads still running, as a whole. [active] holds each queued or downloading
     * file (by id) with the share of it already downloaded (0..1). Every surah/track pair with one of
     * its own files active counts with all its files, so the progress is that of whole surahs: files
     * downloaded earlier count as done, and it never jumps back when the next file starts.
     */
    fun batchProgress(active: Map<String, Float>): DownloadBatch {
        val pairs = active.keys.flatMapTo(sortedSetOf(compareBy({ it.first }, { it.second }))) { id ->
            pairsContaining(id).filter { id in ownIdsByPair.getValue(it) }
        }
        if (pairs.isEmpty()) return DownloadBatch(emptyList(), 1f)
        var total = 0
        var pending = 0f
        for (pair in pairs) {
            val files = filesByPair.getValue(pair)
            total += files.size
            for (file in files) active[file.id]?.let { pending += 1f - it.coerceIn(0f, 1f) }
        }
        return DownloadBatch(pairs.map { it.first }.distinct(), ((total - pending) / total).coerceIn(0f, 1f))
    }

    /** Every surah/track pair whose files include [fileId] (the shared basmala files belong to many). */
    fun pairsContaining(fileId: String): List<Pair<Int, Track>> = pairsByFileId[fileId].orEmpty()
}

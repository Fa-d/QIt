package dev.sadakat.qandeel.core.testing

import dev.sadakat.qandeel.core.domain.audio.WordTimings
import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.repository.AudioTimings

/**
 * [AudioTimings] from maps tests fill in: [words] by surah, then ayah (the same for every reciter
 * unless [wordsByReciter] has the reciter); [durations] by file id, with [defaultDurationMs] for any
 * other file (null = unknown).
 */
class FakeAudioTimings(
    var words: Map<Int, Map<Int, WordTimings>> = emptyMap(),
    var durations: Map<String, Long> = emptyMap(),
    var defaultDurationMs: Long? = null,
    var wordsByReciter: Map<Track, Map<Int, Map<Int, WordTimings>>> = emptyMap(),
) : AudioTimings {

    override suspend fun wordTimings(surah: Int, reciter: Track): Map<Int, WordTimings> =
        (wordsByReciter[reciter] ?: words)[surah].orEmpty()

    override suspend fun durationMs(fileId: String): Long? = durations[fileId] ?: defaultDurationMs
}

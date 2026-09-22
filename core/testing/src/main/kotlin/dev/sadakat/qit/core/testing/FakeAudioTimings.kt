package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.audio.WordTimings
import dev.sadakat.qit.core.domain.repository.AudioTimings

/**
 * [AudioTimings] from maps tests fill in: [words] by surah, then ayah; [durations] by file id, with
 * [defaultDurationMs] for any other file (null = unknown).
 */
class FakeAudioTimings(
    var words: Map<Int, Map<Int, WordTimings>> = emptyMap(),
    var durations: Map<String, Long> = emptyMap(),
    var defaultDurationMs: Long? = null,
) : AudioTimings {

    override suspend fun wordTimings(surah: Int): Map<Int, WordTimings> = words[surah].orEmpty()

    override suspend fun durationMs(fileId: String): Long? = durations[fileId] ?: defaultDurationMs
}

package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.Track

/**
 * Verses whose translation a recording reads together with the next verse's, where the two couldn't
 * be told apart: they have no file of their own, and the next verse's file holds both. Keyed by track;
 * each value lists global ayah numbers as comma-separated numbers and `first-last` runs.
 */
internal object SharedTranslations {

    private val shared: Map<Track, Set<Int>> by lazy { SHARED_TRANSLATIONS.mapValues { (_, ranges) -> parse(ranges) } }

    fun isShared(track: Track, globalAyah: Int): Boolean = shared[track]?.contains(globalAyah) == true

    /** "3-5,9" → {3, 4, 5, 9}; empty for "". */
    fun parse(ranges: String): Set<Int> = ranges.split(',').filter { it.isNotBlank() }.flatMapTo(HashSet()) { part ->
        val bounds = part.trim().split('-').map { it.trim().toInt() }
        require(bounds.size in 1..2 && bounds.first() <= bounds.last()) { "Bad range $part" }
        bounds.first()..bounds.last()
    }
}

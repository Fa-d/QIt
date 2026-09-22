package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.repository.ListeningCounts
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import dev.sadakat.qit.core.domain.repository.ListeningSnapshot
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [ListeningHistory] of a single device: records into [counts], which tests can also set
 * directly; imports are recorded in [imports] and folded into the counts.
 */
class FakeListeningHistory(initial: ListeningCounts = ListeningCounts.EMPTY) : ListeningHistory {

    override val counts = MutableStateFlow(initial)
    override val lastResetAt = MutableStateFlow(0L)

    val imports = mutableListOf<Pair<String, ListeningSnapshot>>()

    override suspend fun recordHeard(ref: AyahRef, atMs: Long) {
        val current = counts.value
        val global = QuranMeta.globalAyah(ref.surah, ref.ayah)
        val array = IntArray(QuranMeta.TOTAL_AYAHS) { current.count(it + 1) }
        array[global - 1]++
        counts.value = ListeningCounts(array, current.lastHeardAt + (ref.surah to atMs), current.listenedMs)
    }

    override suspend fun addListeningTime(surah: Int, ms: Long) {
        val current = counts.value
        val listened = current.listenedMs + (surah to (current.listenedMs[surah] ?: 0L) + ms)
        counts.value =
            ListeningCounts(IntArray(QuranMeta.TOTAL_AYAHS) { current.count(it + 1) }, current.lastHeardAt, listened)
    }

    override suspend fun localSnapshot(): ListeningSnapshot {
        val current = counts.value
        return ListeningSnapshot(
            resetAt = lastResetAt.value,
            ayahCounts = (1..QuranMeta.TOTAL_AYAHS).filter {
                current.count(it) > 0
            }.associateWith { current.count(it) },
            lastHeardAt = current.lastHeardAt,
            listenedMs = current.listenedMs,
        )
    }

    override suspend fun importSnapshot(source: String, snapshot: ListeningSnapshot) {
        imports += source to snapshot
    }

    override suspend fun reset(atMs: Long) {
        lastResetAt.value = atMs
        counts.value = ListeningCounts.EMPTY
    }
}

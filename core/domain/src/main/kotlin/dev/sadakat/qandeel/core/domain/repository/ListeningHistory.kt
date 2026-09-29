package dev.sadakat.qandeel.core.domain.repository

import dev.sadakat.qandeel.core.domain.model.AyahRef
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import kotlinx.coroutines.flow.Flow

/**
 * Everything heard so far: how many times each ayah was heard, when each surah was last heard, and
 * how long each surah was listened to.
 */
class ListeningCounts(
    /** Times each ayah was heard, by global ayah - 1; ayahs past its end count 0. */
    ayahCounts: IntArray = IntArray(0),
    /** When each surah was last heard (epoch ms); surahs never heard are absent. */
    val lastHeardAt: Map<Int, Long> = emptyMap(),
    /** Listening time per surah (ms); surahs never listened to are absent. */
    val listenedMs: Map<Int, Long> = emptyMap(),
) {
    private val counts = ayahCounts.copyOf(QuranMeta.TOTAL_AYAHS)

    /** Times global ayah [globalAyah] (1..6236) was heard. */
    fun count(globalAyah: Int): Int = counts[globalAyah - 1]

    /** Times [surah]:[ayah] was heard. */
    fun count(surah: Int, ayah: Int): Int = count(QuranMeta.globalAyah(surah, ayah))

    override fun equals(other: Any?): Boolean = other is ListeningCounts &&
        counts.contentEquals(other.counts) &&
        lastHeardAt == other.lastHeardAt &&
        listenedMs == other.listenedMs

    override fun hashCode(): Int = (counts.contentHashCode() * 31 + lastHeardAt.hashCode()) * 31 + listenedMs.hashCode()

    override fun toString(): String = "ListeningCounts(heard=${counts.count {
        it > 0
    }} ayahs, surahs=${lastHeardAt.keys})"

    companion object {
        val EMPTY = ListeningCounts()
    }
}

/**
 * What one device heard itself, to hand to another (the watch to the phone). Counts from
 * [resetAt] on: a snapshot taken before the receiver's last reset is stale.
 */
data class ListeningSnapshot(
    /** The reset this snapshot counts from (epoch ms); 0 if the history was never reset. */
    val resetAt: Long,
    /** Times heard by global ayah; ayahs never heard are absent. */
    val ayahCounts: Map<Int, Int>,
    /** When each surah was last heard (epoch ms). */
    val lastHeardAt: Map<Int, Long>,
    /** Listening time per surah (ms). */
    val listenedMs: Map<Int, Long>,
)

/** The listening record, kept on each device; the phone's also holds what its watches heard. */
interface ListeningHistory {

    /** Everything heard, here and on synced devices; updates as listening goes on. */
    val counts: Flow<ListeningCounts>

    /** When the history was last reset (epoch ms), 0 if never. */
    val lastResetAt: Flow<Long>

    /** Counts [ref] as heard once more, at [atMs] (epoch ms). */
    suspend fun recordHeard(ref: AyahRef, atMs: Long)

    /** Adds [ms] of listening time to [surah]. */
    suspend fun addListeningTime(surah: Int, ms: Long)

    /** What this device heard itself. */
    suspend fun localSnapshot(): ListeningSnapshot

    /**
     * Replaces what [source] (another device) heard with [snapshot]. Ignored when the snapshot counts
     * from before this history's last reset.
     */
    suspend fun importSnapshot(source: String, snapshot: ListeningSnapshot)

    /** Forgets everything heard, from every source, as of [atMs] (epoch ms). */
    suspend fun reset(atMs: Long)
}

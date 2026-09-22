package dev.sadakat.qit.core.domain.audio

/** A point in a queue: the item at [index], [positionInItemMs] into it. */
data class TimelinePoint(val index: Int, val positionInItemMs: Long)

/**
 * A surah's queue laid end to end, from the lengths of its items (in queue order): a position in
 * one item is a position in the whole surah, and back. Lets the player show and seek the surah as
 * one recording although every ayah (and its translation) is a file of its own.
 */
class SurahTimeline(itemDurationsMs: List<Long>) {

    private val durations = itemDurationsMs.map { it.coerceAtLeast(0) }

    /** Where each item starts in the surah. */
    private val starts = LongArray(durations.size).also { starts ->
        var next = 0L
        for (i in durations.indices) {
            starts[i] = next
            next += durations[i]
        }
    }

    val itemCount: Int get() = durations.size

    /** The whole surah's length. */
    val durationMs: Long = durations.sum()

    /** Where [positionInItemMs] into item [index] lies in the surah, clamped to the item. */
    fun positionOf(index: Int, positionInItemMs: Long): Long {
        if (index !in durations.indices) return if (index < 0) 0 else durationMs
        return starts[index] + positionInItemMs.coerceIn(0, durations[index])
    }

    /**
     * The item playing at [positionMs] into the surah, and how far into it. Clamped to the surah; an
     * item boundary belongs to the item that starts there, and the very end to the last item's end.
     */
    fun locate(positionMs: Long): TimelinePoint {
        require(durations.isNotEmpty()) { "An empty queue has no timeline" }
        val position = positionMs.coerceIn(0, durationMs)
        // The last item starting at or before the position that isn't empty.
        var index = durations.indices.last { starts[it] <= position && (durations[it] > 0 || it == 0) }
        if (position == durationMs) index = durations.indices.last { durations[it] > 0 || it == 0 }
        return TimelinePoint(index, position - starts[index])
    }
}

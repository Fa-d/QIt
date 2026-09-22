package dev.sadakat.qit.core.domain.audio

/**
 * When each word of one ayah's Arabic is recited: word `i` runs from `spans[2i]` to `spans[2i + 1]`,
 * in ms into the ayah's audio file. Words are those of [dev.sadakat.qit.core.domain.model.ArabicWords],
 * and start in order.
 */
class WordTimings(private val spans: IntArray) {

    init {
        require(spans.size % 2 == 0) { "Word timings come in start/end pairs, got ${spans.size} values" }
    }

    val wordCount: Int get() = spans.size / 2

    /**
     * The word being recited at [positionMs]: the last word that has started, so a pause between
     * words keeps the word just recited, and the last word stays once the ayah is recited.
     * -1 before the first word.
     */
    fun wordAt(positionMs: Long): Int {
        var low = 0
        var high = wordCount - 1
        var found = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (spans[mid * 2] <= positionMs) {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }

    /** When word [index] starts, in ms into the audio file. */
    fun startMs(index: Int): Int = spans[index * 2]

    /** When word [index] ends, in ms into the audio file. */
    fun endMs(index: Int): Int = spans[index * 2 + 1]

    override fun equals(other: Any?): Boolean = other is WordTimings && spans.contentEquals(other.spans)

    override fun hashCode(): Int = spans.contentHashCode()

    override fun toString(): String = "WordTimings(${spans.contentToString()})"
}

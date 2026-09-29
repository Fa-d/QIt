package dev.sadakat.qandeel.core.data.audio

/**
 * Which surahs of the running downloads have just finished, and whether all their files made it.
 * Surahs whose download was cancelled (removed) don't count as finished.
 */
internal class SurahFinishes {

    private var running = emptySet<Int>()
    private val failed = mutableSetOf<Int>()
    private val removed = mutableSetOf<Int>()

    /** Some files of [surahs] failed for good. */
    fun onFailed(surahs: Collection<Int>) {
        failed += surahs
    }

    /** [surahs] are being removed: if they were downloading, they were cancelled, not finished. */
    fun onRemoving(surahs: Collection<Int>) {
        removed += surahs
    }

    /**
     * The downloads now cover [active] surahs. Returns the surahs that just finished, each mapped to
     * whether some of its files failed.
     */
    fun update(active: Set<Int>): Map<Int, Boolean> {
        val finished = (running - active).filterNot { it in removed }.associateWith { it in failed }
        running = active
        failed.retainAll(active)
        removed.retainAll(active)
        return finished
    }
}

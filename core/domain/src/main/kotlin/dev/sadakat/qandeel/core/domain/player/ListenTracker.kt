package dev.sadakat.qandeel.core.domain.player

import dev.sadakat.qandeel.core.domain.audio.QueueItemId
import dev.sadakat.qandeel.core.domain.model.AyahRef
import dev.sadakat.qandeel.core.domain.model.Track

/**
 * Decides which ayahs count as heard: an ayah counts once its Arabic recitation plays to its end.
 * Skipping past it, or jumping forward inside it, doesn't count; every play-through does, so each
 * repeat of a memorizing session counts. The basmala before verse 1 is not an ayah and never counts.
 *
 * Pure: the player adapter reports what happens to the queue's current item and asks, when an item
 * finishes, what to record.
 */
class ListenTracker {

    private var current: QueueItemId? = null

    /** Part of the current play-through was jumped over. */
    private var skipped = false

    /** The current play-through has been counted already (a finish is reported twice at most). */
    private var counted = false

    /** [id] became the current item, starting [positionMs] into it; starting in the middle skips its start. */
    fun start(id: QueueItemId?, positionMs: Long) {
        current = id
        skipped = positionMs > START_TOLERANCE_MS
        counted = false
    }

    /**
     * The current item was sought from [fromMs] to [toMs]. Forward skips part of it; back to its
     * start begins a fresh play-through; back to its middle lets the rest play (and count) again.
     */
    fun seek(fromMs: Long, toMs: Long) {
        when {
            toMs <= START_TOLERANCE_MS -> start(current, toMs)
            toMs > fromMs + START_TOLERANCE_MS -> skipped = true
            toMs < fromMs -> counted = false
        }
    }

    /** Item [id] played to its end; returns the ayah to count as heard, or null. */
    fun finish(id: QueueItemId?): AyahRef? {
        if (id == null || id.track != Track.ARABIC || id.ayah < 1) return null
        if (id != current || !isArmed()) return null
        counted = true
        return AyahRef(id.surah, id.ayah)
    }

    /**
     * The current item played to its end. For events that arrive after the player has already moved
     * on (a repeat jumping back), where the player's own current item is no longer the one that ended.
     */
    fun finishCurrent(): AyahRef? = finish(current)

    /** Nothing of the current play-through was skipped, and it hasn't been counted yet. */
    private fun isArmed() = !skipped && !counted

    private companion object {
        /** Positions this close to an item's start count as its start (seek and position rounding). */
        const val START_TOLERANCE_MS = 500L
    }
}

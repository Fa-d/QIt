package dev.sadakat.qandeel.core.domain.player

import dev.sadakat.qandeel.core.domain.audio.QueueItemId
import dev.sadakat.qandeel.core.domain.model.AyahRef
import dev.sadakat.qandeel.core.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListenTrackerTest {

    private val tracker = ListenTracker()
    private val arabic5 = QueueItemId(18, 5, Track.ARABIC)
    private val english5 = QueueItemId(18, 5, Track.ENGLISH)
    private val arabic6 = QueueItemId(18, 6, Track.ARABIC)

    @Test
    fun `an arabic recitation played to its end counts`() {
        tracker.start(arabic5, 0)
        assertEquals(AyahRef(18, 5), tracker.finish(arabic5))
    }

    @Test
    fun `a finish reported twice counts once`() {
        tracker.start(arabic5, 0)
        tracker.finish(arabic5)
        assertNull(tracker.finish(arabic5))
    }

    @Test
    fun `translations and the basmala never count`() {
        tracker.start(english5, 0)
        assertNull(tracker.finish(english5))
        val basmala = QueueItemId(18, 0, Track.ARABIC)
        tracker.start(basmala, 0)
        assertNull(tracker.finish(basmala))
    }

    @Test
    fun `an item skipped before its end does not count`() {
        tracker.start(arabic5, 0)
        tracker.start(arabic6, 0) // "next" pressed
        assertNull(tracker.finish(arabic5))
        assertEquals(AyahRef(18, 6), tracker.finish(arabic6))
    }

    @Test
    fun `a forward jump inside the ayah does not count`() {
        tracker.start(arabic5, 0)
        tracker.seek(fromMs = 1_000, toMs = 9_000)
        assertNull(tracker.finish(arabic5))
    }

    @Test
    fun `starting in the middle of the ayah does not count`() {
        tracker.start(arabic5, 4_000)
        assertNull(tracker.finish(arabic5))
    }

    @Test
    fun `a jump back lets the rest play and count`() {
        tracker.start(arabic5, 0)
        tracker.seek(fromMs = 6_000, toMs = 2_000)
        assertEquals(AyahRef(18, 5), tracker.finish(arabic5))
    }

    @Test
    fun `a jump back after a skip does not undo the skip`() {
        tracker.start(arabic5, 0)
        tracker.seek(fromMs = 1_000, toMs = 9_000)
        tracker.seek(fromMs = 9_000, toMs = 5_000)
        assertNull(tracker.finish(arabic5))
    }

    @Test
    fun `every repeat of the ayah counts`() {
        tracker.start(arabic5, 0)
        var counted = 0
        repeat(3) {
            if (tracker.finish(arabic5) != null) counted++
            tracker.seek(fromMs = 12_000, toMs = 0) // the repeat restarts the same item
        }
        assertEquals(3, counted)
    }

    @Test
    fun `restarting the ayah forgives an earlier skip`() {
        tracker.start(arabic5, 0)
        tracker.seek(fromMs = 1_000, toMs = 9_000)
        tracker.seek(fromMs = 9_000, toMs = 0)
        assertEquals(AyahRef(18, 5), tracker.finish(arabic5))
    }

    @Test
    fun `a small seek near the position is no skip`() {
        tracker.start(arabic5, 0)
        tracker.seek(fromMs = 3_000, toMs = 3_400)
        assertEquals(AyahRef(18, 5), tracker.finish(arabic5))
    }

    @Test
    fun `finishing the current item counts the item the tracker follows`() {
        tracker.start(arabic6, 0)
        assertEquals(AyahRef(18, 6), tracker.finishCurrent())
        assertNull(tracker.finishCurrent())
    }

    @Test
    fun `nothing counts without a current item`() {
        assertNull(tracker.finish(null))
        tracker.start(null, 0)
        assertNull(tracker.finish(arabic5))
    }
}

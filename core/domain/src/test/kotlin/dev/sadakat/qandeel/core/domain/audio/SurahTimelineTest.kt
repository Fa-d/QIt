package dev.sadakat.qandeel.core.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class SurahTimelineTest {

    // Basmala, then two ayahs of Arabic + translation.
    private val timeline = SurahTimeline(listOf(6_000L, 4_000L, 3_000L, 5_000L, 2_000L))

    @Test
    fun `the surah is its items end to end`() {
        assertEquals(20_000L, timeline.durationMs)
        assertEquals(5, timeline.itemCount)
    }

    @Test
    fun `a position in an item is a position in the surah`() {
        assertEquals(0L, timeline.positionOf(0, 0))
        assertEquals(7_500L, timeline.positionOf(1, 1_500))
        assertEquals(20_000L, timeline.positionOf(4, 2_000))
    }

    @Test
    fun `positions outside the item or the queue are clamped`() {
        assertEquals(10_000L, timeline.positionOf(1, 9_999))
        assertEquals(6_000L, timeline.positionOf(1, -5))
        assertEquals(0L, timeline.positionOf(-1, 0))
        assertEquals(20_000L, timeline.positionOf(5, 0))
    }

    @Test
    fun `a surah position is found in its item`() {
        assertEquals(TimelinePoint(0, 0), timeline.locate(0))
        assertEquals(TimelinePoint(1, 1_500), timeline.locate(7_500))
        assertEquals(TimelinePoint(3, 4_999), timeline.locate(17_999))
    }

    @Test
    fun `a boundary belongs to the item starting there`() {
        assertEquals(TimelinePoint(1, 0), timeline.locate(6_000))
    }

    @Test
    fun `the ends of the surah clamp to its first and last item`() {
        assertEquals(TimelinePoint(0, 0), timeline.locate(-100))
        assertEquals(TimelinePoint(4, 2_000), timeline.locate(20_000))
        assertEquals(TimelinePoint(4, 2_000), timeline.locate(99_000))
    }

    @Test
    fun `items of unknown length are skipped`() {
        val gaps = SurahTimeline(listOf(3_000L, 0L, -1L, 2_000L, 0L))
        assertEquals(5_000L, gaps.durationMs)
        assertEquals(TimelinePoint(3, 0), gaps.locate(3_000))
        assertEquals(TimelinePoint(3, 2_000), gaps.locate(5_000))
    }

    @Test
    fun `a queue of unknown lengths stays at its start`() {
        val unknown = SurahTimeline(listOf(0L, 0L))
        assertEquals(0L, unknown.durationMs)
        assertEquals(TimelinePoint(0, 0), unknown.locate(1_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an empty queue has nothing to locate`() {
        SurahTimeline(emptyList()).locate(0)
    }
}

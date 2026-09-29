package dev.sadakat.qandeel.core.domain.audio

import dev.sadakat.qandeel.core.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueItemIdTest {

    @Test
    fun `toMediaId and parse round trip`() {
        for (track in Track.entries) {
            val id = QueueItemId(surah = 2, ayah = 255, track = track)

            assertEquals(id, QueueItemId.parse(id.toMediaId()))
        }
    }

    @Test
    fun `toMediaId formats surah ayah and track code`() {
        assertEquals("2:0:ar", QueueItemId(2, 0, Track.ARABIC).toMediaId())
        assertEquals("112:4:en", QueueItemId(112, 4, Track.ENGLISH).toMediaId())
    }

    @Test
    fun `parse rejects anything that is not a queue item`() {
        assertNull(QueueItemId.parse(null))
        assertNull(QueueItemId.parse(""))
        assertNull(QueueItemId.parse("garbage"))
        assertNull(QueueItemId.parse("2:255")) // wrong part count
        assertNull(QueueItemId.parse("2:255:ar:extra"))
        assertNull(QueueItemId.parse("2:255:xx")) // unknown track
        assertNull(QueueItemId.parse("two:255:ar")) // not numeric
    }
}

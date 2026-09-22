package dev.sadakat.qit.core.data.link

import dev.sadakat.qit.core.domain.repository.ListeningSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningMessagesTest {

    @Test
    fun `a snapshot survives the trip`() {
        val snapshot = ListeningSnapshot(
            resetAt = 1_000,
            ayahCounts = mapOf(262 to 12, 1 to 1),
            lastHeardAt = mapOf(2 to 1_700_000_000_000L),
            listenedMs = mapOf(2 to 90_000L),
        )

        val received = ListeningSnapshotMessage.fromBytes(ListeningSnapshotMessage.of(snapshot).toBytes())

        assertEquals(snapshot, received.toSnapshot())
    }

    @Test
    fun `a reset survives the trip`() {
        assertEquals(ListeningResetMessage(42), ListeningResetMessage.fromBytes(ListeningResetMessage(42).toBytes()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `garbage is no snapshot`() {
        ListeningSnapshotMessage.fromBytes("{".encodeToByteArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `garbage is no reset`() {
        ListeningResetMessage.fromBytes("[]".encodeToByteArray())
    }
}

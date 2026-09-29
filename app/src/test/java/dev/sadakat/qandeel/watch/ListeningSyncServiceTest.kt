package dev.sadakat.qandeel.watch

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qandeel.core.data.link.ListeningSnapshotMessage
import dev.sadakat.qandeel.core.domain.repository.ListeningSnapshot
import dev.sadakat.qandeel.core.testing.FakeListeningHistory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListeningSyncServiceTest {

    private val history = FakeListeningHistory()

    @Test
    fun `a watch's snapshot is imported under the watch's own name`() = runTest {
        val snapshot = ListeningSnapshot(
            resetAt = 1_000,
            ayahCounts = mapOf(7 to 2),
            lastHeardAt = mapOf(1 to 1_700_000_000_000L),
            listenedMs = mapOf(1 to 90_000L),
        )

        importListeningSnapshot("node1", ListeningSnapshotMessage.of(snapshot).toBytes(), history)

        assertEquals(listOf("watch:node1" to snapshot), history.imports)
    }

    @Test
    fun `garbage bytes never crash and never import`() = runTest {
        importListeningSnapshot("node1", "not json at all".encodeToByteArray(), history)

        assertTrue(history.imports.isEmpty())
    }
}

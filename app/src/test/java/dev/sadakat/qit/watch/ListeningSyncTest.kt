package dev.sadakat.qit.watch

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.data.link.ListeningSnapshotMessage
import dev.sadakat.qit.core.domain.repository.ListeningSnapshot
import dev.sadakat.qit.core.testing.FakeListeningHistory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class) // Garbage items are logged through android.util.Log.
class ListeningSyncTest {

    private val history = FakeListeningHistory()
    private val channel = RecordingChannel()
    private val sync = ListeningSync(history, channel)

    private fun snapshot(resetAt: Long) = ListeningSnapshotMessage.of(
        ListeningSnapshot(resetAt, ayahCounts = mapOf(1 to 1), lastHeardAt = emptyMap(), listenedMs = emptyMap()),
    ).toBytes()

    @Test
    fun `on start the phone imports what each watch heard, under the watch's own name`() = runTest {
        channel.snapshots = listOf("node1" to snapshot(0), "node2" to snapshot(0))

        sync.start(backgroundScope)
        runCurrent()

        assertEquals(listOf("watch:node1", "watch:node2"), history.imports.map { it.first })
    }

    @Test
    fun `a watch's garbage item is dropped without stopping the others`() = runTest {
        channel.snapshots = listOf("node1" to "not json at all".encodeToByteArray(), "node2" to snapshot(0))

        sync.start(backgroundScope)
        runCurrent()

        assertEquals(listOf("watch:node2"), history.imports.map { it.first })
    }

    @Test
    fun `a reset is told to the watches once, and only when there was one`() = runTest {
        sync.start(backgroundScope)
        runCurrent()
        assertTrue(channel.resets.isEmpty()) // Never reset: nothing to tell.

        history.lastResetAt.value = 1_000
        runCurrent()
        assertEquals(listOf(1_000L), channel.resets)

        history.lastResetAt.value = 1_000 // The same reset again: no second item.
        runCurrent()
        assertEquals(listOf(1_000L), channel.resets)

        history.lastResetAt.value = 2_000
        runCurrent()
        assertEquals(listOf(1_000L, 2_000L), channel.resets)
    }

    @Test
    fun `a reset from before the app started is still told to the watches`() = runTest {
        history.lastResetAt.value = 5_000

        sync.start(backgroundScope)
        runCurrent()

        assertEquals(listOf(5_000L), channel.resets)
    }

    /** [ListeningSyncChannel] test double: scripted catch-up items, records published resets. */
    private class RecordingChannel : ListeningSyncChannel {

        var snapshots = emptyList<Pair<String?, ByteArray>>()
        val resets = mutableListOf<Long>()

        override suspend fun listeningSnapshots(): List<Pair<String?, ByteArray>> = snapshots

        override suspend fun publishReset(resetAt: Long) {
            resets += resetAt
        }
    }
}

package dev.sadakat.qit.wear.sync

import dev.sadakat.qit.core.data.link.ListeningSnapshotMessage
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.testing.FakeListeningHistory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListeningPublisherTest {

    private val history = FakeListeningHistory()
    private val published = mutableListOf<ByteArray>()
    private val publisher = ListeningPublisher(history) { published += it }

    @Test
    fun `what the watch heard goes out once at start, and again once things settle`() = runTest {
        history.recordHeard(AyahRef(1, 1), atMs = 1_000)
        publisher.start(backgroundScope)
        runCurrent()
        assertEquals(1, published.size)

        history.recordHeard(AyahRef(1, 2), atMs = 2_000)
        history.recordHeard(AyahRef(1, 3), atMs = 3_000)
        runCurrent()
        assertEquals(1, published.size) // Still settling.
        advanceTimeBy(10_001)

        assertEquals(2, published.size)
        val message = ListeningSnapshotMessage.fromBytes(published.last())
        assertEquals(mapOf(1 to 1, 2 to 1, 3 to 1), message.counts)
    }

    @Test
    fun `the payload counts from the watch's own reset`() = runTest {
        history.lastResetAt.value = 1_234
        publisher.start(backgroundScope)
        runCurrent()

        val message = ListeningSnapshotMessage.fromBytes(published.single())
        assertEquals(1_234, message.resetAt)
    }

    @Test
    fun `a reset that empties the counts is published too`() = runTest {
        history.recordHeard(AyahRef(1, 1), atMs = 1_000)
        publisher.start(backgroundScope)
        runCurrent()
        assertEquals(1, published.size)

        history.reset(atMs = 2_000)
        advanceTimeBy(10_001)

        assertEquals(2, published.size)
        val message = ListeningSnapshotMessage.fromBytes(published.last())
        assertEquals(2_000, message.resetAt)
        assertEquals(emptyMap<Int, Int>(), message.counts)
    }
}

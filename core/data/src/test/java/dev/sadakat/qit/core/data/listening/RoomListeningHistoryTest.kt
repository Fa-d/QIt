package dev.sadakat.qit.core.data.listening

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.repository.ListeningSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomListeningHistoryTest {

    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        QuranDatabase::class.java,
    ).allowMainThreadQueries().build()
    private val history = RoomListeningHistory(database)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `nothing heard at first`() = runTest {
        val counts = history.counts.first()
        assertEquals(0, counts.count(1))
        assertEquals(emptyMap<Int, Long>(), counts.lastHeardAt)
        assertEquals(0L, history.lastResetAt.first())
    }

    @Test
    fun `each hearing adds one and remembers when`() = runTest {
        history.recordHeard(AyahRef(2, 255), atMs = 100)
        history.recordHeard(AyahRef(2, 255), atMs = 300)
        history.recordHeard(AyahRef(2, 256), atMs = 200)

        val counts = history.counts.first()
        assertEquals(2, counts.count(2, 255))
        assertEquals(1, counts.count(2, 256))
        assertEquals(mapOf(2 to 300L), counts.lastHeardAt)
    }

    @Test
    fun `listening time adds up per surah`() = runTest {
        history.addListeningTime(18, 60_000)
        history.addListeningTime(18, 30_000)
        history.addListeningTime(36, 0) // nothing to add

        val counts = history.counts.first()
        assertEquals(mapOf(18 to 90_000L), counts.listenedMs)
        assertEquals(emptyMap<Int, Long>(), counts.lastHeardAt) // time alone isn't hearing an ayah
    }

    @Test
    fun `a watch's snapshot adds to what the phone heard, and replaces its previous one`() = runTest {
        history.recordHeard(AyahRef(1, 1), atMs = 100)
        history.importSnapshot("watch:a", snapshot(counts = mapOf(1 to 2, 8 to 1), lastHeard = mapOf(1 to 500L)))
        history.importSnapshot("watch:a", snapshot(counts = mapOf(1 to 3), lastHeard = mapOf(1 to 700L)))
        history.importSnapshot("watch:b", snapshot(counts = mapOf(1 to 1), ms = mapOf(1 to 5_000L)))

        val counts = history.counts.first()
        assertEquals(1 + 3 + 1, counts.count(1))
        assertEquals(0, counts.count(8)) // watch a's newer snapshot no longer has it
        assertEquals(mapOf(1 to 700L), counts.lastHeardAt)
        assertEquals(mapOf(1 to 5_000L), counts.listenedMs)
    }

    @Test
    fun `the local snapshot holds only what this device heard`() = runTest {
        history.recordHeard(AyahRef(1, 2), atMs = 100)
        history.addListeningTime(1, 4_000)
        history.importSnapshot("watch:a", snapshot(counts = mapOf(3 to 9)))

        val expected = ListeningSnapshot(
            resetAt = 0,
            ayahCounts = mapOf(2 to 1),
            lastHeardAt = mapOf(1 to 100L),
            listenedMs = mapOf(1 to 4_000L),
        )
        assertEquals(expected, history.localSnapshot())
    }

    @Test
    fun `a reset forgets every source and ignores snapshots from before it`() = runTest {
        history.recordHeard(AyahRef(1, 1), atMs = 100)
        history.importSnapshot("watch:a", snapshot(counts = mapOf(2 to 1)))

        history.reset(atMs = 1_000)
        // Sent before the reset reached the watch:
        history.importSnapshot("watch:a", snapshot(counts = mapOf(2 to 1), resetAt = 0))
        history.importSnapshot("watch:b", snapshot(counts = mapOf(3 to 1), resetAt = 1_000))

        val counts = history.counts.first()
        assertEquals(listOf(0, 0, 1), (1..3).map(counts::count))
        assertEquals(1_000L, history.lastResetAt.first())
        assertEquals(1_000L, history.localSnapshot().resetAt)
    }

    @Test
    fun `nonsense in a snapshot is dropped`() = runTest {
        val nonsense = snapshot(counts = mapOf(0 to 1, 7_000 to 1, 5 to 0, 6 to 2), lastHeard = mapOf(200 to 1L))
        history.importSnapshot("watch:a", nonsense)

        val counts = history.counts.first()
        assertEquals(2, counts.count(6))
        assertEquals(0, counts.count(5))
        assertEquals(emptyMap<Int, Long>(), counts.lastHeardAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `another device can't overwrite this one`() = runTest {
        history.importSnapshot(RoomListeningHistory.LOCAL, snapshot(counts = mapOf(1 to 1)))
    }

    private fun snapshot(
        counts: Map<Int, Int>,
        lastHeard: Map<Int, Long> = emptyMap(),
        ms: Map<Int, Long> = emptyMap(),
        resetAt: Long = 0,
    ) = ListeningSnapshot(resetAt, counts, lastHeard, ms)
}

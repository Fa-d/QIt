package dev.sadakat.qit.wear.tile

import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.testing.FakeQuranPlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TileRefresherTest {

    private val player = FakeQuranPlayer()
    private var updates = 0
    private val refresher = TileRefresher(player) { updates++ }

    private fun playing(ayah: Int, isPlaying: Boolean = true) =
        NowPlaying(18, ayah, Track.ARABIC, RecitationMode.ARABIC_ONLY, isPlaying, isBuffering = false)

    @Test
    fun `the tile is redrawn when the ayah or the play state changes, once things settle`() = runTest {
        refresher.start(backgroundScope)
        runCurrent()

        player.nowPlaying.value = playing(ayah = 10)
        player.nowPlaying.value = playing(ayah = 11)
        advanceTimeBy(1_001)
        assertEquals(1, updates)

        player.nowPlaying.value = playing(ayah = 11, isPlaying = false)
        advanceTimeBy(1_001)
        assertEquals(2, updates)
    }

    @Test
    fun `changes the tile doesn't show don't redraw it`() = runTest {
        player.nowPlaying.value = playing(ayah = 10)
        refresher.start(backgroundScope)
        runCurrent()

        // Buffering and the track within an ayah aren't on the tile.
        player.nowPlaying.value = playing(ayah = 10).copy(isBuffering = true, track = Track.ENGLISH)
        advanceTimeBy(5_000)

        assertEquals(0, updates)
    }
}

package dev.sadakat.qit.wear.tile

import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.repository.LastPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class TileStateTest {

    private val names: (Int) -> String = { "Surah $it" }
    private val saved = LastPosition(AyahRef(2, 255), RecitationMode.ARABIC_ENGLISH)

    @Test
    fun `nothing played yet`() {
        assertEquals(TileState.NothingYet, tileStateOf(nowPlaying = null, lastPosition = null, surahName = names))
    }

    @Test
    fun `after a restart the saved position offers to continue`() {
        assertEquals(
            TileState.Continue("Surah 2", AyahRef(2, 255), ayahCount = 286),
            tileStateOf(nowPlaying = null, lastPosition = saved, surahName = names),
        )
    }

    @Test
    fun `what's queued wins over the saved position`() {
        val nowPlaying =
            NowPlaying(18, 10, Track.ARABIC, RecitationMode.ARABIC_ONLY, isPlaying = true, isBuffering = false)

        assertEquals(
            TileState.Queued("Surah 18", AyahRef(18, 10), ayahCount = 110, isPlaying = true),
            tileStateOf(nowPlaying, saved, names),
        )
    }
}

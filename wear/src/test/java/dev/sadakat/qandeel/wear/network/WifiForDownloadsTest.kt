package dev.sadakat.qandeel.wear.network

import dev.sadakat.qandeel.core.domain.model.Track
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState
import org.junit.Assert.assertEquals
import org.junit.Test

class WifiForDownloadsTest {

    private class RecordingOps : NetworkOps {
        val calls = mutableListOf<String>()
        override fun acquireWifi() {
            calls += "acquire"
        }

        override fun releaseWifi() {
            calls += "release"
        }
    }

    /** surah -> track -> state, the shape of [dev.sadakat.qandeel.core.domain.repository.SurahDownloads.states]. */
    private fun statesOf(vararg entries: Pair<Pair<Int, Track>, SurahDownloadState>) =
        entries.groupBy({ it.first.first }, { it.first.second to it.second })
            .mapValues { (_, pairs) -> pairs.toMap() }

    private val machine = WifiRequestStateMachine(RecordingOps())

    private val ops: RecordingOps
        get() = machine.networkOps as RecordingOps

    @Test
    fun `no request while nothing is downloading`() {
        machine.onStates(statesOf())
        machine.onStates(
            statesOf(
                (2 to Track.ARABIC) to SurahDownloadState.Downloaded,
                (3 to Track.ENGLISH) to SurahDownloadState.Failed(1, 200),
            ),
        )

        assertEquals(emptyList<String>(), ops.calls)
    }

    @Test
    fun `requests wifi once when downloads start`() {
        machine.onStates(statesOf())
        machine.onStates(statesOf((2 to Track.ARABIC) to SurahDownloadState.Downloading(0, 286)))

        assertEquals(listOf("acquire"), ops.calls)
    }

    @Test
    fun `progress updates do not churn the request`() {
        machine.onStates(statesOf((2 to Track.ARABIC) to SurahDownloadState.Downloading(1, 286)))
        machine.onStates(statesOf((2 to Track.ARABIC) to SurahDownloadState.Downloading(2, 286)))
        machine.onStates(statesOf((2 to Track.ARABIC) to SurahDownloadState.Downloading(3, 286)))

        assertEquals(listOf("acquire"), ops.calls)
    }

    @Test
    fun `stays on wifi until every surah finished downloading`() {
        machine.onStates(
            statesOf(
                (2 to Track.ARABIC) to SurahDownloadState.Downloading(1, 286),
                (112 to Track.ARABIC) to SurahDownloadState.Downloading(0, 4),
            ),
        )
        machine.onStates(
            statesOf(
                (2 to Track.ARABIC) to SurahDownloadState.Downloaded,
                (112 to Track.ARABIC) to SurahDownloadState.Downloading(2, 4),
            ),
        )
        assertEquals(listOf("acquire"), ops.calls)

        machine.onStates(
            statesOf(
                (2 to Track.ARABIC) to SurahDownloadState.Downloaded,
                (112 to Track.ARABIC) to SurahDownloadState.Failed(3, 4),
            ),
        )
        assertEquals(listOf("acquire", "release"), ops.calls)
    }

    @Test
    fun `re-acquires for a later download`() {
        machine.onStates(statesOf((2 to Track.ARABIC) to SurahDownloadState.Downloading(1, 286)))
        machine.onStates(statesOf((2 to Track.ARABIC) to SurahDownloadState.Downloaded))
        machine.onStates(statesOf((3 to Track.ARABIC) to SurahDownloadState.Downloading(0, 200)))

        assertEquals(listOf("acquire", "release", "acquire"), ops.calls)
    }
}

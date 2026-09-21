package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.QuranPlayer
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [QuranPlayer] that records commands and simulates the resulting state: [play] starts at the
 * requested ayah, [nextAyah]/[previousAyah] move by one, [togglePlayPause] flips `isPlaying`.
 */
class FakeQuranPlayer : QuranPlayer {

    data class PlayCall(val surah: Int, val fromAyah: Int, val mode: RecitationMode)

    override val nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val error = MutableStateFlow<String?>(null)

    val playCalls = mutableListOf<PlayCall>()
    var restoreCalls = 0
        private set

    override fun play(surah: Int, fromAyah: Int, mode: RecitationMode) {
        playCalls += PlayCall(surah, fromAyah, mode)
        nowPlaying.value = NowPlaying(surah, fromAyah, Track.ARABIC, mode, isPlaying = true, isBuffering = false)
    }

    override fun togglePlayPause() {
        nowPlaying.value = nowPlaying.value?.let { it.copy(isPlaying = !it.isPlaying) }
    }

    override fun nextAyah() {
        nowPlaying.value = nowPlaying.value?.let { it.copy(ayah = it.ayah + 1, track = Track.ARABIC) }
    }

    override fun previousAyah() {
        nowPlaying.value = nowPlaying.value?.let { it.copy(ayah = (it.ayah - 1).coerceAtLeast(0), track = Track.ARABIC) }
    }

    override fun stop() {
        nowPlaying.value = null
    }

    override fun restoreLast(playWhenReady: Boolean) {
        restoreCalls++
    }
}

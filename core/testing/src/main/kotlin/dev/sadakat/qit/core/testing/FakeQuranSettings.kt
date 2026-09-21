package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [QuranSettings]. */
class FakeQuranSettings(
    mode: RecitationMode = RecitationMode.ARABIC_BANGLA,
    lastPosition: LastPosition? = null,
    readingPrefs: ReadingPrefs = ReadingPrefs(),
    playbackSpeed: PlaybackSpeed = PlaybackSpeed.X1,
) : QuranSettings {

    override val mode = MutableStateFlow(mode)
    override val lastPosition = MutableStateFlow(lastPosition)
    override val readingPrefs = MutableStateFlow(readingPrefs)
    override val playbackSpeed = MutableStateFlow(playbackSpeed)

    override suspend fun setMode(mode: RecitationMode) {
        this.mode.value = mode
    }

    override suspend fun saveLastPosition(position: LastPosition) {
        lastPosition.value = position
    }

    override suspend fun updateReadingPrefs(transform: (ReadingPrefs) -> ReadingPrefs) {
        readingPrefs.update(transform)
    }

    override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
        playbackSpeed.value = speed
    }
}

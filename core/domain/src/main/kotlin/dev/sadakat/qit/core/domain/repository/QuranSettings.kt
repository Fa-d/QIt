package dev.sadakat.qit.core.domain.repository

import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import kotlinx.coroutines.flow.Flow

data class LastPosition(val ref: AyahRef, val mode: RecitationMode)

/** User preferences and playback memory, persisted across app restarts. */
interface QuranSettings {

    /** The recitation mode the user picked; defaults to [RecitationMode.ARABIC_BANGLA]. */
    val mode: Flow<RecitationMode>

    suspend fun setMode(mode: RecitationMode)

    /** Where playback last was; null before anything has played. */
    val lastPosition: Flow<LastPosition?>

    suspend fun saveLastPosition(position: LastPosition)

    /** The reader's comfort settings; defaults until the user changes one. */
    val readingPrefs: Flow<ReadingPrefs>

    /** Applies [transform] to the stored prefs atomically. */
    suspend fun updateReadingPrefs(transform: (ReadingPrefs) -> ReadingPrefs)

    /** The recitation speed last chosen; defaults to [PlaybackSpeed.X1]. */
    val playbackSpeed: Flow<PlaybackSpeed>

    suspend fun setPlaybackSpeed(speed: PlaybackSpeed)
}

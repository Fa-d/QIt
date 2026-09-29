package dev.sadakat.qandeel.core.domain.repository

import dev.sadakat.qandeel.core.domain.model.AyahRef
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.ReadingPrefs
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.PlaybackSpeed
import kotlinx.coroutines.flow.Flow

data class LastPosition(val ref: AyahRef, val mode: RecitationMode)

/** User preferences and playback memory, persisted across app restarts. */
interface QuranSettings {

    /** The recitation mode the user picked; defaults to [RecitationMode.ARABIC_BANGLA]. */
    val mode: Flow<RecitationMode>

    suspend fun setMode(mode: RecitationMode)

    /** Who reads the Bangla (with their recording's Arabic) in Arabic + Bangla; defaults to [BanglaVoice.DEFAULT]. */
    val banglaVoice: Flow<BanglaVoice>

    suspend fun setBanglaVoice(voice: BanglaVoice)

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

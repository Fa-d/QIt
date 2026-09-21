package dev.sadakat.qit.shared.quran.settings

import android.content.Context
import dev.sadakat.qit.shared.quran.model.AyahRef
import dev.sadakat.qit.shared.quran.model.RecitationMode
import kotlinx.coroutines.flow.Flow

data class LastPosition(val ref: AyahRef, val mode: RecitationMode)

interface QuranSettings {

    /** The recitation mode the user picked; defaults to [RecitationMode.ARABIC_BANGLA]. */
    val mode: Flow<RecitationMode>

    suspend fun setMode(mode: RecitationMode)

    /** Where playback last was; null before anything has played. */
    val lastPosition: Flow<LastPosition?>

    suspend fun saveLastPosition(position: LastPosition)
}

/** [QuranSettings] in a Preferences DataStore named "quran_settings". */
class DataStoreQuranSettings(private val context: Context) : QuranSettings {

    override val mode: Flow<RecitationMode> get() = TODO("W2b")

    override suspend fun setMode(mode: RecitationMode) {
        TODO("W2b")
    }

    override val lastPosition: Flow<LastPosition?> get() = TODO("W2b")

    override suspend fun saveLastPosition(position: LastPosition) {
        TODO("W2b")
    }
}

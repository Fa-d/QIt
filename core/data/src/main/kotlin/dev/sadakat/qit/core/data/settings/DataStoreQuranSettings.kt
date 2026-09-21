package dev.sadakat.qit.core.data.settings

import android.content.Context
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.Flow

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

package dev.sadakat.qit.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One Preferences DataStore per process; the delegate keeps a single instance for the file. */
private val Context.quranDataStore by preferencesDataStore(name = "quran_settings")

/** [QuranSettings] in a Preferences DataStore named "quran_settings". */
class DataStoreQuranSettings(private val context: Context) : QuranSettings {

    override val mode: Flow<RecitationMode> =
        context.quranDataStore.data.map { preferences ->
            preferences[MODE]?.toMode() ?: DEFAULT_MODE
        }

    override suspend fun setMode(mode: RecitationMode) {
        context.quranDataStore.edit { it[MODE] = mode.name }
    }

    override val lastPosition: Flow<LastPosition?> =
        context.quranDataStore.data.map { preferences ->
            val surah = preferences[LAST_SURAH] ?: return@map null
            val ayah = preferences[LAST_AYAH] ?: return@map null
            LastPosition(AyahRef(surah, ayah), preferences[LAST_MODE]?.toMode() ?: DEFAULT_MODE)
        }

    override suspend fun saveLastPosition(position: LastPosition) {
        context.quranDataStore.edit {
            it[LAST_SURAH] = position.ref.surah
            it[LAST_AYAH] = position.ref.ayah
            it[LAST_MODE] = position.mode.name
        }
    }

    private companion object {
        val MODE = stringPreferencesKey("mode")
        val LAST_SURAH = intPreferencesKey("last_surah")
        val LAST_AYAH = intPreferencesKey("last_ayah")
        val LAST_MODE = stringPreferencesKey("last_mode")
        val DEFAULT_MODE = RecitationMode.ARABIC_BANGLA
    }
}

/** The stored enum name, or null when no [RecitationMode] has that name (an older app wrote it). */
private fun String.toMode(): RecitationMode? = RecitationMode.entries.firstOrNull { it.name == this }

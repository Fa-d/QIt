package dev.sadakat.qit.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.WordByWord
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
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

    override val readingPrefs: Flow<ReadingPrefs> =
        context.quranDataStore.data.map { it.readingPrefs() }

    override suspend fun updateReadingPrefs(transform: (ReadingPrefs) -> ReadingPrefs) {
        context.quranDataStore.edit { preferences ->
            val prefs = transform(preferences.readingPrefs())
            preferences[ARABIC_TEXT_SIZE] = prefs.arabicTextSize.name
            preferences[SHOW_TRANSLATION] = prefs.showTranslation
            preferences[FOLLOW_ALONG] = prefs.followAlong
            preferences[WORD_BY_WORD] = prefs.wordByWord.name
            preferences[THEME_MODE] = prefs.themeMode.name
            preferences[DYNAMIC_COLOR] = prefs.dynamicColor
        }
    }

    override val playbackSpeed: Flow<PlaybackSpeed> =
        context.quranDataStore.data.map { preferences ->
            preferences[PLAYBACK_SPEED]?.let { enumOrNull<PlaybackSpeed>(it) } ?: PlaybackSpeed.X1
        }

    override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
        context.quranDataStore.edit { it[PLAYBACK_SPEED] = speed.name }
    }

    private fun Preferences.readingPrefs(): ReadingPrefs {
        val defaults = ReadingPrefs()
        return ReadingPrefs(
            arabicTextSize = this[ARABIC_TEXT_SIZE]?.let { enumOrNull<ArabicTextSize>(it) } ?: defaults.arabicTextSize,
            showTranslation = this[SHOW_TRANSLATION] ?: defaults.showTranslation,
            followAlong = this[FOLLOW_ALONG] ?: defaults.followAlong,
            wordByWord = this[WORD_BY_WORD]?.let { enumOrNull<WordByWord>(it) } ?: defaults.wordByWord,
            themeMode = this[THEME_MODE]?.let { enumOrNull<ThemeMode>(it) } ?: defaults.themeMode,
            dynamicColor = this[DYNAMIC_COLOR] ?: defaults.dynamicColor,
        )
    }

    private companion object {
        val MODE = stringPreferencesKey("mode")
        val LAST_SURAH = intPreferencesKey("last_surah")
        val LAST_AYAH = intPreferencesKey("last_ayah")
        val LAST_MODE = stringPreferencesKey("last_mode")
        val ARABIC_TEXT_SIZE = stringPreferencesKey("arabic_text_size")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val FOLLOW_ALONG = booleanPreferencesKey("follow_along")
        val WORD_BY_WORD = stringPreferencesKey("word_by_word")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PLAYBACK_SPEED = stringPreferencesKey("playback_speed")
        val DEFAULT_MODE = RecitationMode.ARABIC_BANGLA
    }
}

/** The stored enum name, or null when no [RecitationMode] has that name (an older app wrote it). */
private fun String.toMode(): RecitationMode? = enumOrNull<RecitationMode>(this)

/** The [E] named [name], or null for a name this version doesn't know. */
private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }

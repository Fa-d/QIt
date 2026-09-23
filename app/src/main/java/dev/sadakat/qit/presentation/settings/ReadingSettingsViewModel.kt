package dev.sadakat.qit.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.WordByWord
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingSettingsUiState(
    val prefs: ReadingPrefs = ReadingPrefs(),
    val voice: BanglaVoice = BanglaVoice.DEFAULT,
)

/** The reading-comfort settings, shown in the sheet the reader and the home screen open. */
@HiltViewModel
class ReadingSettingsViewModel @Inject constructor(
    private val settings: QuranSettings,
    private val player: QuranPlayer,
) : ViewModel() {

    val uiState: StateFlow<ReadingSettingsUiState> = combine(settings.readingPrefs, settings.banglaVoice) {
            prefs,
            voice,
        ->
        ReadingSettingsUiState(prefs = prefs, voice = voice)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingSettingsUiState())

    fun setArabicTextSize(size: ArabicTextSize) {
        updateReadingPrefs { it.copy(arabicTextSize = size) }
    }

    fun setShowTranslation(show: Boolean) {
        updateReadingPrefs { it.copy(showTranslation = show) }
    }

    fun setFollowAlong(follow: Boolean) {
        updateReadingPrefs { it.copy(followAlong = follow) }
    }

    /** Persists [voice]; if Arabic + Bangla is playing, restarts it at the current ayah with it. */
    fun setBanglaVoice(voice: BanglaVoice) {
        viewModelScope.launch {
            settings.setBanglaVoice(voice)
            player.nowPlaying.value?.let { current ->
                if (current.mode == RecitationMode.ARABIC_BANGLA && current.voice != voice) {
                    player.play(current.surah, current.ayah, current.mode)
                }
            }
        }
    }

    fun setWordByWord(language: WordByWord) {
        updateReadingPrefs { it.copy(wordByWord = language) }
    }

    fun setThemeMode(mode: ThemeMode) {
        updateReadingPrefs { it.copy(themeMode = mode) }
    }

    private fun updateReadingPrefs(transform: (ReadingPrefs) -> ReadingPrefs) {
        viewModelScope.launch { settings.updateReadingPrefs(transform) }
    }
}

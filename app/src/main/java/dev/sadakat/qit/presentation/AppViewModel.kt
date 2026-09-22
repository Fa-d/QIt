package dev.sadakat.qit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.ArabicTextSize
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The app-wide look: theme, wallpaper colors and Arabic size, from the reading settings. */
data class AppUiState(
    /** False until the settings are read, so the first frame already has the right theme. */
    val isReady: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val arabicTextSize: ArabicTextSize = ArabicTextSize.MEDIUM,
)

@HiltViewModel
class AppViewModel @Inject constructor(settings: QuranSettings) : ViewModel() {

    val uiState: StateFlow<AppUiState> = settings.readingPrefs
        .map { AppUiState(isReady = true, it.themeMode, it.dynamicColor, it.arabicTextSize) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())
}

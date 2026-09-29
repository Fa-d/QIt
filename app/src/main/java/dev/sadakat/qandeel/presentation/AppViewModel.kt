package dev.sadakat.qandeel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qandeel.core.domain.model.ArabicTextSize
import dev.sadakat.qandeel.core.domain.model.ThemeMode
import dev.sadakat.qandeel.core.domain.model.UiStyle
import dev.sadakat.qandeel.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The app-wide look: style, page tone, wallpaper colors and Arabic size, from the reading settings. */
data class AppUiState(
    /** False until the settings are read, so the first frame already has the right theme. */
    val isReady: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val arabicTextSize: ArabicTextSize = ArabicTextSize.MEDIUM,
    val uiStyle: UiStyle = UiStyle.MUSHAF,
)

@HiltViewModel
class AppViewModel @Inject constructor(settings: QuranSettings) : ViewModel() {

    val uiState: StateFlow<AppUiState> = settings.readingPrefs
        .map { prefs ->
            AppUiState(
                isReady = true,
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor,
                arabicTextSize = prefs.arabicTextSize,
                uiStyle = prefs.uiStyle,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())
}

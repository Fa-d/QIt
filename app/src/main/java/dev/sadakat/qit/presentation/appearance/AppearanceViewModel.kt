package dev.sadakat.qit.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.ReadingPrefs
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.core.domain.model.UiStyle
import dev.sadakat.qit.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How the app looks: its style, the page tone and whether the wallpaper colors it. */
data class AppearanceUiState(
    val style: UiStyle = UiStyle.MUSHAF,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
)

/** The Appearance settings. Every change applies at once: the whole app restyles behind the screen. */
@HiltViewModel
class AppearanceViewModel @Inject constructor(private val settings: QuranSettings) : ViewModel() {

    val uiState: StateFlow<AppearanceUiState> = settings.readingPrefs
        .map { AppearanceUiState(style = it.uiStyle, themeMode = it.themeMode, dynamicColor = it.dynamicColor) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceUiState())

    fun setStyle(style: UiStyle) {
        update { it.copy(uiStyle = style) }
    }

    fun setThemeMode(mode: ThemeMode) {
        update { it.copy(themeMode = mode) }
    }

    fun setDynamicColor(use: Boolean) {
        update { it.copy(dynamicColor = use) }
    }

    private fun update(transform: (ReadingPrefs) -> ReadingPrefs) {
        viewModelScope.launch { settings.updateReadingPrefs(transform) }
    }
}

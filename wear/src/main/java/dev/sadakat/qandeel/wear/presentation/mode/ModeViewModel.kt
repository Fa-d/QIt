package dev.sadakat.qandeel.wear.presentation.mode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.repository.QuranSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WearModeUiState(val mode: RecitationMode = RecitationMode.ARABIC_BANGLA)

/** Picks the recitation mode (what plays for each ayah); replaces the old cycling chip. */
@HiltViewModel
class ModeViewModel @Inject constructor(private val settings: QuranSettings) : ViewModel() {

    val uiState: StateFlow<WearModeUiState> = settings.mode
        .map { WearModeUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearModeUiState())

    /** Persists the choice; navigation pops back so the picker never outstays its welcome. */
    fun select(mode: RecitationMode) {
        viewModelScope.launch { settings.setMode(mode) }
    }
}

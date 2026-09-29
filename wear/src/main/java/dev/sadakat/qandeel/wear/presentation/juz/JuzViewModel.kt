package dev.sadakat.qandeel.wear.presentation.juz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qandeel.core.domain.model.AyahRef
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.repository.QuranText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One of the Quran's thirty parts: where it starts, to label the row and open the surah. */
data class JuzRowUiModel(val juz: Int, val start: AyahRef, val surahName: String?)

data class WearJuzUiState(val rows: List<JuzRowUiModel> = emptyList())

/** The 30 juz, each opening its surah at the juz's first ayah. */
@HiltViewModel
class JuzViewModel @Inject constructor(quranText: QuranText) : ViewModel() {

    val uiState: StateFlow<WearJuzUiState> = flow {
        // Names are cosmetic; the juz itself is fixed structure the domain already knows.
        val names = runCatching { quranText.surahs().associate { it.number to it.nameEnglish } }
            .getOrDefault(emptyMap())
        val rows = (1..QuranMeta.JUZ_COUNT).map { juz ->
            val start = QuranMeta.juzStart(juz)
            JuzRowUiModel(juz = juz, start = start, surahName = names[start.surah])
        }
        emit(WearJuzUiState(rows))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearJuzUiState())
}

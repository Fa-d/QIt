package dev.sadakat.qit.wear.presentation.surahlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import dev.sadakat.qit.core.domain.repository.stateOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One surah row: the surah itself plus its download state for the current mode. */
data class SurahRowUiModel(val surah: Surah, val download: SurahDownloadState)

/** The surah list, whole or filtered down to what plays offline. */
data class WearSurahListUiState(
    val rows: List<SurahRowUiModel> = emptyList(),
    val downloadedOnly: Boolean = false,
    val mode: RecitationMode = RecitationMode.ARABIC_BANGLA,
)

/** The 114 surahs, or the downloaded subset when navigated with `downloaded=true`. */
@HiltViewModel
class SurahListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    quranText: QuranText,
    settings: QuranSettings,
    surahDownloads: SurahDownloads,
) : ViewModel() {

    private val downloadedOnly: Boolean = savedStateHandle.get<Boolean>("downloaded") ?: false

    private val surahsFlow = flow { emit(runCatching { quranText.surahs() }.getOrDefault(emptyList())) }

    val uiState: StateFlow<WearSurahListUiState> = combine(
        surahsFlow,
        combine(settings.mode, settings.banglaVoice, ::Pair),
        surahDownloads.states,
    ) { surahs, (mode, voice), downloads ->
        WearSurahListUiState(
            rows = surahs
                .map { SurahRowUiModel(it, downloads.stateOf(it.number, mode.tracks(voice))) }
                .filter { !downloadedOnly || it.download is SurahDownloadState.Downloaded },
            downloadedOnly = downloadedOnly,
            mode = mode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearSurahListUiState())
}

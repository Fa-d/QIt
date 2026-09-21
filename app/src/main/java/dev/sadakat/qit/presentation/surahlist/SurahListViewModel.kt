package dev.sadakat.qit.presentation.surahlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import dev.sadakat.qit.core.domain.repository.stateOf
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the continue-listening card resumes. */
data class ContinueListening(val surah: Int, val surahName: String, val ayah: Int)

data class SurahListUiState(
    val query: String = "",
    val surahs: List<Surah> = emptyList(),
    val loadFailed: Boolean = false,
    val mode: RecitationMode = RecitationMode.ARABIC_BANGLA,
    val downloadStates: Map<Int, SurahDownloadState> = emptyMap(),
    val continueListening: ContinueListening? = null,
)

@HiltViewModel
class SurahListViewModel @Inject constructor(
    quranText: QuranText,
    private val settings: QuranSettings,
    downloads: SurahDownloads,
    private val player: QuranPlayer,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private data class SurahsLoad(val surahs: List<Surah> = emptyList(), val failed: Boolean = false)

    private val surahsLoad = flow { emit(SurahsLoad(surahs = quranText.surahs())) }
        .catch { emit(SurahsLoad(failed = true)) }

    // The settings are cold flows in production; cache the latest values for synchronous actions.
    private var currentMode: RecitationMode = RecitationMode.ARABIC_BANGLA
    private var currentLastPosition: LastPosition? = null

    init {
        viewModelScope.launch { settings.mode.collect { currentMode = it } }
        viewModelScope.launch { settings.lastPosition.collect { currentLastPosition = it } }
    }

    val uiState: StateFlow<SurahListUiState> = combine(
        surahsLoad,
        query,
        settings.mode,
        settings.lastPosition,
        downloads.states,
    ) { load, query, mode, lastPosition, states ->
        SurahListUiState(
            query = query,
            surahs = load.surahs.filter { it.matches(query) },
            loadFailed = load.failed,
            mode = mode,
            downloadStates = load.surahs.associate { it.number to states.stateOf(it.number, mode.tracks) },
            continueListening = lastPosition?.let { last ->
                load.surahs.firstOrNull { it.number == last.ref.surah }?.let { surah ->
                    ContinueListening(surah.number, surah.nameEnglish, last.ref.ayah)
                }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurahListUiState())

    fun onSearchQueryChange(query: String) {
        this.query.value = query
    }

    /** Resumes the saved position in the mode it was saved with. */
    fun continueListening() {
        val last = currentLastPosition ?: return
        player.play(last.ref.surah, last.ref.ayah, last.mode)
    }
}

/** A surah matches by its exact number, or a case-insensitive substring of any of its names. */
private fun Surah.matches(rawQuery: String): Boolean {
    val query = rawQuery.trim()
    if (query.isEmpty()) return true
    if (query.toIntOrNull() == number) return true
    return nameEnglish.contains(query, ignoreCase = true) ||
        meaningEnglish.contains(query, ignoreCase = true) ||
        nameArabic.contains(query)
}

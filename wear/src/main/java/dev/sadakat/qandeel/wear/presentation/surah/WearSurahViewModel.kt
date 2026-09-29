package dev.sadakat.qandeel.wear.presentation.surah

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.model.Surah
import dev.sadakat.qandeel.core.domain.player.QuranPlayer
import dev.sadakat.qandeel.core.domain.repository.QuranSettings
import dev.sadakat.qandeel.core.domain.repository.QuranText
import dev.sadakat.qandeel.core.domain.repository.SurahDownloadState
import dev.sadakat.qandeel.core.domain.repository.SurahDownloads
import dev.sadakat.qandeel.core.domain.repository.stateOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WearSurahUiState(
    val surah: Surah? = null,
    val mode: RecitationMode = RecitationMode.ARABIC_BANGLA,
    val download: SurahDownloadState = SurahDownloadState.NotDownloaded,
)

/** One surah: play it (from a juz start if arrived that way), download its audio, or remove it. */
@HiltViewModel
class WearSurahViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    quranText: QuranText,
    settings: QuranSettings,
    private val surahDownloads: SurahDownloads,
    private val player: QuranPlayer,
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle.get<Int>("number") ?: 0

    /** The ayah to play from when the screen was opened from a juz (0 or absent: from the start). */
    private val fromAyah: Int? = savedStateHandle.get<Int>("from")?.takeIf { it > 0 }

    private val surahFlow = flow { emit(runCatching { quranText.surah(surahNumber) }.getOrNull()) }

    /** The voice the mode's tracks are read in, kept for the actions (no picker on the watch). */
    private var currentVoice: BanglaVoice = BanglaVoice.DEFAULT

    init {
        viewModelScope.launch { settings.banglaVoice.collect { currentVoice = it } }
    }

    val uiState: StateFlow<WearSurahUiState> = combine(
        surahFlow,
        combine(settings.mode, settings.banglaVoice, ::Pair),
        surahDownloads.states,
    ) { surah, (mode, voice), downloads ->
        WearSurahUiState(
            surah = surah,
            mode = mode,
            download = if (surah != null) {
                downloads.stateOf(surah.number, mode.tracks(voice))
            } else {
                SurahDownloadState.NotDownloaded
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearSurahUiState())

    /** Plays from the juz start if given, else the basmala (ayah 0) when the surah has one, else 1. */
    fun play() {
        val state = uiState.value
        val surah = state.surah ?: return
        val fromAyah = fromAyah ?: if (QuranMeta.hasBasmalaPrefix(surah.number)) 0 else 1
        player.play(surah.number, fromAyah, state.mode)
    }

    fun download() {
        val state = uiState.value
        val surah = state.surah ?: return
        surahDownloads.download(surah.number, state.mode.tracks(currentVoice))
    }

    fun remove() {
        val state = uiState.value
        val surah = state.surah ?: return
        surahDownloads.remove(surah.number, state.mode.tracks(currentVoice))
    }
}

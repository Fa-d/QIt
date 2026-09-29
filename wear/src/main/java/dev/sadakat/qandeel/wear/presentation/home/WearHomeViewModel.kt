package dev.sadakat.qandeel.wear.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.RecitationMode
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
import javax.inject.Inject

/** The home hub: where the rest of the app hangs off, plus what the edge button should do. */
data class WearHomeUiState(
    val loaded: Boolean = false,
    val mode: RecitationMode = RecitationMode.ARABIC_BANGLA,
    /** Surahs fully downloaded for the current mode; the "Downloaded" row hides at zero. */
    val downloadedCount: Int = 0,
    /** True while anything is queued, playing or paused. */
    val isQueued: Boolean = false,
    /** "18:23" of the last saved position; null when nothing was saved or something is queued. */
    val continuePosition: String? = null,
)

/** Home: a small hub (surahs, juz, downloads, recitation) over the shared player state. */
@HiltViewModel
class WearHomeViewModel @Inject constructor(
    quranText: QuranText,
    settings: QuranSettings,
    surahDownloads: SurahDownloads,
    private val player: QuranPlayer,
) : ViewModel() {

    private val surahsFlow = flow { emit(runCatching { quranText.surahs() }.getOrDefault(emptyList())) }

    /** The mode with the voice that plays with it: together they name the downloaded tracks. */
    private data class Recitation(val mode: RecitationMode, val voice: BanglaVoice)

    private val recitation = combine(settings.mode, settings.banglaVoice, ::Recitation)

    val uiState: StateFlow<WearHomeUiState> = combine(
        surahsFlow,
        recitation,
        surahDownloads.states,
        player.nowPlaying,
        settings.lastPosition,
    ) { surahs, recitation, downloads, nowPlaying, lastPosition ->
        WearHomeUiState(
            loaded = surahs.isNotEmpty(),
            mode = recitation.mode,
            downloadedCount = surahs.count {
                downloads.stateOf(it.number, recitation.mode.tracks(recitation.voice)) is SurahDownloadState.Downloaded
            },
            isQueued = nowPlaying != null,
            continuePosition = if (nowPlaying == null) lastPosition?.let { "${it.ref.surah}:${it.ref.ayah}" } else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearHomeUiState())

    /** Queues the last saved position so Continue picks up where the user left off. */
    fun continuePlaying() {
        player.restoreLast(playWhenReady = true)
    }
}

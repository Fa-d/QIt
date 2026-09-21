package dev.sadakat.qit.wear.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.player.QuranPlayer
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Home screen: surah list with offline state, plus the playback and mode chips. */
@HiltViewModel
class WearHomeViewModel @Inject constructor(
    quranText: QuranText,
    private val settings: QuranSettings,
    surahDownloads: SurahDownloads,
    private val player: QuranPlayer,
) : ViewModel() {

    /** "Now playing" chip content, shown while something is queued. */
    data class NowPlayingChip(val surahName: String, val position: String)

    /** "Continue" chip content, shown when nothing is queued but a position was saved. */
    data class ContinueChip(val surahName: String, val position: String)

    data class SurahRow(val surah: Surah, val download: SurahDownloadState)

    data class UiState(
        val rows: List<SurahRow> = emptyList(),
        val mode: RecitationMode = RecitationMode.ARABIC_BANGLA,
        val nowPlayingChip: NowPlayingChip? = null,
        val continueChip: ContinueChip? = null,
    )

    private val surahsFlow = flow { emit(quranText.surahs()) }

    val uiState: StateFlow<UiState> = combine(
        surahsFlow,
        surahDownloads.states,
        settings.mode,
        player.nowPlaying,
        settings.lastPosition,
    ) { surahs, downloads, mode, nowPlaying, lastPosition ->
        UiState(
            rows = surahs.map { SurahRow(it, downloads.stateOf(it.number, mode.tracks)) },
            mode = mode,
            nowPlayingChip = nowPlaying?.let {
                NowPlayingChip(surahs.nameOf(it.surah), positionOf(it.surah, it.ayah))
            },
            continueChip = if (nowPlaying == null && lastPosition != null) {
                val ref = lastPosition.ref
                ContinueChip(surahs.nameOf(ref.surah), positionOf(ref.surah, ref.ayah))
            } else {
                null
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /** Steps Arabic-only → Arabic + English → Arabic + Bangla, persisting the choice. */
    fun cycleMode() {
        viewModelScope.launch {
            val next = RecitationMode.entries[(uiState.value.mode.ordinal + 1) % RecitationMode.entries.size]
            settings.setMode(next)
        }
    }

    /** Queues the last saved position so Continue picks up where the user left off. */
    fun continuePlaying() {
        player.restoreLast(playWhenReady = true)
    }
}

private fun List<Surah>.nameOf(number: Int): String =
    firstOrNull { it.number == number }?.nameEnglish ?: "Surah $number"

private fun positionOf(surah: Int, ayah: Int): String = "$surah:$ayah"

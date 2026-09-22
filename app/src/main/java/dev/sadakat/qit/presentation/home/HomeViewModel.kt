package dev.sadakat.qit.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.AyahRefParser
import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.repository.LastPosition
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
import dev.sadakat.qit.core.domain.repository.SurahDownloadState
import dev.sadakat.qit.core.domain.repository.SurahDownloads
import dev.sadakat.qit.core.domain.repository.stateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BrowseMode { SURAH, JUZ }

data class SurahRowUi(val surah: Surah, val download: SurahDownloadState, val isPlaying: Boolean)

data class JuzRowUi(val juz: Int, val start: AyahRef, val surahName: String)

/** A typed "2:255": where it goes. */
data class AyahJumpUi(val ref: AyahRef, val surahName: String)

/**
 * The continue card: what is queued now ([isCurrent]) or, after a restart before anything is
 * queued, where listening last stopped.
 */
data class ContinueListeningUi(
    val surah: Int,
    val surahName: String,
    val surahNameArabic: String,
    val ayah: Int,
    val ayahCount: Int,
    val isCurrent: Boolean,
    val isPlaying: Boolean,
) {
    val progress: Float get() = ayah.toFloat() / ayahCount
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val continueListening: ContinueListeningUi? = null,
    val query: String = "",
    val jumpTarget: AyahJumpUi? = null,
    val browse: BrowseMode = BrowseMode.SURAH,
    /** Surahs matching [query] (all of them when it's blank). */
    val surahs: List<SurahRowUi> = emptyList(),
    val juz: List<JuzRowUi> = emptyList(),
) {
    val isSearching: Boolean get() = query.isNotBlank()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    quranText: QuranText,
    private val settings: QuranSettings,
    downloads: SurahDownloads,
    private val player: QuranPlayer,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val browse = MutableStateFlow(BrowseMode.SURAH)

    private data class Load(val surahs: List<Surah> = emptyList(), val failed: Boolean = false)

    private val load = flow { emit(Load(surahs = quranText.surahs())) }.catch { emit(Load(failed = true)) }

    private data class Listening(val nowPlaying: NowPlaying?, val lastPosition: LastPosition?)

    private val listening = combine(player.nowPlaying, settings.lastPosition, ::Listening)

    /** The mode with the voice that plays with it: together they name the tracks acted on. */
    private data class Recitation(val mode: RecitationMode, val voice: BanglaVoice)

    private val recitation = combine(settings.mode, settings.banglaVoice, ::Recitation)

    private data class Browsing(val query: String, val browse: BrowseMode)

    private val browsing = combine(query, browse, ::Browsing)

    val uiState: StateFlow<HomeUiState> = combine(
        load,
        browsing,
        recitation,
        downloads.states,
        listening,
    ) { load, browsing, recitation, downloadStates, listening ->
        val byNumber = load.surahs.associateBy { it.number }
        val playingSurah = listening.nowPlaying?.surah
        HomeUiState(
            isLoading = load.surahs.isEmpty() && !load.failed,
            loadFailed = load.failed,
            continueListening = continueCard(listening, byNumber),
            query = browsing.query,
            jumpTarget = AyahRefParser.parse(browsing.query)?.let { ref ->
                byNumber[ref.surah]?.let { AyahJumpUi(ref, it.nameEnglish) }
            },
            browse = browsing.browse,
            surahs = load.surahs
                .filter { SurahSearch.matches(it, browsing.query) }
                .map {
                    SurahRowUi(
                        it,
                        downloadStates.stateOf(it.number, recitation.mode.tracks(recitation.voice)),
                        it.number == playingSurah,
                    )
                },
            juz = if (byNumber.isEmpty()) emptyList() else juzRows(byNumber),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(query: String) {
        this.query.value = query
    }

    fun onBrowseChange(mode: BrowseMode) {
        browse.value = mode
    }

    /** The card's play button: pauses or resumes what is queued, else resumes the saved position. */
    fun onContinuePlayPause() {
        if (player.nowPlaying.value != null) {
            player.togglePlayPause()
            return
        }
        viewModelScope.launch {
            val last = settings.lastPosition.first() ?: return@launch
            player.play(last.ref.surah, last.ref.ayah, last.mode)
        }
    }

    private fun continueCard(listening: Listening, surahs: Map<Int, Surah>): ContinueListeningUi? {
        val nowPlaying = listening.nowPlaying
        val (ref, isPlaying) = when {
            nowPlaying != null -> AyahRef(nowPlaying.surah, nowPlaying.ayah) to nowPlaying.isPlaying
            listening.lastPosition != null -> listening.lastPosition.ref to false
            else -> return null
        }
        val surah = surahs[ref.surah] ?: return null
        return ContinueListeningUi(
            surah = surah.number,
            surahName = surah.nameEnglish,
            surahNameArabic = surah.nameArabicShort,
            ayah = ref.ayah,
            ayahCount = surah.ayahCount,
            isCurrent = nowPlaying != null,
            isPlaying = isPlaying,
        )
    }

    private fun juzRows(surahs: Map<Int, Surah>): List<JuzRowUi> = (1..QuranMeta.JUZ_COUNT).map { juz ->
        val start = QuranMeta.juzStart(juz)
        JuzRowUi(juz, start, surahs[start.surah]?.nameEnglish.orEmpty())
    }
}

package dev.sadakat.qandeel.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qandeel.core.domain.audio.QueuePlan
import dev.sadakat.qandeel.core.domain.audio.SurahTimeline
import dev.sadakat.qandeel.core.domain.model.Ayah
import dev.sadakat.qandeel.core.domain.model.BanglaVoice
import dev.sadakat.qandeel.core.domain.model.QuranMeta
import dev.sadakat.qandeel.core.domain.model.RecitationMode
import dev.sadakat.qandeel.core.domain.player.NowPlaying
import dev.sadakat.qandeel.core.domain.player.PlaybackError
import dev.sadakat.qandeel.core.domain.player.PlaybackProgress
import dev.sadakat.qandeel.core.domain.player.PlaybackSpeed
import dev.sadakat.qandeel.core.domain.player.QuranPlayer
import dev.sadakat.qandeel.core.domain.player.RepeatSetting
import dev.sadakat.qandeel.core.domain.player.SleepOption
import dev.sadakat.qandeel.core.domain.player.SleepTimerStatus
import dev.sadakat.qandeel.core.domain.player.WordPointer
import dev.sadakat.qandeel.core.domain.repository.AudioTimings
import dev.sadakat.qandeel.core.domain.repository.QuranSettings
import dev.sadakat.qandeel.core.domain.repository.QuranText
import dev.sadakat.qandeel.core.domain.repository.WordMeanings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the mini player and the full player show. */
data class PlayerUiState(
    val nowPlaying: NowPlaying? = null,
    /** Who reads the Bangla of the queued surah, from [NowPlaying.voice]; matters only in Arabic + Bangla. */
    val voice: BanglaVoice = BanglaVoice.DEFAULT,
    /** English name of the playing surah; null while unknown. */
    val surahName: String? = null,
    /** The playing ayah's Arabic; null for the basmala (the screen shows it itself). */
    val ayahArabic: String? = null,
    /** Its translation in the playing mode, if the mode has one and translations are shown. */
    val ayahTranslation: String? = null,
    /** The meaning of each of its words (the basmala's too) while word by word is on; else empty. */
    val ayahMeanings: List<String> = emptyList(),
    val sleepTimer: SleepTimerStatus = SleepTimerStatus.Off,
    val error: PlaybackError? = null,
    /**
     * Where each ayah starts in the surah (ms), by ayah - 1 (the basmala comes before the first):
     * names the ayah under the time bar's thumb. Empty while the files' lengths are unknown.
     */
    val ayahStartsMs: List<Long> = emptyList(),
) {
    /** The ayah playing at [surahPositionMs] into the surah (0 = the basmala), or null if unknown. */
    fun ayahAt(surahPositionMs: Long): Int? {
        if (ayahStartsMs.isEmpty()) return null
        return ayahStartsMs.indexOfLast { it <= surahPositionMs } + 1
    }
}

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest/mapLatest: drop a stale surah's lookups.
@HiltViewModel
@Suppress("TooManyFunctions") // One function per transport control, each a one-liner on the player.
class PlayerViewModel @Inject constructor(
    private val player: QuranPlayer,
    private val quranText: QuranText,
    private val settings: QuranSettings,
    private val timings: AudioTimings,
    private val wordMeanings: WordMeanings,
) : ViewModel() {

    // The error the user dismissed; hidden until the player clears it (a retry) or reports another.
    private val dismissedError = MutableStateFlow<PlaybackError?>(null)

    init {
        // Rebuild the queue from the last session, paused, so the player reappears where playback
        // left off (a no-op when something is already queued).
        player.restoreLast(playWhenReady = false)
        // The player clears its error when playback is retried; forget the dismissal then, so the
        // same failure happening again is shown again (the messages are fixed strings).
        viewModelScope.launch { player.error.collect { if (it == null) dismissedError.value = null } }
    }

    private val surahNames = flow {
        emit(quranText.surahs().associate { it.number to it.nameEnglish })
    }.catch {
        // Names are cosmetic; the player falls back to "Surah N".
        emit(emptyMap())
    }

    /** The playing surah's ayahs, loaded once per surah. */
    private val surahAyahs = player.nowPlaying
        .map { it?.surah }
        .distinctUntilChanged()
        .flatMapLatest { surah ->
            if (surah == null) {
                flowOf(emptyList())
            } else {
                flow { emit(quranText.ayahs(surah)) }.catch { emit(emptyList()) }
            }
        }

    /** The playing surah's word meanings in the word-by-word language, loaded once per surah and language. */
    private val surahMeanings = combine(
        player.nowPlaying.map { it?.surah }.distinctUntilChanged(),
        settings.readingPrefs.map { it.wordByWord }.distinctUntilChanged(),
    ) { surah, language -> surah to language }
        .flatMapLatest { (surah, language) ->
            if (surah == null) {
                flowOf(emptyMap())
            } else {
                flow { emit(wordMeanings.meanings(surah, language)) }.catch { emit(emptyMap()) }
            }
        }

    private data class Reading(
        val ayahs: List<Ayah>,
        val showTranslation: Boolean,
        val meanings: Map<Int, List<String>>,
    )

    private val reading = combine(surahAyahs, settings.readingPrefs, surahMeanings) { ayahs, prefs, meanings ->
        Reading(ayahs, prefs.showTranslation, meanings)
    }

    /** Where each ayah of the playing queue starts; empty while its files' lengths are unknown. */
    private val ayahStarts = player.nowPlaying
        .map { playing -> playing?.let { Triple(it.surah, it.mode, it.voice) } }
        .distinctUntilChanged()
        .mapLatest { key -> key?.let { (surah, mode, voice) -> ayahStartsOf(surah, mode, voice) }.orEmpty() }
        .catch { emit(emptyList()) }

    private suspend fun ayahStartsOf(surah: Int, mode: RecitationMode, voice: BanglaVoice): List<Long> {
        val entries = QueuePlan.plan(surah, mode, voice)
        val timeline = SurahTimeline(entries.map { timings.durationMs(it.file.id) ?: return emptyList() })
        return (1..QuranMeta.ayahCount(surah)).map { ayah ->
            timeline.positionOf(entries.indexOfFirst { it.id.ayah == ayah }, 0)
        }
    }

    private data class Status(
        val error: PlaybackError?,
        val dismissed: PlaybackError?,
        val sleepTimer: SleepTimerStatus,
        val ayahStarts: List<Long>,
    )

    private val status = combine(player.error, dismissedError, player.sleepTimer, ayahStarts, ::Status)

    val uiState: StateFlow<PlayerUiState> = combine(
        player.nowPlaying,
        surahNames,
        reading,
        status,
    ) { nowPlaying, names, reading, status ->
        val ayah = nowPlaying?.takeIf { it.ayah >= 1 }?.let { reading.ayahs.getOrNull(it.ayah - 1) }
        PlayerUiState(
            nowPlaying = nowPlaying,
            voice = nowPlaying?.voice ?: BanglaVoice.DEFAULT,
            surahName = nowPlaying?.let { names[it.surah] },
            ayahArabic = ayah?.arabic,
            ayahTranslation = nowPlaying?.mode?.translation
                ?.takeIf { reading.showTranslation }
                ?.let { track -> ayah?.translation(track) },
            ayahMeanings = nowPlaying?.let { reading.meanings[it.ayah] }.orEmpty(),
            sleepTimer = status.sleepTimer,
            error = status.error?.takeIf { it != status.dismissed },
            ayahStartsMs = status.ayahStarts,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    /** Where playback is in the surah; ticks while playing, so only the time bars read it. */
    val progress: StateFlow<PlaybackProgress> =
        player.progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackProgress.START)

    /** The word being recited; changes once per word, not with every tick of [progress]. */
    val pointer: StateFlow<WordPointer> =
        player.pointer.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordPointer.Off)

    /** Moves to [surahPositionMs] into the surah. */
    fun seekTo(surahPositionMs: Long) = player.seekTo(surahPositionMs)

    fun togglePlayPause() = player.togglePlayPause()

    fun nextAyah() = player.nextAyah()

    fun previousAyah() = player.previousAyah()

    fun stop() = player.stop()

    fun consumeError() {
        dismissedError.value = player.error.value
    }

    /** Prepares the failed queue again and plays from where it stopped. */
    fun retry() = player.retry()

    /** Persists [mode] and continues the playing ayah in it. */
    fun setMode(mode: RecitationMode) {
        val current = player.nowPlaying.value ?: return
        viewModelScope.launch {
            settings.setMode(mode)
            player.play(current.surah, current.ayah, mode)
        }
    }

    /** Persists [voice] and continues the queued ayah with it, playing or paused as it was. */
    fun setVoice(voice: BanglaVoice) {
        viewModelScope.launch {
            settings.setBanglaVoice(voice)
            player.nowPlaying.value?.let { current ->
                if (current.mode == RecitationMode.ARABIC_BANGLA && current.voice != voice) {
                    // Re-queued in the new voice at the same ayah, playing or paused as it was.
                    player.play(current.surah, current.ayah, current.mode, playWhenReady = current.isPlaying)
                }
            }
        }
    }

    fun setRepeat(repeat: RepeatSetting) = player.setRepeat(repeat)

    fun setSpeed(speed: PlaybackSpeed) = player.setSpeed(speed)

    /** Starts a sleep timer, or cancels the running one with null. */
    fun setSleepTimer(option: SleepOption?) = player.setSleepTimer(option)
}

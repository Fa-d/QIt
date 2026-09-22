package dev.sadakat.qit.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sadakat.qit.core.domain.audio.QueuePlan
import dev.sadakat.qit.core.domain.audio.SurahTimeline
import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackProgress
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.core.domain.player.WordPointer
import dev.sadakat.qit.core.domain.repository.AudioTimings
import dev.sadakat.qit.core.domain.repository.QuranSettings
import dev.sadakat.qit.core.domain.repository.QuranText
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
    /** English name of the playing surah; null while unknown. */
    val surahName: String? = null,
    /** The playing ayah's Arabic; null for the basmala (the screen shows it itself). */
    val ayahArabic: String? = null,
    /** Its translation in the playing mode, if the mode has one and translations are shown. */
    val ayahTranslation: String? = null,
    val sleepTimer: SleepTimerStatus = SleepTimerStatus.Off,
    val error: String? = null,
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
class PlayerViewModel @Inject constructor(
    private val player: QuranPlayer,
    private val quranText: QuranText,
    private val settings: QuranSettings,
    private val timings: AudioTimings,
) : ViewModel() {

    // The error the user dismissed; hidden until the player clears it (a retry) or reports another.
    private val dismissedError = MutableStateFlow<String?>(null)

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

    private data class Reading(val ayahs: List<Ayah>, val showTranslation: Boolean)

    private val reading =
        combine(surahAyahs, settings.readingPrefs) { ayahs, prefs -> Reading(ayahs, prefs.showTranslation) }

    /** Where each ayah of the playing queue starts; empty while its files' lengths are unknown. */
    private val ayahStarts = player.nowPlaying
        .map { playing -> playing?.let { it.surah to it.mode } }
        .distinctUntilChanged()
        .mapLatest { key -> key?.let { (surah, mode) -> ayahStartsOf(surah, mode) }.orEmpty() }
        .catch { emit(emptyList()) }

    private suspend fun ayahStartsOf(surah: Int, mode: RecitationMode): List<Long> {
        val entries = QueuePlan.plan(surah, mode)
        val timeline = SurahTimeline(entries.map { timings.durationMs(it.file.id) ?: return emptyList() })
        return (1..QuranMeta.ayahCount(surah)).map { ayah ->
            timeline.positionOf(entries.indexOfFirst { it.id.ayah == ayah }, 0)
        }
    }

    private data class Status(
        val error: String?,
        val dismissed: String?,
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
            surahName = nowPlaying?.let { names[it.surah] },
            ayahArabic = ayah?.arabic,
            ayahTranslation = nowPlaying?.mode?.translation
                ?.takeIf { reading.showTranslation }
                ?.let { track -> ayah?.translation(track) },
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

    /** Persists [mode] and continues the playing ayah in it. */
    fun setMode(mode: RecitationMode) {
        val current = player.nowPlaying.value ?: return
        viewModelScope.launch {
            settings.setMode(mode)
            player.play(current.surah, current.ayah, mode)
        }
    }

    fun setRepeat(repeat: RepeatSetting) = player.setRepeat(repeat)

    fun setSpeed(speed: PlaybackSpeed) = player.setSpeed(speed)

    /** Starts a sleep timer, or cancels the running one with null. */
    fun setSleepTimer(option: SleepOption?) = player.setSleepTimer(option)
}

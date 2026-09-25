package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track
import dev.sadakat.qit.core.domain.player.NowPlaying
import dev.sadakat.qit.core.domain.player.PlaybackError
import dev.sadakat.qit.core.domain.player.PlaybackProgress
import dev.sadakat.qit.core.domain.player.PlaybackSpeed
import dev.sadakat.qit.core.domain.player.QuranPlayer
import dev.sadakat.qit.core.domain.player.RepeatSetting
import dev.sadakat.qit.core.domain.player.SleepOption
import dev.sadakat.qit.core.domain.player.SleepTimerStatus
import dev.sadakat.qit.core.domain.player.WordPointer
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [QuranPlayer] that records commands and simulates the resulting state: [play] starts at the
 * requested ayah (with repeat off), [nextAyah]/[previousAyah] move by one, [togglePlayPause] flips
 * `isPlaying`, [setRepeat]/[setSpeed] show up in [nowPlaying], [seekTo] moves [progress], and the
 * sleep timer reports a countdown that tests can drive through [sleepTimer].
 */
@Suppress("TooManyFunctions") // One override per command of the port it fakes.
class FakeQuranPlayer : QuranPlayer {

    data class PlayCall(val surah: Int, val fromAyah: Int, val mode: RecitationMode, val playWhenReady: Boolean = true)

    data class PlayFromWordCall(val surah: Int, val ayah: Int, val word: Int, val mode: RecitationMode)

    data class RepeatAyahCall(val surah: Int, val ayah: Int, val mode: RecitationMode, val times: Int?)

    override val nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val error = MutableStateFlow<PlaybackError?>(null)
    override val sleepTimer = MutableStateFlow<SleepTimerStatus>(SleepTimerStatus.Off)
    override val progress = MutableStateFlow(PlaybackProgress.START)
    override val pointer = MutableStateFlow<WordPointer>(WordPointer.Off)

    val playCalls = mutableListOf<PlayCall>()
    val playFromWordCalls = mutableListOf<PlayFromWordCall>()
    val repeatAyahCalls = mutableListOf<RepeatAyahCall>()
    val seekCalls = mutableListOf<Long>()
    var restoreCalls = 0
        private set
    var retryCalls = 0
        private set

    /** The speed of the next [play]; [setSpeed] changes it like the real player does. */
    var speed = PlaybackSpeed.X1
        private set

    /** The last timer started and not cancelled. */
    var sleepOption: SleepOption? = null
        private set

    override fun play(surah: Int, fromAyah: Int, mode: RecitationMode, playWhenReady: Boolean) {
        playCalls += PlayCall(surah, fromAyah, mode, playWhenReady)
        nowPlaying.value =
            NowPlaying(
                surah,
                fromAyah,
                Track.ARABIC,
                mode,
                isPlaying = playWhenReady,
                isBuffering = false,
                speed = speed,
            )
    }

    /** Plays from that word's ayah, like [play] there. */
    override fun playFromWord(surah: Int, ayah: Int, word: Int, mode: RecitationMode) {
        playFromWordCalls += PlayFromWordCall(surah, ayah, word, mode)
        play(surah, ayah, mode)
    }

    /** Plays that ayah, like [play], with its repeat on. */
    override fun repeatAyah(surah: Int, ayah: Int, mode: RecitationMode, times: Int?) {
        repeatAyahCalls += RepeatAyahCall(surah, ayah, mode, times)
        play(surah, ayah, mode)
        nowPlaying.value = nowPlaying.value?.copy(repeat = RepeatSetting.Range(ayah, ayah, times))
    }

    override fun retry() {
        retryCalls++
        error.value = null
    }

    override fun togglePlayPause() {
        nowPlaying.value = nowPlaying.value?.let { it.copy(isPlaying = !it.isPlaying) }
    }

    override fun nextAyah() {
        nowPlaying.value = nowPlaying.value?.let { it.copy(ayah = it.ayah + 1, track = Track.ARABIC) }
    }

    override fun previousAyah() {
        nowPlaying.value =
            nowPlaying.value?.let { it.copy(ayah = (it.ayah - 1).coerceAtLeast(0), track = Track.ARABIC) }
    }

    override fun seekTo(surahPositionMs: Long) {
        seekCalls += surahPositionMs
        progress.value = progress.value.copy(surahPositionMs = surahPositionMs)
    }

    override fun stop() {
        nowPlaying.value = null
        setSleepTimer(null)
    }

    override fun restoreLast(playWhenReady: Boolean) {
        restoreCalls++
    }

    override fun setRepeat(repeat: RepeatSetting) {
        nowPlaying.value = nowPlaying.value?.copy(repeat = repeat)
    }

    override fun setSpeed(speed: PlaybackSpeed) {
        this.speed = speed
        nowPlaying.value = nowPlaying.value?.copy(speed = speed)
    }

    override fun setSleepTimer(option: SleepOption?) {
        sleepOption = option
        sleepTimer.value = when (option) {
            null -> SleepTimerStatus.Off
            is SleepOption.Minutes -> SleepTimerStatus.Counting(option.minutes * MS_PER_MINUTE)
            SleepOption.EndOfSurah -> SleepTimerStatus.EndOfSurah
        }
    }

    private companion object {
        const val MS_PER_MINUTE = 60_000L
    }
}

package dev.sadakat.qandeel.core.domain.player

/** Recitation speed; the pitch stays natural at every speed. */
enum class PlaybackSpeed(val factor: Float) {
    X0_75(0.75f),
    X1(1f),
    X1_25(1.25f),
    X1_5(1.5f),
}

/** What plays again, for memorizing. The basmala (ayah 0) never repeats. */
sealed interface RepeatSetting {
    data object Off : RepeatSetting

    /** Every ayah plays [times] times in a row before the next one; null = the current ayah forever. */
    data class Ayah(val times: Int?) : RepeatSetting {
        init {
            require(times == null || times >= 2) { "An ayah repeats at least twice, got $times" }
        }
    }

    /** Ayahs [from]..[to] play in order, the whole range [times] times; null = forever. */
    data class Range(val from: Int, val to: Int, val times: Int?) : RepeatSetting {
        init {
            require(from in 1..to) { "Invalid range $from..$to" }
            require(times == null || times >= 2) { "A range repeats at least twice, got $times" }
        }
    }
}

/** When the sleep timer stops playback. */
sealed interface SleepOption {
    data class Minutes(val minutes: Int) : SleepOption {
        init {
            require(minutes > 0) { "Sleep timer needs a positive duration, got $minutes" }
        }
    }

    /** When the current surah finishes. */
    data object EndOfSurah : SleepOption
}

/** The sleep timer's state, as the UI shows it. */
sealed interface SleepTimerStatus {
    data object Off : SleepTimerStatus

    /** Stopping in [remainingMs]. */
    data class Counting(val remainingMs: Long) : SleepTimerStatus

    /** Stopping when the surah ends. */
    data object EndOfSurah : SleepTimerStatus

    /** The volume is fading out; stopping in [remainingMs]. */
    data class FadingOut(val remainingMs: Long) : SleepTimerStatus
}

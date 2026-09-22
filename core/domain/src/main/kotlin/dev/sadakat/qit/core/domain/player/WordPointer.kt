package dev.sadakat.qit.core.domain.player

import dev.sadakat.qit.core.domain.audio.WordTimings
import dev.sadakat.qit.core.domain.model.Track

/** Where the word pointer is in the playing ayah's text. */
sealed interface WordPointer {

    /** Nothing to point at (nothing plays, or the words' timings are unknown): plain text. */
    data object Off : WordPointer

    /**
     * The Arabic is recited; [word] is the word being recited (in
     * [dev.sadakat.qit.core.domain.model.ArabicWords] order), -1 before the first.
     */
    data class Reciting(val word: Int) : WordPointer

    /** The ayah's translation is being read: the Arabic steps back, the translation comes forward. */
    data object Translating : WordPointer

    companion object {
        /** The pointer for [nowPlaying] at [progress], given the playing surah's word [timings] by ayah. */
        fun of(nowPlaying: NowPlaying?, progress: PlaybackProgress, timings: Map<Int, WordTimings>): WordPointer =
            when {
                nowPlaying == null -> Off
                nowPlaying.track != Track.ARABIC -> Translating
                else -> timings[nowPlaying.ayah]?.let { Reciting(it.wordAt(progress.itemPositionMs)) } ?: Off
            }
    }
}

package dev.sadakat.qit.core.domain.model

import dev.sadakat.qit.core.domain.repository.ListeningCounts

/**
 * How far one surah has been listened to. A **round** is the whole surah heard once, in any order and
 * over any number of sessions: [rounds] is how many times every one of its ayahs has been heard, and
 * [ayahsIntoNextRound] how many have been heard once more than that.
 */
data class SurahListening(
    val surah: Int,
    val ayahCount: Int,
    val rounds: Int,
    val ayahsIntoNextRound: Int,
    /** Ayahs heard at least once. */
    val ayahsHeard: Int,
    /** Every ayah listen in the surah, summed. */
    val totalListens: Int,
    /** When it was last heard (epoch ms); null if never. */
    val lastHeardAt: Long?,
    val listenedMs: Long,
) {
    /** How far the round in progress has come, 0..1 (0 right after a round completes). */
    val nextRoundProgress: Float get() = ayahsIntoNextRound.toFloat() / ayahCount

    val isHeard: Boolean get() = totalListens > 0
}

/** The whole Quran's listening. */
data class QuranListening(
    /** Ayahs heard at least once, of [QuranMeta.TOTAL_AYAHS]. */
    val ayahsHeard: Int,
    /** Times the whole Quran has been heard (every ayah at least this many times). */
    val rounds: Int,
    val listenedMs: Long,
    /** Every surah heard at least once, by number. */
    val surahs: List<SurahListening>,
) {
    /** Share of the Quran's ayahs heard at least once, 0..1. */
    val coverage: Float get() = ayahsHeard.toFloat() / QuranMeta.TOTAL_AYAHS
}

/** How the Progress screen orders the surahs. */
enum class ListeningOrder { RECENT, MOST_HEARD, BY_NUMBER }

/** Listening progress, derived from [ListeningCounts]. Pure. */
object ListeningProgress {

    fun surah(counts: ListeningCounts, surah: Int): SurahListening {
        val ayahCount = QuranMeta.ayahCount(surah)
        var rounds = Int.MAX_VALUE
        var heard = 0
        var total = 0
        for (ayah in 1..ayahCount) {
            val count = counts.count(surah, ayah)
            rounds = minOf(rounds, count)
            if (count > 0) heard++
            total += count
        }
        val intoNext = (1..ayahCount).count { counts.count(surah, it) > rounds }
        return SurahListening(
            surah = surah,
            ayahCount = ayahCount,
            rounds = rounds,
            ayahsIntoNextRound = intoNext,
            ayahsHeard = heard,
            totalListens = total,
            lastHeardAt = counts.lastHeardAt[surah],
            listenedMs = counts.listenedMs[surah] ?: 0L,
        )
    }

    fun quran(counts: ListeningCounts): QuranListening {
        val surahs = (1..QuranMeta.SURAH_COUNT).map { surah(counts, it) }
        return QuranListening(
            ayahsHeard = surahs.sumOf { it.ayahsHeard },
            rounds = surahs.minOf { it.rounds },
            listenedMs = counts.listenedMs.values.sum(),
            surahs = surahs.filter { it.isHeard },
        )
    }

    /** [surahs] in [order]: last heard first, most listens first (ties by number), or by number. */
    fun sorted(surahs: List<SurahListening>, order: ListeningOrder): List<SurahListening> = when (order) {
        ListeningOrder.RECENT -> surahs.sortedWith(
            compareByDescending<SurahListening> {
                it.lastHeardAt ?: 0L
            }.thenBy { it.surah },
        )

        ListeningOrder.MOST_HEARD -> surahs.sortedWith(
            compareByDescending<SurahListening> {
                it.totalListens
            }.thenBy { it.surah },
        )

        ListeningOrder.BY_NUMBER -> surahs.sortedBy { it.surah }
    }
}

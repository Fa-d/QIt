package dev.sadakat.qit.core.domain.repository

import dev.sadakat.qit.core.domain.model.Ayah
import dev.sadakat.qit.core.domain.model.Surah

/** Quran text and surah metadata, available offline. */
interface QuranText {

    /** All 114 surahs, in order. */
    suspend fun surahs(): List<Surah>

    /** @throws IllegalArgumentException if [number] is not in 1..114 */
    suspend fun surah(number: Int): Surah

    /** Every ayah of [surah], in order, with Arabic, English and Bangla text. */
    suspend fun ayahs(surah: Int): List<Ayah>
}

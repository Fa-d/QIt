package dev.sadakat.qit.core.domain.repository

import dev.sadakat.qit.core.domain.model.WordByWord

/** The meaning of each Arabic word of the Quran, available offline. */
interface WordMeanings {

    /**
     * The meanings of [surah]'s words in [language], by ayah: one per word of the ayah's Arabic as
     * [dev.sadakat.qit.core.domain.model.ArabicWords] splits it. Ayah 0 — the basmala shown before
     * verse 1 — has 1:1's meanings. Empty for [WordByWord.OFF].
     */
    suspend fun meanings(surah: Int, language: WordByWord): Map<Int, List<String>>
}

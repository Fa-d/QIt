package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.WordByWord
import dev.sadakat.qit.core.domain.repository.WordMeanings

/**
 * [WordMeanings] from a map tests fill in: [bySurah] by surah, then ayah, for any language but OFF;
 * [requested] records each call.
 */
class FakeWordMeanings(var bySurah: Map<Int, Map<Int, List<String>>> = emptyMap()) : WordMeanings {
    val requested = mutableListOf<Pair<Int, WordByWord>>()
    override suspend fun meanings(surah: Int, language: WordByWord): Map<Int, List<String>> {
        requested += surah to language
        return if (language == WordByWord.OFF) emptyMap() else bySurah[surah].orEmpty()
    }
}

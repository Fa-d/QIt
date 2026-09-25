package dev.sadakat.qit.core.testing

import dev.sadakat.qit.core.domain.model.WordByWord
import dev.sadakat.qit.core.domain.repository.WordMeanings

/**
 * [WordMeanings] from maps tests fill in: [byLanguage] by language, then surah, then ayah (it wins
 * for the languages it covers), [bySurah] by surah, then ayah, for any other language but OFF;
 * [requested] records each call.
 */
class FakeWordMeanings(
    var bySurah: Map<Int, Map<Int, List<String>>> = emptyMap(),
    var byLanguage: Map<WordByWord, Map<Int, Map<Int, List<String>>>> = emptyMap(),
) : WordMeanings {
    val requested = mutableListOf<Pair<Int, WordByWord>>()
    override suspend fun meanings(surah: Int, language: WordByWord): Map<Int, List<String>> {
        requested += surah to language
        return if (language == WordByWord.OFF) {
            emptyMap()
        } else {
            byLanguage[language]?.get(surah) ?: bySurah[surah].orEmpty()
        }
    }
}

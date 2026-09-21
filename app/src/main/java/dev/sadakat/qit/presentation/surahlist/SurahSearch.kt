package dev.sadakat.qit.presentation.surahlist

import dev.sadakat.qit.core.domain.model.Surah
import java.util.Locale

/**
 * Surah search that tolerates how people actually type surah names: transliterations vary
 * (Ikhlas/Ikhlaas, Yasin/Yaseen, Baqarah/Baqara) and Arabic is typed without the diacritics the
 * bundled names carry (الفاتحة vs سُورَةُ ٱلْفَاتِحَةِ). Query and names go through the same
 * normalization, then a substring match. A number only matches that surah.
 */
internal object SurahSearch {

    /** Harakat, Quranic annotation marks, superscript alef and tatweel. */
    private val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val alefVariants = Regex("[\\u0622\\u0623\\u0625\\u0671]")

    fun matches(surah: Surah, rawQuery: String): Boolean {
        val query = rawQuery.trim()
        if (query.isEmpty()) return true
        query.toIntOrNull()?.let { return it == surah.number }

        val latinQuery = latin(query)
        val arabicQuery = arabic(query)
        val latinMatch = latinQuery.isNotEmpty() &&
            (latin(surah.nameEnglish).contains(latinQuery) || latin(surah.meaningEnglish).contains(latinQuery))
        val arabicMatch = arabicQuery.isNotEmpty() && arabic(surah.nameArabic).contains(arabicQuery)
        return latinMatch || arabicMatch
    }

    /** Lower case letters only, long vowels folded (ee→i, oo→u, doubled letters once), final vowel+h dropped. */
    private fun latin(text: String): String {
        val letters = text.lowercase(Locale.ROOT)
            .filter { it in 'a'..'z' }
            .replace("ee", "i")
            .replace("oo", "u")
        val collapsed = buildString { for (c in letters) if (isEmpty() || last() != c) append(c) }
        val endsInVowelH = collapsed.length > 1 && collapsed.last() == 'h' && collapsed[collapsed.length - 2] in "aeiou"
        return if (endsInVowelH) collapsed.dropLast(1) else collapsed
    }

    /** Arabic letters only: marks removed, alef forms unified, ta marbuta → ha, alef maqsura → ya. */
    private fun arabic(text: String): String = arabicMarks.replace(text, "")
        .replace(alefVariants, "ا")
        .replace('ة', 'ه')
        .replace('ى', 'ي')
        .filter { it in 'ء'..'ي' }
}

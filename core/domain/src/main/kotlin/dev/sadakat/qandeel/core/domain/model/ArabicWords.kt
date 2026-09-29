package dev.sadakat.qandeel.core.domain.model

/**
 * The words of an ayah's Arabic as the word pointer moves over them: the text split on whitespace,
 * where a pause mark standing on its own (ۖ ۗ ۘ ۙ ۚ ۛ ۜ, ۞, ۩) belongs to the word before it — or to
 * the word after it when it opens the ayah (۞ often does). The bundled word timings count words the
 * same way (`scripts/build_audio_timing.py`).
 */
object ArabicWords {

    private const val FIRST_PAUSE_MARK = 'ۖ'
    private const val LAST_PAUSE_MARK = 'ۜ'
    private const val RUB_EL_HIZB = '۞'
    private const val SAJDAH = '۩'

    /** Each word's character range in [text]; a pause mark is inside its word's range. */
    fun ranges(text: String): List<IntRange> {
        val words = mutableListOf<IntRange>()
        var leadingMarksStart: Int? = null
        for (token in tokens(text)) {
            val isMark = isPauseMark(text, token)
            when {
                !isMark -> {
                    words += (leadingMarksStart ?: token.first)..token.last
                    leadingMarksStart = null
                }

                words.isEmpty() -> if (leadingMarksStart == null) leadingMarksStart = token.first

                else -> words[words.lastIndex] = words.last().first..token.last
            }
        }
        return words
    }

    /** How many words [text] has. */
    fun count(text: String): Int = ranges(text).size

    /** The whitespace-separated tokens of [text], as character ranges. */
    private fun tokens(text: String): List<IntRange> {
        val tokens = mutableListOf<IntRange>()
        var start = -1
        for (i in text.indices) {
            if (text[i].isWhitespace()) {
                if (start >= 0) tokens += start until i
                start = -1
            } else if (start < 0) {
                start = i
            }
        }
        if (start >= 0) tokens += start until text.length
        return tokens
    }

    /** A token made only of pause marks (and the combining marks that may ride on them). */
    private fun isPauseMark(text: String, token: IntRange): Boolean {
        var hasMark = false
        for (i in token) {
            val ch = text[i]
            when {
                ch.isPauseMark() -> hasMark = true
                !ch.isCombiningMark() -> return false
            }
        }
        return hasMark
    }

    private fun Char.isPauseMark() = this in FIRST_PAUSE_MARK..LAST_PAUSE_MARK || this == RUB_EL_HIZB || this == SAJDAH

    private fun Char.isCombiningMark(): Boolean = when (category) {
        CharCategory.NON_SPACING_MARK, CharCategory.ENCLOSING_MARK, CharCategory.COMBINING_SPACING_MARK -> true
        else -> false
    }
}

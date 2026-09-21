package dev.sadakat.qit.core.domain.model

/**
 * Reads a typed verse reference such as "2:255" — the way people quote the Quran — so search can jump
 * straight to an ayah. Surah and ayah may be separated by `:`, `.`, `/` or spaces, and written in
 * Western, Arabic-Indic (٢:٢٥٥) or Bengali (২:২৫৫) digits.
 */
object AyahRefParser {

    private val reference = Regex("""^\s*(\d{1,3})\s*[:./\s]\s*(\d{1,3})\s*$""")

    /** The ayah [text] refers to, or null when it isn't a reference to an existing ayah. */
    fun parse(text: String): AyahRef? {
        val match = reference.matchEntire(westernDigits(text)) ?: return null
        val surah = match.groupValues[1].toInt()
        val ayah = match.groupValues[2].toInt()
        if (surah !in 1..QuranMeta.SURAH_COUNT || ayah !in 1..QuranMeta.ayahCount(surah)) return null
        return AyahRef(surah, ayah)
    }

    private fun westernDigits(text: String): String = buildString(text.length) {
        for (c in text) {
            val zero = DIGIT_ZEROS.firstOrNull { c - it in 0..LAST_DIGIT }
            append(if (zero == null) c else '0' + (c - zero))
        }
    }

    /** The zero of each digit set people type verse numbers in; each set's digits follow its zero. */
    private val DIGIT_ZEROS = listOf(
        '\u0660', // Arabic-Indic
        '\u06F0', // Extended Arabic-Indic (Persian, Urdu)
        '\u09E6', // Bengali
    )
    private const val LAST_DIGIT = 9
}

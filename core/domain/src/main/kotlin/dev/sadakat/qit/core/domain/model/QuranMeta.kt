package dev.sadakat.qit.core.domain.model

/**
 * Fixed structure of the Quran: surah lengths and the global ayah numbering (1..6236) used by
 * every audio source (`ayah = verses of all previous surahs + verse`).
 */
object QuranMeta {
    const val SURAH_COUNT = 114
    const val TOTAL_AYAHS = 6236
    const val JUZ_COUNT = 30
    private const val AL_FATIHA = 1
    private const val AT_TAWBAH = 9

    private val AYAH_COUNTS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
        89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
        12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
        30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
    )

    /** Global number of each surah's first ayah, indexed by surah - 1. */
    private val FIRST_AYAH = IntArray(SURAH_COUNT).also { first ->
        var next = 1
        for (i in 0 until SURAH_COUNT) {
            first[i] = next
            next += AYAH_COUNTS[i]
        }
    }

    fun ayahCount(surah: Int): Int {
        require(surah in 1..SURAH_COUNT) { "Invalid surah $surah" }
        return AYAH_COUNTS[surah - 1]
    }

    fun globalAyah(surah: Int, ayah: Int): Int {
        require(ayah in 1..ayahCount(surah)) { "Invalid ayah $surah:$ayah" }
        return FIRST_AYAH[surah - 1] + ayah - 1
    }

    /** Inverse of [globalAyah]: the surah and verse of global ayah [globalAyah] (1..6236). */
    fun ayahRef(globalAyah: Int): AyahRef {
        require(globalAyah in 1..TOTAL_AYAHS) { "Invalid global ayah $globalAyah" }
        val found = FIRST_AYAH.binarySearch(globalAyah)
        // Not a surah's first ayah: binarySearch returns -(insertion point) - 1, one past its surah.
        val surahIndex = if (found >= 0) found else -(found + 1) - 1
        return AyahRef(surahIndex + 1, globalAyah - FIRST_AYAH[surahIndex] + 1)
    }

    /** Every surah except Al-Fatiha (whose verse 1 is the basmala) and At-Tawbah opens with a basmala. */
    fun hasBasmalaPrefix(surah: Int): Boolean = surah != AL_FATIHA && surah != AT_TAWBAH

    /** Where each of the 30 juz (the Quran's thirty equal-length parts) begins, as `surah, ayah` pairs. */
    private val JUZ_STARTS = intArrayOf(
        1, 1, 2, 142, 2, 253, 3, 93, 4, 24, 4, 148, 5, 82, 6, 111, 7, 88, 8, 41,
        9, 93, 11, 6, 12, 53, 15, 1, 17, 1, 18, 75, 21, 1, 23, 1, 25, 21, 27, 56,
        29, 46, 33, 31, 36, 28, 39, 32, 41, 47, 46, 1, 51, 31, 58, 1, 67, 1, 78, 1,
    )

    /** Global ayah number of each juz's first ayah, indexed by juz - 1. */
    private val JUZ_FIRST_GLOBAL = IntArray(JUZ_COUNT) { globalAyah(JUZ_STARTS[it * 2], JUZ_STARTS[it * 2 + 1]) }

    /** The first ayah of [juz] (1..30). */
    fun juzStart(juz: Int): AyahRef {
        require(juz in 1..JUZ_COUNT) { "Invalid juz $juz" }
        return AyahRef(JUZ_STARTS[(juz - 1) * 2], JUZ_STARTS[(juz - 1) * 2 + 1])
    }

    /** The juz [surah]:[ayah] belongs to; ayah 0 (a surah's basmala) counts as the surah's first ayah. */
    fun juzOf(surah: Int, ayah: Int): Int {
        val global = globalAyah(surah, ayah.coerceAtLeast(1))
        // Index of the last juz starting at or before the ayah.
        val found = JUZ_FIRST_GLOBAL.binarySearch(global)
        return if (found >= 0) found + 1 else -(found + 1)
    }
}

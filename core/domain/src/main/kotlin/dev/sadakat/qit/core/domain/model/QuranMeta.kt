package dev.sadakat.qit.core.domain.model

/**
 * Fixed structure of the Quran: surah lengths and the global ayah numbering (1..6236) used by
 * every audio source (`ayah = verses of all previous surahs + verse`).
 */
object QuranMeta {
    const val SURAH_COUNT = 114
    const val TOTAL_AYAHS = 6236

    private val AYAH_COUNTS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
        89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
        12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
        30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
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

    /** Every surah except Al-Fatiha (whose verse 1 is the basmala) and At-Tawbah opens with a basmala. */
    fun hasBasmalaPrefix(surah: Int): Boolean = surah != 1 && surah != 9
}

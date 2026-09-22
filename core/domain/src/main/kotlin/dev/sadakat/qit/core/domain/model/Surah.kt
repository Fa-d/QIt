package dev.sadakat.qit.core.domain.model

enum class Revelation { MECCAN, MEDINAN }

data class Surah(
    val number: Int,
    /** e.g. "سُورَةُ ٱلْفَاتِحَةِ" */
    val nameArabic: String,
    /** Transliterated, e.g. "Al-Faatiha" */
    val nameEnglish: String,
    /** e.g. "The Opening" */
    val meaningEnglish: String,
    val ayahCount: Int,
    val revelation: Revelation,
) {
    /**
     * The Arabic name without its leading word "سُورَةُ" (surah), e.g. "ٱلْفَاتِحَةِ": for lists and
     * cards, where the word would only repeat on every row.
     */
    val nameArabicShort: String get() = nameArabic.removePrefix(SURAH_WORD).trim()

    fun globalAyah(ayah: Int): Int = QuranMeta.globalAyah(number, ayah)

    private companion object {
        const val SURAH_WORD = "سُورَةُ"
    }
}

data class Ayah(
    val surah: Int,
    val number: Int,
    val globalNumber: Int,
    /** Uthmani script */
    val arabic: String,
    /** Saheeh International */
    val english: String,
    /** Muhiuddin Khan */
    val bangla: String,
) {
    fun translation(track: Track): String? = when (track) {
        Track.ENGLISH -> english
        Track.BANGLA -> bangla
        Track.ARABIC -> null
    }
}

/** A position in the Quran. [ayah] 0 means the basmala played before verse 1. */
data class AyahRef(val surah: Int, val ayah: Int)

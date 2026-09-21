package dev.sadakat.qit.core.domain.model

/** One audio recording of the Quran, verse by verse. [code] is stable: used in media ids, download ids and messages. */
enum class Track(val code: String, val label: String) {
    ARABIC("ar", "Arabic"),
    ENGLISH("en", "English"),
    BANGLA("bn", "Bangla");

    companion object {
        fun fromCode(code: String): Track? = entries.firstOrNull { it.code == code }
    }
}

/** What plays for each ayah, in order. */
enum class RecitationMode(val tracks: List<Track>, val label: String) {
    ARABIC_ONLY(listOf(Track.ARABIC), "Arabic"),
    ARABIC_ENGLISH(listOf(Track.ARABIC, Track.ENGLISH), "Arabic + English"),
    ARABIC_BANGLA(listOf(Track.ARABIC, Track.BANGLA), "Arabic + Bangla");

    /** The translation shown and played after the Arabic, or null for Arabic only. */
    val translation: Track? get() = tracks.firstOrNull { it != Track.ARABIC }
}

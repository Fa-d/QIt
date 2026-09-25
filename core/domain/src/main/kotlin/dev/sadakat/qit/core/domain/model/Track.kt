package dev.sadakat.qit.core.domain.model

/** The language a [Track] is spoken in. */
enum class Language { ARABIC, ENGLISH, BANGLA }

/** One audio recording of the Quran, verse by verse. [code] is stable: used in media ids, download ids and messages. */
enum class Track(val code: String, val language: Language) {
    /** Mishary Alafasy. */
    ARABIC("ar", Language.ARABIC),

    /** Saheeh International, read by Ibrahim Walk. */
    ENGLISH("en", Language.ENGLISH),

    /** Islamic Foundation's translation, cut from the Alafasy + Bangla recording. */
    BANGLA("bn", Language.BANGLA),

    /** Abdul Basit, mujawwad: the Arabic of Sayed Ismat Toha's recording. */
    ARABIC_BASIT_MUJAWWAD("ar.basit", Language.ARABIC),

    /** Sayed Ismat Toha reading Hafiz Munir Uddin Ahmad's translation, cut from his recording with Abdul Basit. */
    BANGLA_TOHA("bn.toha", Language.BANGLA),

    /** Abdur-Rahman As-Sudais: the reciter of Shareef Baezeed Mahmood's recording. */
    ARABIC_SUDAIS("ar.sudais", Language.ARABIC),

    /** Shareef Baezeed Mahmood, cut from his recording with Sudais. */
    BANGLA_BAEZEED("bn.baezeed", Language.BANGLA),
    ;

    val isArabic: Boolean get() = language == Language.ARABIC

    companion object {
        fun fromCode(code: String): Track? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Who reads the Bangla in [RecitationMode.ARABIC_BANGLA], with the Arabic reciter of their recording,
 * which plays with them. [code] is stable: it is stored in the settings and sent to the watch.
 */
enum class BanglaVoice(val code: String, val arabic: Track, val bangla: Track) {
    ISLAMIC_FOUNDATION("if", Track.ARABIC, Track.BANGLA),
    SAYED_ISMAT_TOHA("toha", Track.ARABIC_BASIT_MUJAWWAD, Track.BANGLA_TOHA),
    SHAREEF_BAEZEED_MAHMOOD("baezeed", Track.ARABIC_SUDAIS, Track.BANGLA_BAEZEED),
    ;

    companion object {
        val DEFAULT = ISLAMIC_FOUNDATION

        fun fromCode(code: String): BanglaVoice? = entries.firstOrNull { it.code == code }
    }
}

/** What plays for each ayah, in order. */
enum class RecitationMode(val label: String) {
    ARABIC_ONLY("Arabic"),
    ARABIC_ENGLISH("Arabic + English"),
    ARABIC_BANGLA("Arabic + Bangla"),
    ;

    /** The recordings played for each ayah, in order; [voice] picks the pair for Arabic + Bangla. */
    fun tracks(voice: BanglaVoice): List<Track> = when (this) {
        ARABIC_ONLY -> listOf(Track.ARABIC)
        ARABIC_ENGLISH -> listOf(Track.ARABIC, Track.ENGLISH)
        ARABIC_BANGLA -> listOf(voice.arabic, voice.bangla)
    }

    /** Whose text is shown as the translation ([Track.ENGLISH] or [Track.BANGLA]), or null for Arabic only. */
    val translation: Track?
        get() = when (this) {
            ARABIC_ONLY -> null
            ARABIC_ENGLISH -> Track.ENGLISH
            ARABIC_BANGLA -> Track.BANGLA
        }
}

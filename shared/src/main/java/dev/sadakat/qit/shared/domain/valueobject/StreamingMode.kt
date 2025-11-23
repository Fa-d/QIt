package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing the streaming mode for audio playback
 */
enum class StreamingMode {
    /**
     * Real-time streaming from phone without downloading to watch storage
     */
    REAL_TIME,

    /**
     * Progressive download - downloads chunks while playing
     */
    PROGRESSIVE,

    /**
     * Play from local storage (already downloaded)
     */
    LOCAL;

    fun requiresPhoneConnection(): Boolean {
        return this == REAL_TIME || this == PROGRESSIVE
    }

    fun usesLocalStorage(): Boolean {
        return this == PROGRESSIVE || this == LOCAL
    }
}

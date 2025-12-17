package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing the default playback destination
 */
enum class PlaybackDestination(
    val displayName: String,
    val description: String
) {
    PHONE("Phone", "Play music on this device"),
    WATCH("Watch", "Stream music to connected watch");

    companion object {
        fun fromString(value: String): PlaybackDestination {
            return when (value.uppercase()) {
                "PHONE" -> PHONE
                "WATCH" -> WATCH
                else -> PHONE // Default to phone
            }
        }
    }
}

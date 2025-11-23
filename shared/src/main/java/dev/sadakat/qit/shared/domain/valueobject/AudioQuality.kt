package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing audio quality settings
 */
enum class AudioQuality(
    val bitrate: Int, // kbps
    val displayName: String
) {
    LOW(64, "Low (64 kbps)"),
    MEDIUM(128, "Medium (128 kbps)"),
    HIGH(256, "High (256 kbps)"),
    ORIGINAL(0, "Original Quality");

    fun isTranscodingRequired(originalBitrate: Int): Boolean {
        return this != ORIGINAL && originalBitrate > this.bitrate
    }

    fun estimateFileSize(originalSize: FileSize, originalBitrate: Int): FileSize {
        if (this == ORIGINAL || originalBitrate == 0) {
            return originalSize
        }

        // Estimate based on bitrate ratio
        val ratio = this.bitrate.toDouble() / originalBitrate.toDouble()
        val estimatedBytes = (originalSize.bytes * ratio).toLong()
        return FileSize.fromBytes(estimatedBytes)
    }

    companion object {
        fun fromBitrate(bitrate: Int): AudioQuality {
            return when {
                bitrate == 0 -> ORIGINAL
                bitrate <= 64 -> LOW
                bitrate <= 128 -> MEDIUM
                bitrate <= 256 -> HIGH
                else -> ORIGINAL
            }
        }
    }
}

package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing a file size in bytes
 */
data class FileSize private constructor(val bytes: Long) {

    init {
        require(bytes >= 0) { "File size cannot be negative" }
    }

    val kilobytes: Double get() = bytes / 1024.0
    val megabytes: Double get() = kilobytes / 1024.0
    val gigabytes: Double get() = megabytes / 1024.0

    fun format(): String {
        return when {
            gigabytes >= 1.0 -> String.format("%.2f GB", gigabytes)
            megabytes >= 1.0 -> String.format("%.2f MB", megabytes)
            kilobytes >= 1.0 -> String.format("%.2f KB", kilobytes)
            else -> "$bytes B"
        }
    }

    operator fun plus(other: FileSize): FileSize {
        return FileSize(this.bytes + other.bytes)
    }

    operator fun compareTo(other: FileSize): Int {
        return this.bytes.compareTo(other.bytes)
    }

    companion object {
        val ZERO = FileSize(0L)

        fun fromBytes(bytes: Long): FileSize {
            return FileSize(bytes)
        }

        fun fromKilobytes(kb: Long): FileSize {
            return FileSize(kb * 1024)
        }

        fun fromMegabytes(mb: Long): FileSize {
            return FileSize(mb * 1024 * 1024)
        }
    }
}

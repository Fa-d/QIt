package dev.sadakat.qit.shared.domain.valueobject

/**
 * Value Object representing a duration of time
 */
data class Duration private constructor(val milliseconds: Long) {

    init {
        require(milliseconds >= 0) { "Duration cannot be negative" }
    }

    val seconds: Long get() = milliseconds / 1000
    val minutes: Long get() = seconds / 60
    val hours: Long get() = minutes / 60

    fun format(): String {
        val hrs = hours
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hrs > 0) {
            String.format("%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%d:%02d", mins, secs)
        }
    }

    operator fun plus(other: Duration): Duration {
        return Duration(this.milliseconds + other.milliseconds)
    }

    operator fun compareTo(other: Duration): Int {
        return this.milliseconds.compareTo(other.milliseconds)
    }

    companion object {
        val ZERO = Duration(0L)

        fun fromMilliseconds(millis: Long): Duration {
            return Duration(millis)
        }

        fun fromSeconds(seconds: Long): Duration {
            return Duration(seconds * 1000)
        }

        fun fromMinutes(minutes: Long): Duration {
            return Duration(minutes * 60 * 1000)
        }
    }
}

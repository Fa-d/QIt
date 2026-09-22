package dev.sadakat.qit.core.data.link

import dev.sadakat.qit.core.domain.repository.ListeningSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Watch → phone on [WearPaths.LISTENING]: everything the watch heard itself. */
@Serializable
data class ListeningSnapshotMessage(
    val resetAt: Long,
    /** Times heard, by global ayah. */
    val counts: Map<Int, Int>,
    /** Last heard (epoch ms), by surah. */
    val lastHeardAt: Map<Int, Long>,
    /** Listening time (ms), by surah. */
    val listenedMs: Map<Int, Long>,
) {
    fun toSnapshot() = ListeningSnapshot(resetAt, counts, lastHeardAt, listenedMs)

    fun toBytes(): ByteArray = Json.encodeToString(serializer(), this).encodeToByteArray()

    companion object {
        fun of(snapshot: ListeningSnapshot) = ListeningSnapshotMessage(
            resetAt = snapshot.resetAt,
            counts = snapshot.ayahCounts,
            lastHeardAt = snapshot.lastHeardAt,
            listenedMs = snapshot.listenedMs,
        )

        /** @throws IllegalArgumentException if [bytes] are not a snapshot. */
        fun fromBytes(bytes: ByteArray): ListeningSnapshotMessage =
            Json.decodeFromString(serializer(), bytes.decodeToString())
    }
}

/** Phone → watch on [WearPaths.LISTENING_RESET]: forget everything heard before [resetAt] (epoch ms). */
@Serializable
data class ListeningResetMessage(val resetAt: Long) {

    fun toBytes(): ByteArray = Json.encodeToString(serializer(), this).encodeToByteArray()

    companion object {
        /** @throws IllegalArgumentException if [bytes] are not a reset. */
        fun fromBytes(bytes: ByteArray): ListeningResetMessage =
            Json.decodeFromString(serializer(), bytes.decodeToString())
    }
}

package dev.sadakat.qit.core.data.link

import dev.sadakat.qit.core.domain.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Phone → watch on [WearPaths.QURAN_DOWNLOAD]: download a surah on the watch. */
@Serializable
data class QuranDownloadMessage(val surah: Int, val trackCodes: List<String>) {

    val tracks: List<Track> get() = trackCodes.mapNotNull { Track.fromCode(it) }

    fun toBytes(): ByteArray = Json.encodeToString(serializer(), this).encodeToByteArray()

    companion object {
        fun of(surah: Int, tracks: List<Track>) = QuranDownloadMessage(surah, tracks.map { it.code })

        fun fromBytes(bytes: ByteArray): QuranDownloadMessage =
            Json.decodeFromString(serializer(), bytes.decodeToString())
    }
}

package dev.sadakat.qit.shared.quran.link

import dev.sadakat.qit.shared.quran.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Phone → watch on [dev.sadakat.qit.shared.constants.WearPaths.QURAN_DOWNLOAD]: download a surah on the watch. */
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

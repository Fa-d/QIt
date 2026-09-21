package dev.sadakat.qit.shared.quran.audio

import androidx.media3.common.MediaItem
import dev.sadakat.qit.shared.quran.model.RecitationMode
import dev.sadakat.qit.shared.quran.model.Surah
import dev.sadakat.qit.shared.quran.model.Track

/**
 * Turns a surah + recitation mode into the player's queue: an optional basmala prefix (ayah 0),
 * then, for each ayah, one item per track of the mode, in the mode's order.
 * Every item's media id is [mediaId], so the current ayah can always be read back with [parse].
 */
object QuranQueueBuilder {

    data class QueueItemId(val surah: Int, val ayah: Int, val track: Track)

    fun mediaId(surah: Int, ayah: Int, track: Track): String = "$surah:$ayah:${track.code}"

    fun parse(mediaId: String?): QueueItemId? {
        val parts = mediaId?.split(':') ?: return null
        if (parts.size != 3) return null
        val surah = parts[0].toIntOrNull() ?: return null
        val ayah = parts[1].toIntOrNull() ?: return null
        val track = Track.fromCode(parts[2]) ?: return null
        return QueueItemId(surah, ayah, track)
    }

    fun build(surah: Surah, mode: RecitationMode): List<MediaItem> = TODO("W2b")

    /** Index of the first item of [ayah] (0 = basmala) in a queue made by [build]; 0 if absent. */
    fun indexOf(items: List<MediaItem>, ayah: Int): Int = TODO("W2b")
}

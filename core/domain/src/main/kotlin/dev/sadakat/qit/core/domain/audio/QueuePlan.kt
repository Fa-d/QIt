package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track

/** Identity of one queue item: which surah, which ayah (0 = basmala) and which recording. */
data class QueueItemId(val surah: Int, val ayah: Int, val track: Track) {

    /** Stable media id of the item, `"{surah}:{ayah}:{trackCode}"`. */
    fun toMediaId(): String = "$surah:$ayah:${track.code}"

    companion object {
        /** Inverse of [toMediaId]; null for anything that is not a Quran queue item. */
        fun parse(mediaId: String?): QueueItemId? {
            val parts = mediaId?.split(':') ?: return null
            if (parts.size != 3) return null
            val surah = parts[0].toIntOrNull() ?: return null
            val ayah = parts[1].toIntOrNull() ?: return null
            val track = Track.fromCode(parts[2]) ?: return null
            return QueueItemId(surah, ayah, track)
        }
    }
}

data class QueueEntry(val id: QueueItemId, val file: QuranAudioUrls.AudioFile)

/**
 * The play queue of a surah and how to move through it by ayah. Pure logic: the player adapter in
 * :core:data turns entries into media items and applies the indexes computed here.
 */
object QueuePlan {

    /**
     * The queue for [surah] in [mode]: a basmala prefix (ayah 0) for surahs with one, then for each
     * ayah one entry per track of [mode], in the mode's order.
     * Basmala prefix: Arabic only → ar basmala; Arabic + English → ar then en basmala;
     * Arabic + Bangla → only the Bangla intro (it already contains the Arabic basmala).
     */
    fun plan(surah: Int, mode: RecitationMode): List<QueueEntry> = TODO("W2b")

    /** Index of the first item of [ayah] (0 = basmala); 0 if the queue has no such ayah. */
    fun indexOfAyah(queue: List<QueueItemId>, ayah: Int): Int = TODO("W2b")

    /** Index of the first item of the ayah after the one at [currentIndex]; null at the last ayah. */
    fun nextAyahIndex(queue: List<QueueItemId>, currentIndex: Int): Int? = TODO("W2b")

    /**
     * Where "previous" goes from [currentIndex] at [positionMs] into the current item: the start of the
     * current ayah if we are past its first item or more than 3 s in, else the first item of the
     * previous ayah; null if already at the very start.
     */
    fun previousAyahIndex(queue: List<QueueItemId>, currentIndex: Int, positionMs: Long): Int? = TODO("W2b")
}

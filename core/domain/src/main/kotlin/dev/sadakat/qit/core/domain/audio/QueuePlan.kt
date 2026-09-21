package dev.sadakat.qit.core.domain.audio

import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Track

/** Identity of one queue item: which surah, which ayah (0 = basmala) and which recording. */
data class QueueItemId(val surah: Int, val ayah: Int, val track: Track) {

    /** Stable media id of the item, `"{surah}:{ayah}:{trackCode}"`. */
    fun toMediaId(): String = "$surah:$ayah:${track.code}"

    companion object {
        private const val MEDIA_ID_PARTS = 3

        /** Inverse of [toMediaId]; null for anything that is not a Quran queue item. */
        fun parse(mediaId: String?): QueueItemId? {
            val parts = mediaId?.split(':') ?: return null
            if (parts.size != MEDIA_ID_PARTS) return null
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

    /** After this much of an item, "previous" restarts the ayah instead of leaving it. */
    private const val RESTART_AYAH_AFTER_MS = 3_000L

    /**
     * The queue for [surah] in [mode]: a basmala prefix (ayah 0) for surahs with one, then for each
     * ayah one entry per track of [mode], in the mode's order.
     * Basmala prefix: Arabic only → ar basmala; Arabic + English → ar then en basmala;
     * Arabic + Bangla → only the Bangla intro (it already contains the Arabic basmala).
     */
    fun plan(surah: Int, mode: RecitationMode): List<QueueEntry> {
        val prefixTracks = when (mode) {
            RecitationMode.ARABIC_ONLY -> listOf(Track.ARABIC)
            RecitationMode.ARABIC_ENGLISH -> listOf(Track.ARABIC, Track.ENGLISH)
            // The Bangla intro already contains the Arabic basmala, so no separate Arabic prefix.
            RecitationMode.ARABIC_BANGLA -> listOf(Track.BANGLA)
        }
        val prefix = if (QuranMeta.hasBasmalaPrefix(surah)) {
            prefixTracks.mapNotNull { track ->
                QuranAudioUrls.basmala(track, surah)?.let { file ->
                    QueueEntry(QueueItemId(surah, ayah = 0, track), file)
                }
            }
        } else {
            emptyList()
        }
        val verses = (1..QuranMeta.ayahCount(surah)).flatMap { ayah ->
            val globalAyah = QuranMeta.globalAyah(surah, ayah)
            mode.tracks.map { track ->
                QueueEntry(QueueItemId(surah, ayah, track), QuranAudioUrls.verse(track, globalAyah))
            }
        }
        return prefix + verses
    }

    /** Index of the first item of [ayah] (0 = basmala); 0 if the queue has no such ayah. */
    fun indexOfAyah(queue: List<QueueItemId>, ayah: Int): Int =
        queue.indexOfFirst { it.ayah == ayah }.takeIf { it >= 0 } ?: 0

    /** Index of the first item of the ayah after the one at [currentIndex]; null at the last ayah. */
    fun nextAyahIndex(queue: List<QueueItemId>, currentIndex: Int): Int? {
        if (currentIndex !in queue.indices) return null
        // Ayah numbers grow monotonically along the queue, so the first item beyond the current
        // ayah is the first item of the next ayah — whatever track the current item is on.
        val ayah = queue[currentIndex].ayah
        return queue.indexOfFirst { it.ayah > ayah }.takeIf { it >= 0 }
    }

    /**
     * Where "previous" goes from [currentIndex] at [positionMs] into the current item: the start of the
     * current ayah if we are past its first item or more than 3 s in, else the first item of the
     * previous ayah; null if already at the very start.
     */
    fun previousAyahIndex(queue: List<QueueItemId>, currentIndex: Int, positionMs: Long): Int? {
        if (currentIndex !in queue.indices) return null
        val ayah = queue[currentIndex].ayah
        val firstOfCurrent = queue.indexOfFirst { it.ayah == ayah }
        if (currentIndex > firstOfCurrent || positionMs > RESTART_AYAH_AFTER_MS) return firstOfCurrent
        if (firstOfCurrent == 0) return null
        // Items of one ayah are contiguous: the item just before this ayah's first belongs to the
        // previous ayah, whose first item we then look up.
        val previousAyah = queue[firstOfCurrent - 1].ayah
        return queue.indexOfFirst { it.ayah == previousAyah }
    }
}

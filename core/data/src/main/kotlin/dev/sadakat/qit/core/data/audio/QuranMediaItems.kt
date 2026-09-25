package dev.sadakat.qit.core.data.audio

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.sadakat.qit.core.domain.audio.QueueItemId
import dev.sadakat.qit.core.domain.audio.QueuePlan
import dev.sadakat.qit.core.domain.model.BanglaVoice
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah
import dev.sadakat.qit.core.domain.model.Track

/**
 * Media3 items for a surah's queue ([QueuePlan.plan]): uri = the file's URL (also the cache key, so
 * downloaded files play offline), media id = [dev.sadakat.qit.core.domain.audio.QueueItemId.toMediaId],
 * and metadata for the media notification (e.g. title "Al-Baqarah 2:255").
 */
object QuranMediaItems {

    fun build(surah: Surah, mode: RecitationMode, voice: BanglaVoice): List<MediaItem> =
        QueuePlan.plan(surah.number, mode, voice).map { entry ->
            MediaItem.Builder()
                .setUri(entry.file.url)
                .setMediaId(entry.id.toMediaId())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title(surah, entry.id))
                        .setArtist(entry.id.track.artist)
                        .setAlbumTitle(surah.nameEnglish)
                        .build(),
                )
                .build()
        }

    private fun title(surah: Surah, id: QueueItemId): String = if (id.ayah == 0) {
        "${surah.nameEnglish} · Bismillah"
    } else {
        "${surah.nameEnglish} ${surah.number}:${id.ayah}"
    }

    /** Who the media notification credits for the item's audio. */
    private val Track.artist: String
        get() = when (this) {
            Track.ARABIC -> "Mishary Alafasy"
            Track.ENGLISH -> "Saheeh International"
            Track.BANGLA -> "Bangla translation"
            Track.ARABIC_BASIT_MUJAWWAD -> "Abdul Basit Abdus Samad"
            Track.BANGLA_TOHA -> "Sayed Ismat Toha"
            Track.ARABIC_SUDAIS -> "Abdur-Rahman As-Sudais"
            Track.BANGLA_BAEZEED -> "Shareef Baezeed Mahmood"
        }
}

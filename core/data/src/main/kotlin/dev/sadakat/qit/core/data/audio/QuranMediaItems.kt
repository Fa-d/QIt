package dev.sadakat.qit.core.data.audio

import androidx.media3.common.MediaItem
import dev.sadakat.qit.core.domain.audio.QueuePlan
import dev.sadakat.qit.core.domain.model.RecitationMode
import dev.sadakat.qit.core.domain.model.Surah

/**
 * Media3 items for a surah's queue ([QueuePlan.plan]): uri = the file's URL (also the cache key, so
 * downloaded files play offline), media id = [dev.sadakat.qit.core.domain.audio.QueueItemId.toMediaId],
 * and metadata for the media notification (e.g. title "Al-Baqarah 2:255").
 */
object QuranMediaItems {

    fun build(surah: Surah, mode: RecitationMode): List<MediaItem> = TODO("W2b")
}

package dev.sadakat.qit.core.data.listening

import android.content.Context
import dev.sadakat.qit.core.domain.model.AyahRef
import dev.sadakat.qit.core.domain.model.QuranMeta
import dev.sadakat.qit.core.domain.repository.ListeningCounts
import dev.sadakat.qit.core.domain.repository.ListeningHistory
import dev.sadakat.qit.core.domain.repository.ListeningSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * [ListeningHistory] in [QuranDatabase]. Rows are kept per source: [LOCAL] for this device, and one
 * source per synced device (the phone keeps each watch's snapshot as it last received it). The
 * counts are the sums over all sources.
 */
class RoomListeningHistory internal constructor(database: QuranDatabase) : ListeningHistory {

    private val dao = database.listeningDao()

    override val counts: Flow<ListeningCounts> = combine(dao.ayahTotals(), dao.surahTotals()) { ayahs, surahs ->
        val counts = IntArray(QuranMeta.TOTAL_AYAHS)
        for (row in ayahs) {
            if (row.globalAyah in 1..QuranMeta.TOTAL_AYAHS) counts[row.globalAyah - 1] = row.count
        }
        ListeningCounts(
            ayahCounts = counts,
            lastHeardAt = surahs.filter { it.lastHeardAt > 0 }.associate { it.surah to it.lastHeardAt },
            listenedMs = surahs.filter { it.listenedMs > 0 }.associate { it.surah to it.listenedMs },
        )
    }

    override val lastResetAt: Flow<Long> = dao.resetAt().map { it ?: 0L }

    override suspend fun recordHeard(ref: AyahRef, atMs: Long) {
        dao.recordHeard(LOCAL, ref.surah, QuranMeta.globalAyah(ref.surah, ref.ayah), atMs)
    }

    override suspend fun addListeningTime(surah: Int, ms: Long) {
        if (ms > 0) dao.addListeningTime(LOCAL, surah, ms)
    }

    override suspend fun localSnapshot(): ListeningSnapshot {
        val surahs = dao.surahListening(LOCAL)
        return ListeningSnapshot(
            resetAt = dao.currentResetAt() ?: 0L,
            ayahCounts = dao.ayahListens(LOCAL).associate { it.globalAyah to it.count },
            lastHeardAt = surahs.filter { it.lastHeardAt > 0 }.associate { it.surah to it.lastHeardAt },
            listenedMs = surahs.filter { it.listenedMs > 0 }.associate { it.surah to it.listenedMs },
        )
    }

    override suspend fun importSnapshot(source: String, snapshot: ListeningSnapshot) {
        require(source != LOCAL) { "Another device's snapshot can't replace this device's own listening" }
        // Counted from before the last reset: what it holds was deliberately forgotten.
        if (snapshot.resetAt < (dao.currentResetAt() ?: 0L)) return
        val ayahs = snapshot.ayahCounts
            .filter { (ayah, count) -> ayah in 1..QuranMeta.TOTAL_AYAHS && count > 0 }
            .map { (ayah, count) -> AyahListenEntity(source, ayah, count) }
        val surahs = (snapshot.lastHeardAt.keys + snapshot.listenedMs.keys)
            .filter { it in 1..QuranMeta.SURAH_COUNT }
            .map { surah ->
                SurahListeningEntity(source, surah, snapshot.listenedMs[surah] ?: 0L, snapshot.lastHeardAt[surah] ?: 0L)
            }
        dao.replaceSource(source, ayahs, surahs)
    }

    override suspend fun reset(atMs: Long) {
        dao.reset(atMs)
    }

    companion object {
        /** The source of what this device heard itself. */
        const val LOCAL = "local"

        /** The history in the app's database. Build one per process (it is a DI singleton). */
        fun create(context: Context): RoomListeningHistory = RoomListeningHistory(QuranDatabase.build(context))
    }
}

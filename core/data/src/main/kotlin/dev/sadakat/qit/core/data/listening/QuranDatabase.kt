package dev.sadakat.qit.core.data.listening

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** How many times [source] heard global ayah [globalAyah]. */
@Entity(tableName = "ayah_listens", primaryKeys = ["source", "global_ayah"])
internal data class AyahListenEntity(
    val source: String,
    @ColumnInfo(name = "global_ayah") val globalAyah: Int,
    val count: Int,
)

/** How long [source] listened to [surah], and when it last heard one of its ayahs (0 = never). */
@Entity(tableName = "surah_listening", primaryKeys = ["source", "surah"])
internal data class SurahListeningEntity(
    val source: String,
    val surah: Int,
    @ColumnInfo(name = "listened_ms") val listenedMs: Long,
    @ColumnInfo(name = "last_heard_at") val lastHeardAt: Long,
)

/** When the history was last reset; a single row. */
@Entity(tableName = "listening_reset")
internal data class ListeningResetEntity(@PrimaryKey val id: Int = 0, @ColumnInfo(name = "reset_at") val resetAt: Long)

internal data class AyahTotal(@ColumnInfo(name = "global_ayah") val globalAyah: Int, val count: Int)

internal data class SurahTotal(
    val surah: Int,
    @ColumnInfo(name = "listened_ms") val listenedMs: Long,
    @ColumnInfo(name = "last_heard_at") val lastHeardAt: Long,
)

/**
 * The listening tables. Increments are an update, then an insert when there was no row: SQLite's
 * upsert (`ON CONFLICT DO UPDATE`) needs Android 11, and QIt runs from Android 8.
 */
@Dao
@Suppress("TooManyFunctions") // A DAO: one small function per SQL statement.
internal abstract class ListeningDao {

    @Query("SELECT global_ayah, SUM(count) AS count FROM ayah_listens GROUP BY global_ayah")
    abstract fun ayahTotals(): Flow<List<AyahTotal>>

    @Query(
        "SELECT surah, SUM(listened_ms) AS listened_ms, MAX(last_heard_at) AS last_heard_at " +
            "FROM surah_listening GROUP BY surah",
    )
    abstract fun surahTotals(): Flow<List<SurahTotal>>

    @Query("SELECT reset_at FROM listening_reset WHERE id = 0")
    abstract fun resetAt(): Flow<Long?>

    @Query("SELECT reset_at FROM listening_reset WHERE id = 0")
    abstract suspend fun currentResetAt(): Long?

    @Query("SELECT * FROM ayah_listens WHERE source = :source")
    abstract suspend fun ayahListens(source: String): List<AyahListenEntity>

    @Query("SELECT * FROM surah_listening WHERE source = :source")
    abstract suspend fun surahListening(source: String): List<SurahListeningEntity>

    @Transaction
    open suspend fun recordHeard(source: String, surah: Int, globalAyah: Int, atMs: Long) {
        if (bumpAyah(source, globalAyah) == 0) insertAyahs(listOf(AyahListenEntity(source, globalAyah, 1)))
        if (touchSurah(source, surah, atMs) == 0) insertSurahs(listOf(SurahListeningEntity(source, surah, 0, atMs)))
    }

    @Transaction
    open suspend fun addListeningTime(source: String, surah: Int, ms: Long) {
        if (addTime(source, surah, ms) == 0) insertSurahs(listOf(SurahListeningEntity(source, surah, ms, 0)))
    }

    /** Replaces everything [source] heard with [ayahs] and [surahs]. */
    @Transaction
    open suspend fun replaceSource(source: String, ayahs: List<AyahListenEntity>, surahs: List<SurahListeningEntity>) {
        deleteAyahs(source)
        deleteSurahs(source)
        insertAyahs(ayahs)
        insertSurahs(surahs)
    }

    /** Forgets everything, from every source, and remembers when. */
    @Transaction
    open suspend fun reset(atMs: Long) {
        deleteAllAyahs()
        deleteAllSurahs()
        setResetAt(ListeningResetEntity(resetAt = atMs))
    }

    @Query("UPDATE ayah_listens SET count = count + 1 WHERE source = :source AND global_ayah = :globalAyah")
    protected abstract suspend fun bumpAyah(source: String, globalAyah: Int): Int

    @Query(
        "UPDATE surah_listening SET last_heard_at = MAX(last_heard_at, :atMs) " +
            "WHERE source = :source AND surah = :surah",
    )
    protected abstract suspend fun touchSurah(source: String, surah: Int, atMs: Long): Int

    @Query("UPDATE surah_listening SET listened_ms = listened_ms + :ms WHERE source = :source AND surah = :surah")
    protected abstract suspend fun addTime(source: String, surah: Int, ms: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAyahs(rows: List<AyahListenEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSurahs(rows: List<SurahListeningEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun setResetAt(row: ListeningResetEntity)

    @Query("DELETE FROM ayah_listens WHERE source = :source")
    protected abstract suspend fun deleteAyahs(source: String)

    @Query("DELETE FROM surah_listening WHERE source = :source")
    protected abstract suspend fun deleteSurahs(source: String)

    @Query("DELETE FROM ayah_listens")
    protected abstract suspend fun deleteAllAyahs()

    @Query("DELETE FROM surah_listening")
    protected abstract suspend fun deleteAllSurahs()
}

/**
 * QIt's database: the listening history. Version 1; the schema is exported to `core/data/schemas/`,
 * so a later version must add a migration (and a test against the exported schema) instead of
 * dropping what was heard.
 */
@Database(
    entities = [AyahListenEntity::class, SurahListeningEntity::class, ListeningResetEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class QuranDatabase : RoomDatabase() {

    abstract fun listeningDao(): ListeningDao

    companion object {
        fun build(context: Context): QuranDatabase =
            Room.databaseBuilder(context.applicationContext, QuranDatabase::class.java, "quran.db").build()
    }
}

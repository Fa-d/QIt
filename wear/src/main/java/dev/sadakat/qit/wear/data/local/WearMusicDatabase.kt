package dev.sadakat.qit.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.sadakat.qit.wear.data.local.dao.PlaylistDao
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.data.local.entity.PlaylistEntity
import dev.sadakat.qit.wear.data.local.entity.SongEntity

// Migration to add coverArtUri, mimeType, and bitrate columns to SongEntity
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE songs ADD COLUMN coverArtUri TEXT"
        )
        database.execSQL(
            "ALTER TABLE songs ADD COLUMN mimeType TEXT"
        )
        database.execSQL(
            "ALTER TABLE songs ADD COLUMN bitrate INTEGER NOT NULL DEFAULT 0"
        )
    }
}

@Database(
    entities = [PlaylistEntity::class, SongEntity::class],
    version = 2,
    exportSchema = false,
    autoMigrations = []
)
abstract class WearMusicDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: WearMusicDatabase? = null

        fun getDatabase(context: Context): WearMusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WearMusicDatabase::class.java,
                    "wear_music_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

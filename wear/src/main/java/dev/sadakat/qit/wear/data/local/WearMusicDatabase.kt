package dev.sadakat.qit.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.sadakat.qit.wear.data.local.dao.PlaylistDao
import dev.sadakat.qit.wear.data.local.dao.SongDao
import dev.sadakat.qit.wear.data.local.entity.PlaylistEntity
import dev.sadakat.qit.wear.data.local.entity.SongEntity

@Database(
    entities = [PlaylistEntity::class, SongEntity::class],
    version = 1,
    exportSchema = false
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

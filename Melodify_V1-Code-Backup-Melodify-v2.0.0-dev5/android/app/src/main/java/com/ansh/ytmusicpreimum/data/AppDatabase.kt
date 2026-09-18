package com.ansh.ytmusicpreimum.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackCrossRef::class, HistoryEntity::class, SearchHistoryEntity::class, StreamCacheEntity::class, LyricsEntity::class, EqualizerPresetEntity::class, QueueEntity::class], version = 8)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "aura_player_database"
                    )
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                } catch (t: Throwable) {
                    android.util.Log.e("AppDatabase", "Error initializing Room DB, clearing corrupt database file", t)
                    try { context.applicationContext.deleteDatabase("aura_player_database") } catch (e: Throwable) {}
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "aura_player_database"
                    )
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                }
                INSTANCE = db
                db
            }
        }
    }
}

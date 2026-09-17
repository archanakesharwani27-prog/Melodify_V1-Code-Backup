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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_player_database"
                )
                // Prevent wiping user playlists, downloads, and favorites on version upgrades.
                // Destructive migration is only permitted on accidental database downgrades.
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

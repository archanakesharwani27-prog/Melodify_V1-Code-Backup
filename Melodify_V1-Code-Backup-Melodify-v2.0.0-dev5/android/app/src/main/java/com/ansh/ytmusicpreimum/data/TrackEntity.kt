package com.ansh.ytmusicpreimum.data

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Immutable
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "Unknown Album",
    val thumbnailUrl: String = "",
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val addedDate: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val coverUrl: String? = null
)

@Entity(tableName = "playlist_track_cross_ref", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackCrossRef(
    val playlistId: Long = 0,
    val trackId: String = "",
    val position: Int = 0
)

@Immutable
@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stream_cache")
data class StreamCacheEntity(
    @PrimaryKey val videoId: String,
    val streamUrl: String,
    val expiryTime: Long,
    val quality: String
)

@Immutable
@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val position: Int = 0
)

@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey val videoId: String,
    val lyrics: String,
    val isSynced: Boolean,
    val expiryTime: Long
)

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey val name: String,
    val bandLevels: String, // Comma separated shorts
    val bassBoost: Short,
    val virtualizer: Short,
    val loudness: Int,
    val isCustom: Boolean = true
)

@Immutable
data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(PlaylistTrackCrossRef::class, parentColumn = "playlistId", entityColumn = "trackId")
    )
    val tracks: List<TrackEntity>
)

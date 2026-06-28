package com.ansh.ytmusicpreimum.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
)

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
    val trackId: String = ""
)

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(PlaylistTrackCrossRef::class, parentColumn = "playlistId", entityColumn = "trackId")
    )
    val tracks: List<TrackEntity>
)

package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.data.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackEntityTest {

    @Test
    fun testTrackEntityCreation_defaultValues() {
        val track = TrackEntity(
            id = "test_123",
            title = "Shape of You",
            artist = "Ed Sheeran",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )

        assertEquals("test_123", track.id)
        assertEquals("Shape of You", track.title)
        assertEquals("Ed Sheeran", track.artist)
        assertEquals("https://example.com/thumb.jpg", track.thumbnailUrl)
        assertFalse(track.isDownloaded)
        assertFalse(track.isLiked)
        assertEquals(0, track.playCount)
    }

    @Test
    fun testTrackEntityCopy_toggleLiked() {
        val track = TrackEntity(
            id = "test_456",
            title = "Kesariya",
            artist = "Arijit Singh",
            thumbnailUrl = "https://example.com/kesariya.jpg",
            isLiked = false
        )

        val likedTrack = track.copy(isLiked = true)
        assertTrue(likedTrack.isLiked)
        assertEquals(track.id, likedTrack.id)
        assertEquals(track.title, likedTrack.title)
    }

    @Test
    fun testTrackEntityCopy_downloadedWithFilePath() {
        val track = TrackEntity(
            id = "test_789",
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            thumbnailUrl = "https://example.com/tumhiho.jpg"
        )

        val downloadedTrack = track.copy(
            isDownloaded = true,
            localFilePath = "/storage/emulated/0/Music/test_789.mp3"
        )

        assertTrue(downloadedTrack.isDownloaded)
        assertEquals("/storage/emulated/0/Music/test_789.mp3", downloadedTrack.localFilePath)
    }
}

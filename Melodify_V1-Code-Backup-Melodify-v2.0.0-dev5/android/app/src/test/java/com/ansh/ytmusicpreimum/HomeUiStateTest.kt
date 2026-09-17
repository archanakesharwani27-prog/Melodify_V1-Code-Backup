package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {

    @Test
    fun testHomeUiState_defaultValues() {
        val state = HomeUiState()
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
        assertTrue(state.trending.isEmpty())
        assertTrue(state.forYou.isEmpty())
        assertTrue(state.moodMixes.isEmpty())
        assertTrue(state.curatedSections.isEmpty())
    }

    @Test
    fun testHomeUiState_loadingTransition() {
        val state = HomeUiState().copy(isLoading = true, isRefreshing = true)
        assertTrue(state.isLoading)
        assertTrue(state.isRefreshing)
    }

    @Test
    fun testHomeUiState_populatedData() {
        val dummyTracks = listOf(
            TrackEntity("t1", "Track 1", "Artist 1", "thumb1"),
            TrackEntity("t2", "Track 2", "Artist 2", "thumb2")
        )
        val state = HomeUiState(
            isLoading = false,
            trending = dummyTracks,
            forYou = dummyTracks
        )

        assertEquals(2, state.trending.size)
        assertEquals(2, state.forYou.size)
        assertEquals("Track 1", state.trending[0].title)
    }
}

package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.SearchUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUiStateTest {

    @Test
    fun testSearchUiState_idle() {
        val state: SearchUiState = SearchUiState.Idle
        assertTrue(state is SearchUiState.Idle)
    }

    @Test
    fun testSearchUiState_loading() {
        val state: SearchUiState = SearchUiState.Loading
        assertTrue(state is SearchUiState.Loading)
    }

    @Test
    fun testSearchUiState_success() {
        val tracks = listOf(
            TrackEntity("id_1", "Song A", "Artist A", "thumbA")
        )
        val state: SearchUiState = SearchUiState.Success(tracks)
        assertTrue(state is SearchUiState.Success)
        assertEquals(1, (state as SearchUiState.Success).tracks.size)
        assertEquals("Song A", state.tracks[0].title)
    }

    @Test
    fun testSearchUiState_empty() {
        val state: SearchUiState = SearchUiState.Empty
        assertTrue(state is SearchUiState.Empty)
    }

    @Test
    fun testSearchUiState_error() {
        val state: SearchUiState = SearchUiState.Error("Network timeout")
        assertTrue(state is SearchUiState.Error)
        assertEquals("Network timeout", (state as SearchUiState.Error).message)
    }
}

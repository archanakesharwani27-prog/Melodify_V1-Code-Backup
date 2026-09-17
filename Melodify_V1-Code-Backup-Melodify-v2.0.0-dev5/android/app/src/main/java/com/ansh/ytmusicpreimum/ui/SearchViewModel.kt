package com.ansh.ytmusicpreimum.ui

import android.app.Application
import android.content.Context
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ansh.ytmusicpreimum.YouTubeExtractor
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.data.TrackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val tracks: List<TrackEntity>) : SearchUiState
    object Empty : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = TrackRepository(database.trackDao())

    private val authPrefs = application.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val userEmail: String get() = authPrefs.getString("user_email", "default") ?: "default"

    // In-memory LRU cache for 50 recent search results
    private val searchCache = LruCache<String, List<TrackEntity>>(50)

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    val searchResults: StateFlow<List<TrackEntity>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchError = MutableStateFlow<Boolean>(false)
    val searchError: StateFlow<Boolean> = _searchError

    private var searchJob: Job? = null

    val recentSearches: StateFlow<List<String>> = repository.recentSearches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun search(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()

        if (trimmed.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            _searchError.value = false
            _uiState.value = SearchUiState.Idle
            return
        }

        // 1. Instant Cache Hit Check
        val cached = searchCache.get(trimmed.lowercase())
        if (cached != null) {
            _searchResults.value = cached
            _isSearching.value = false
            _searchError.value = false
            _uiState.value = if (cached.isEmpty()) SearchUiState.Empty else SearchUiState.Success(cached)
            return
        }

        searchJob = viewModelScope.launch {
            // Check local tracks immediately while debouncing network call
            val localMatches = repository.searchLocalTracks(trimmed).firstOrNull() ?: emptyList()
            if (localMatches.isNotEmpty() && _searchResults.value.isEmpty()) {
                _searchResults.value = localMatches
            }

            delay(300.milliseconds) // Debounce rapid keystrokes
            _isSearching.value = true
            _searchError.value = false
            _uiState.value = SearchUiState.Loading

            try {
                val results = YouTubeExtractor.search(trimmed)
                val enriched = repository.enrichTracks(results)

                // Combine with any local matches not present in remote results
                val combined = if (localMatches.isNotEmpty()) {
                    val remoteIds = enriched.map { it.id }.toSet()
                    localMatches.filter { it.id !in remoteIds } + enriched
                } else {
                    enriched
                }

                searchCache.put(trimmed.lowercase(), combined)
                _searchResults.value = combined
                _uiState.value = if (combined.isEmpty()) SearchUiState.Empty else SearchUiState.Success(combined)
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Search failed for query: $trimmed", e)
                _searchError.value = true
                _uiState.value = SearchUiState.Error(e.localizedMessage ?: "Search failed")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            repository.addToSearchHistory(query.trim())
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }
}

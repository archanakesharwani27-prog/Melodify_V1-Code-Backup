package com.ansh.ytmusicpreimum.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ansh.ytmusicpreimum.data.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = com.ansh.ytmusicpreimum.data.AppDatabase.getDatabase(application)
    private val repository = com.ansh.ytmusicpreimum.data.TrackRepository(database.trackDao())

    private val authPrefs = application.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val userEmail: String get() = authPrefs.getString("user_email", "default") ?: "default"
    
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
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            _searchError.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300.milliseconds) // Debounce
            _isSearching.value = true
            _searchError.value = false
            try {
                val results = com.ansh.ytmusicpreimum.YouTubeExtractor.search(query)
                _searchResults.value = repository.enrichTracks(results)
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Search failed", e)
                _searchError.value = true
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            repository.addToSearchHistory(query)
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

package com.ansh.ytmusicpreimum.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ansh.ytmusicpreimum.data.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    
    private val _searchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    val searchResults: StateFlow<List<TrackEntity>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _recentSearches = MutableStateFlow<List<String>>(loadHistory())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                _searchResults.value = com.ansh.ytmusicpreimum.YouTubeExtractor.search(query)
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
            _isSearching.value = false
        }
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        val currentList = _recentSearches.value.toMutableList()
        currentList.remove(query)
        currentList.add(0, query)
        val updatedList = currentList.take(10)
        _recentSearches.value = updatedList
        saveHistory(updatedList)
    }

    fun removeRecentSearch(query: String) {
        val updatedList = _recentSearches.value.filter { it != query }
        _recentSearches.value = updatedList
        saveHistory(updatedList)
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        saveHistory(emptyList())
    }

    private fun saveHistory(history: List<String>) {
        sharedPrefs.edit().putString("history", history.joinToString("|||")).apply()
    }

    private fun loadHistory(): List<String> {
        val historyString = sharedPrefs.getString("history", "") ?: ""
        return if (historyString.isEmpty()) emptyList() else historyString.split("|||")
    }
}

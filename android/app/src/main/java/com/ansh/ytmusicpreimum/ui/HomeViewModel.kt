package com.ansh.ytmusicpreimum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.YouTubeExtractor
import com.ansh.ytmusicpreimum.data.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

class HomeViewModel : ViewModel() {
    private val _trendingSongs = MutableStateFlow<List<TrackEntity>>(emptyList())
    val trendingSongs: StateFlow<List<TrackEntity>> = _trendingSongs

    private val _forYouSongs = MutableStateFlow<List<TrackEntity>>(emptyList())
    val forYouSongs: StateFlow<List<TrackEntity>> = _forYouSongs

    private val _moodMixes = MutableStateFlow<List<Pair<String, List<TrackEntity>>>>(emptyList())
    val moodMixes: StateFlow<List<Pair<String, List<TrackEntity>>>> = _moodMixes

    private val _evergreenEmotionalMixes = MutableStateFlow<List<Pair<String, List<TrackEntity>>>>(emptyList())
    val evergreenEmotionalMixes: StateFlow<List<Pair<String, List<TrackEntity>>>> = _evergreenEmotionalMixes

    private val _highEnergyPartyMixes = MutableStateFlow<List<Pair<String, List<TrackEntity>>>>(emptyList())
    val highEnergyPartyMixes: StateFlow<List<Pair<String, List<TrackEntity>>>> = _highEnergyPartyMixes

    private val _culturalFestiveMixes = MutableStateFlow<List<Pair<String, List<TrackEntity>>>>(emptyList())
    val culturalFestiveMixes: StateFlow<List<Pair<String, List<TrackEntity>>>> = _culturalFestiveMixes

    private val _smartMixes = MutableStateFlow<List<Pair<String, List<TrackEntity>>>>(emptyList())
    val smartMixes: StateFlow<List<Pair<String, List<TrackEntity>>>> = _smartMixes

    private val _globalHits = MutableStateFlow<List<Pair<String, List<TrackEntity>>>>(emptyList())
    val globalHits: StateFlow<List<Pair<String, List<TrackEntity>>>> = _globalHits

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var lastRefreshTime: Long = 0
    private var repository: TrackRepository? = null

    fun setRepository(repo: TrackRepository) {
        this.repository = repo
        if (_trendingSongs.value.isEmpty()) {
            loadHomeData()
        }
    }

    init {
        loadHomeData()
    }

    fun refresh() {
        loadHomeData(force = true)
    }

    private fun loadHomeData(force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!force && _trendingSongs.value.isNotEmpty() && (currentTime - lastRefreshTime < 14400000)) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            lastRefreshTime = currentTime
            
            try {
                // Analysis of user habits
                val liked = repository?.likedTracks?.first()?.take(5) ?: emptyList()
                val topArtist = liked.map { it.artist }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "Arijit Singh"
                
                val forYouQuery = if (liked.isNotEmpty()) "$topArtist Radio Mix" else "Popular Hindi Hits 2024"

                // 1. FAST LOAD: Initial important sections
                val trendingJob = async { YouTubeExtractor.search("Trending Hindi Songs India Top Hits") }
                val forYouJob = async { YouTubeExtractor.search(forYouQuery) }
                
                val fastResults = awaitAll(trendingJob, forYouJob)
                _trendingSongs.value = repository?.enrichTracks(fastResults[0].shuffled().take(10).ifEmpty { getDummyTracks("Trending") }) ?: emptyList()
                _forYouSongs.value = repository?.enrichTracks(fastResults[1].shuffled().take(10).ifEmpty { getDummyTracks("For You") }) ?: emptyList()
                _isLoading.value = false // Hide loader early

                // 2. BACKGROUND LOAD: The rest of the sections (Throttled)
                launch {
                    val moodQueries = listOf(
                        "lofi music relaxation",
                        "Gym Motivation Workout Music",
                        "Deep Focus ambient"
                    )
                    val smartQueries = listOf(
                        "New Music Release India",
                        "Evergreen Classic Hits"
                    )
                    val culturalQueries = listOf(
                        "Bhakti Sagar Shiv Bhajan Aarti",
                        "Desi Shaadi Sangeet Vibe"
                    )
                    
                    // Load in smaller chunks
                    val moodResults = moodQueries.map { q -> async { YouTubeExtractor.search(q) } }.awaitAll()
                    _moodMixes.value = listOf(
                        "Chill / Relaxation Zone" to (repository?.enrichTracks(moodResults[0]) ?: moodResults[0]),
                        "Workout / Energy Booster" to (repository?.enrichTracks(moodResults[1]) ?: moodResults[1]),
                        "Focus / Study Mode" to (repository?.enrichTracks(moodResults[2]) ?: moodResults[2])
                    )
                    
                    val smartResults = smartQueries.map { q -> async { YouTubeExtractor.search(q) } }.awaitAll()
                    _smartMixes.value = listOf(
                        "Radar / New Release" to (repository?.enrichTracks(smartResults[0]) ?: smartResults[0]),
                        "Time Capsule" to (repository?.enrichTracks(smartResults[1]) ?: smartResults[1])
                    )
                    
                    // Delay slightly between chunks to reduce CPU/Network spike
                    delay(500)
                    
                    val culturalResults = culturalQueries.map { q -> async { YouTubeExtractor.search(q) } }.awaitAll()
                    _culturalFestiveMixes.value = listOf(
                        "Bhakti Ras (भक्ति सागर)" to (repository?.enrichTracks(culturalResults[0]) ?: culturalResults[0]),
                        "Desi Shaadi / Sangeet" to (repository?.enrichTracks(culturalResults[1]) ?: culturalResults[1])
                    )
                    
                    // Load remaining sections...
                    val topIndia = YouTubeExtractor.search("Top 50 Songs India")
                    _globalHits.value = listOf("Top India" to (repository?.enrichTracks(topIndia) ?: topIndia))
                }

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading home data", e)
                _isLoading.value = false
            }
        }
    }

    private fun getDummyTracks(category: String): List<TrackEntity> {
        return listOf(
            TrackEntity("dQw4w9WgXcQ", "$category Special 1", "Artist Collection", "https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg"),
            TrackEntity("3JZ_D3ELwOQ", "$category Mix 2", "Artist Collection", "https://img.youtube.com/vi/3JZ_D3ELwOQ/0.jpg")
        )
    }
}

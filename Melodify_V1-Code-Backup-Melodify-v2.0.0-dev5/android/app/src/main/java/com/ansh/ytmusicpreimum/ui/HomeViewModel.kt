package com.ansh.ytmusicpreimum.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ansh.ytmusicpreimum.YouTubeExtractor
import com.ansh.ytmusicpreimum.data.AppDatabase
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.data.TrackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val trending: List<TrackEntity> = emptyList(),
    val forYou: List<TrackEntity> = emptyList(),
    val moodMixes: List<Pair<String, List<TrackEntity>>> = emptyList(),
    val curatedSections: List<Pair<String, List<TrackEntity>>> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = TrackRepository(database.trackDao())

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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

    private val _curatedSections = MutableStateFlow<List<Pair<String, List<TrackEntity>>>>(emptyList())
    val curatedSections: StateFlow<List<Pair<String, List<TrackEntity>>>> = _curatedSections

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun getTracksBySection(title: String): List<TrackEntity> {
        return when (title) {
            "Trending Now" -> trendingSongs.value
            "For You" -> forYouSongs.value
            "Melodify Picks" -> curatedSections.value.find { it.first == "Melodify Picks" }?.second ?: emptyList()
            "Hidden Gems" -> curatedSections.value.find { it.first == "Hidden Gems" }?.second ?: emptyList()
            "Top India" -> globalHits.value.find { it.first == "Top India" }?.second ?: emptyList()
            "Recently Played" -> emptyList()
            else -> {
                moodMixes.value.find { it.first.contains(title, ignoreCase = true) }?.second
                    ?: evergreenEmotionalMixes.value.find { it.first.contains(title, ignoreCase = true) }?.second
                    ?: highEnergyPartyMixes.value.find { it.first.contains(title, ignoreCase = true) }?.second
                    ?: culturalFestiveMixes.value.find { it.first.contains(title, ignoreCase = true) }?.second
                    ?: smartMixes.value.find { it.first.contains(title, ignoreCase = true) }?.second
                    ?: emptyList()
            }
        }
    }

    private var lastRefreshTime: Long = 0

    // Backward-compatibility stub
    fun setRepository(repo: TrackRepository) {
        // Repository is now cleanly instantiated internally via AndroidViewModel
    }

    init {
        loadHomeData()
    }

    fun refresh() {
        loadHomeData(force = true)
    }

    private fun getTimeOfDayMelodifySeed(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val morningSeeds = listOf(
            "Acoustic Hindi Morning Melodies",
            "Fresh Bollywood Morning Hits",
            "Sufi Morning Soul Chill",
            "Arijit Singh Morning Acoustic"
        )
        val afternoonSeeds = listOf(
            "Bollywood Indie Pop Gems",
            "Coke Studio Bharat Hits",
            "Lo-Fi Hindi Pop Hits",
            "Trending Hindi Indie Beats"
        )
        val eveningSeeds = listOf(
            "Sunset Drive Bollywood Hits",
            "Bollywood Romantic Evening Vibes",
            "Unplugged Bollywood Acoustic",
            "Soulful Bollywood Evening Melodies"
        )
        val nightSeeds = listOf(
            "Late Night Bollywood Lo-Fi",
            "Midnight Acoustic Hindi Soul",
            "Midnight Chill Hop Hindi",
            "Diljit & Arijit Midnight Acoustic"
        )

        val seedList = when (hour) {
            in 5..11 -> morningSeeds
            in 12..16 -> afternoonSeeds
            in 17..20 -> eveningSeeds
            else -> nightSeeds
        }
        return seedList.random()
    }

    fun refreshMelodifyPicks() {
        viewModelScope.launch {
            try {
                val seed = getTimeOfDayMelodifySeed()
                val picks = YouTubeExtractor.search(seed)
                val enrichedPicks = repository.enrichTracks(picks.shuffled().take(10))
                if (enrichedPicks.isNotEmpty()) {
                    val current = _curatedSections.value.toMutableList()
                    val index = current.indexOfFirst { it.first == "Melodify Picks" }
                    if (index >= 0) {
                        current[index] = "Melodify Picks" to enrichedPicks
                    } else {
                        current.add(0, "Melodify Picks" to enrichedPicks)
                    }
                    _curatedSections.value = current
                    _uiState.value = _uiState.value.copy(curatedSections = current)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error refreshing Melodify Picks", e)
            }
        }
    }

    private fun loadHomeData(force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!force && _trendingSongs.value.isNotEmpty() && (currentTime - lastRefreshTime < 1800000)) { // 30 min cache
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = _uiState.value.copy(isLoading = true, isRefreshing = force, error = null)
            lastRefreshTime = currentTime

            try {
                // Determine user habit recommendations based on local liked history
                val liked = repository.likedTracks.firstOrNull()?.take(5) ?: emptyList()
                val topArtist = liked.map { it.artist }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "Arijit Singh"
                val forYouQuery = if (liked.isNotEmpty()) "$topArtist Radio Mix" else "Popular Hindi Hits 2024"

                // 1. FAST LOAD: Primary home sections
                val trendingJob = async { YouTubeExtractor.search("Trending Hindi Songs India Top Hits") }
                val forYouJob = async { YouTubeExtractor.search(forYouQuery) }

                val fastResults = awaitAll(trendingJob, forYouJob)
                val enrichedTrending = repository.enrichTracks(fastResults[0].shuffled().take(10).ifEmpty { getFallbackTracks("Trending") })
                val enrichedForYou = repository.enrichTracks(fastResults[1].shuffled().take(10).ifEmpty { getFallbackTracks("For You") })

                _trendingSongs.value = enrichedTrending
                _forYouSongs.value = enrichedForYou
                _isLoading.value = false
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    trending = enrichedTrending,
                    forYou = enrichedForYou
                )

                // 2. BACKGROUND LOAD: Additional Curated Mixes (Chunked for performance)
                launch {
                    try {
                        val moodQueries = listOf(
                            "lofi music relaxation",
                            "Gym Motivation Workout Music",
                            "Deep Focus ambient"
                        )
                        val moodResults = moodQueries.map { q -> async { YouTubeExtractor.search(q) } }.awaitAll()
                        val moodList = listOf(
                            "Chill / Relaxation Zone" to repository.enrichTracks(moodResults[0]),
                            "Workout / Energy Booster" to repository.enrichTracks(moodResults[1]),
                            "Focus / Study Mode" to repository.enrichTracks(moodResults[2])
                        )
                        _moodMixes.value = moodList
                        _uiState.value = _uiState.value.copy(moodMixes = moodList)

                        delay(400) // Brief pacing to keep Compose rendering smooth

                        val smartQueries = listOf(
                            "New Music Release India",
                            "Evergreen Classic Hits"
                        )
                        val smartResults = smartQueries.map { q -> async { YouTubeExtractor.search(q) } }.awaitAll()
                        _smartMixes.value = listOf(
                            "Radar / New Release" to repository.enrichTracks(smartResults[0]),
                            "Time Capsule" to repository.enrichTracks(smartResults[1])
                        )

                        val culturalQueries = listOf(
                            "Bhakti Sagar Shiv Bhajan Aarti",
                            "Desi Shaadi Sangeet Vibe"
                        )
                        val culturalResults = culturalQueries.map { q -> async { YouTubeExtractor.search(q) } }.awaitAll()
                        _culturalFestiveMixes.value = listOf(
                            "Bhakti Ras (भक्ति सागर)" to repository.enrichTracks(culturalResults[0]),
                            "Desi Shaadi / Sangeet" to repository.enrichTracks(culturalResults[1])
                        )

                        val topIndia = YouTubeExtractor.search("Top 50 Songs India")
                        _globalHits.value = listOf("Top India" to repository.enrichTracks(topIndia))

                        val dynamicPicksSeed = getTimeOfDayMelodifySeed()
                        val curatedQueries = listOf(
                            dynamicPicksSeed,
                            "Indie Hindi Gems 2024",
                            "Bollywood Lo-fi Mashup 1 hour"
                        )
                        val curatedResults = curatedQueries.map { q -> async { YouTubeExtractor.search(q) } }.awaitAll()
                        val curatedList = listOf(
                            "Melodify Picks" to repository.enrichTracks(curatedResults[0]),
                            "Hidden Gems" to repository.enrichTracks(curatedResults[1]),
                            "Editor's Choice" to repository.enrichTracks(curatedResults[2])
                        )
                        _curatedSections.value = curatedList
                        _uiState.value = _uiState.value.copy(curatedSections = curatedList)
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error loading background home mixes", e)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading home data", e)
                _isLoading.value = false
                _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, error = e.localizedMessage)
                if (_trendingSongs.value.isEmpty()) {
                    _trendingSongs.value = getFallbackTracks("Trending")
                }
            }
        }
    }

    private suspend fun getFallbackTracks(category: String): List<TrackEntity> {
        val cachedTracks = repository.allTracks.firstOrNull()?.take(10) ?: emptyList()
        if (cachedTracks.isNotEmpty()) {
            return cachedTracks
        }
        return listOf(
            TrackEntity("dQw4w9WgXcQ", "$category Special", "Melodify Artist", "https://img.youtube.com/vi/dQw4w9WgXcQ/0.jpg"),
            TrackEntity("3JZ_D3ELwOQ", "$category Hits", "Melodify Artist", "https://img.youtube.com/vi/3JZ_D3ELwOQ/0.jpg")
        )
    }
}

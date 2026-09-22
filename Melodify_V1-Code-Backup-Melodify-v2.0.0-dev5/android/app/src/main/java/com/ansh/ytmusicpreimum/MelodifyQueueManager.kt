package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.data.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class MelodifyQueueManager {
    private val _queue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val queue = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex = _currentIndex.asStateFlow()

    fun serialize(): String {
        val array = JSONArray()
        _queue.value.forEach { track ->
            val obj = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("thumbnailUrl", track.thumbnailUrl)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserialize(json: String, index: Int) {
        try {
            val array = JSONArray(json)
            val list = mutableListOf<TrackEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(TrackEntity(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    thumbnailUrl = obj.getString("thumbnailUrl")
                ))
            }
            _queue.value = list
            _currentIndex.value = index
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setQueue(tracks: List<TrackEntity>, startId: String? = null) {
        _queue.value = tracks
        _currentIndex.value = if (startId != null) {
            tracks.indexOfFirst { it.id == startId }.coerceAtLeast(-1)
        } else {
            -1
        }
    }

    fun playNext(track: TrackEntity) {
        val currentList = _queue.value.toMutableList()
        val insertIndex = _currentIndex.value + 1
        
        // Remove if already in queue to avoid duplicates in upcoming
        currentList.removeAll { it.id == track.id }
        
        if (insertIndex in 0..currentList.size) {
            currentList.add(insertIndex, track)
        } else {
            currentList.add(track)
        }
        _queue.value = currentList
    }

    fun addToQueue(track: TrackEntity) {
        val currentList = _queue.value.toMutableList()
        if (!currentList.any { it.id == track.id }) {
            currentList.add(track)
            _queue.value = currentList
        }
    }

    fun addTracks(tracks: List<TrackEntity>) {
        val currentList = _queue.value.toMutableList()
        val existingIds = currentList.map { it.id }.toSet()
        val newTracks = tracks.filter { it.id !in existingIds }
        if (newTracks.isNotEmpty()) {
            currentList.addAll(newTracks)
            _queue.value = currentList
        }
    }

    fun insertAt(index: Int, track: TrackEntity) {
        val currentList = _queue.value.toMutableList()
        if (index in 0..currentList.size) {
            currentList.add(index, track)
            if (index <= _currentIndex.value) _currentIndex.value++
            _queue.value = currentList
        }
    }

    fun removeAt(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            if (index <= _currentIndex.value) _currentIndex.value--
            _queue.value = currentList
        }
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val track = currentList.removeAt(fromIndex)
            currentList.add(toIndex, track)
            
            // Adjust currentIndex
            if (_currentIndex.value == fromIndex) {
                _currentIndex.value = toIndex
            } else if (fromIndex < _currentIndex.value && toIndex >= _currentIndex.value) {
                _currentIndex.value--
            } else if (fromIndex > _currentIndex.value && toIndex <= _currentIndex.value) {
                _currentIndex.value++
            }
            
            _queue.value = currentList
        }
    }

    fun getNextTrack(): TrackEntity? {
        val currentList = _queue.value
        if (_currentIndex.value < currentList.size - 1) {
            _currentIndex.value++
            return currentList[_currentIndex.value]
        }
        return null
    }

    fun getPreviousTrack(): TrackEntity? {
        val currentList = _queue.value
        if (_currentIndex.value > 0) {
            _currentIndex.value--
            return currentList[_currentIndex.value]
        }
        return null
    }
    
    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = -1
    }

    fun getCurrentIndex() = _currentIndex.value
    fun setCurrentIndex(index: Int) {
        _currentIndex.value = index
    }
}


package com.ansh.ytmusicpreimum

import com.ansh.ytmusicpreimum.data.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MelodifyQueueManager {
    private val _queue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val queue = _queue.asStateFlow()

    private var currentIndex = -1

    fun setQueue(tracks: List<TrackEntity>, startId: String? = null) {
        _queue.value = tracks
        currentIndex = if (startId != null) {
            tracks.indexOfFirst { it.id == startId }.coerceAtLeast(-1)
        } else {
            -1
        }
    }

    fun playNext(track: TrackEntity) {
        val currentList = _queue.value.toMutableList()
        val insertIndex = currentIndex + 1
        
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

    fun removeAt(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            if (index <= currentIndex) currentIndex--
            _queue.value = currentList
        }
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val track = currentList.removeAt(fromIndex)
            currentList.add(toIndex, track)
            
            // Adjust currentIndex
            if (currentIndex == fromIndex) {
                currentIndex = toIndex
            } else if (fromIndex < currentIndex && toIndex >= currentIndex) {
                currentIndex--
            } else if (fromIndex > currentIndex && toIndex <= currentIndex) {
                currentIndex++
            }
            
            _queue.value = currentList
        }
    }

    fun getNextTrack(): TrackEntity? {
        val currentList = _queue.value
        if (currentIndex < currentList.size - 1) {
            currentIndex++
            return currentList[currentIndex]
        }
        return null
    }

    fun getPreviousTrack(): TrackEntity? {
        val currentList = _queue.value
        if (currentIndex > 0) {
            currentIndex--
            return currentList[currentIndex]
        }
        return null
    }
    
    fun getCurrentIndex() = currentIndex
    fun setCurrentIndex(index: Int) {
        currentIndex = index
    }
}

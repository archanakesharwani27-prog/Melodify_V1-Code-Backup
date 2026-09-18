package com.ansh.ytmusicpreimum.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ansh.ytmusicpreimum.DownloadState
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.MusicViewModel
import com.ansh.ytmusicpreimum.ui.SearchViewModel
import com.ansh.ytmusicpreimum.ui.components.SearchResultSkeleton
import com.ansh.ytmusicpreimum.ui.components.TrackItem
import com.ansh.ytmusicpreimum.ui.theme.*

@Composable
fun SearchScreen(
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit, 
    searchViewModel: SearchViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel(),
    downloadStates: Map<String, DownloadState> = emptyMap(),
    onDownloadClick: (TrackEntity) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onAddToPlaylist: (TrackEntity) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val results by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val searchError by searchViewModel.searchError.collectAsState()
    val recent by searchViewModel.recentSearches.collectAsState()

    // Predictive Prefetch
    LaunchedEffect(results) {
        if (results.isNotEmpty()) {
            musicViewModel.prefetchTracks(results)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicViewModel.cancelPredictivePrefetch()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Text(
            "Search", 
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), 
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), 
            color = MaterialTheme.colorScheme.onBackground
        )
        SearchBar(query, onQueryChange = { query = it; searchViewModel.search(it) }, onSearch = { searchViewModel.addToHistory(query) })
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (query.isBlank()) {
                item { SearchEmptyState() }
                item { Text("Browse All", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = Color.White) }
                item { GenreGrid { query = it; searchViewModel.search(it); searchViewModel.addToHistory(it) } }
                item { RecentSearches(recent = recent, onItemClick = { query = it; searchViewModel.search(it); searchViewModel.addToHistory(it) }, onDelete = { searchViewModel.removeRecentSearch(it) }, onClearAll = { searchViewModel.clearRecentSearches() }) }
            } else if (isSearching) {
                items(10) { SearchResultSkeleton() }
            } else if (searchError) {
                item { SearchErrorState { searchViewModel.search(query) } }
            } else if (results.isEmpty()) {
                item { NoResultsState() }
            } else {
                items(results, key = { it.id }) { track -> 
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(track.id) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        TrackItem(
                            track = track,
                            downloadState = downloadStates[track.id],
                            onDownloadClick = { onDownloadClick(track) },
                            onCancelDownload = { onCancelDownload(track.id) },
                            onAddToPlaylist = { onAddToPlaylist(track) }
                        ) { 
                            searchViewModel.addToHistory(query)
                            onSongPlay(track, listOf(track)) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchEmptyState() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎵", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Search your favorite songs, artists or albums.", style = MaterialTheme.typography.bodyLarge, color = GrayText, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
fun NoResultsState() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No results found", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Try another keyword.", style = MaterialTheme.typography.bodyMedium, color = GrayText)
        }
    }
}

@Composable
fun SearchErrorState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Network error", style = MaterialTheme.typography.titleMedium, color = Color.White)
            TextButton(onClick = onRetry) {
                Text("Retry", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(GeminiCardBg)
            .border(
                width = 1.dp,
                color = if (isFocused) GeminiBlue else GeminiBorder.copy(alpha = 0.8f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        TextField(
            value = query, 
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { Text("Search songs, artists, or moods...", color = GeminiTextSecondary, fontSize = 14.sp) },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null, 
                    tint = if (isFocused) GeminiBlue else GeminiTextSecondary,
                    modifier = Modifier.size(22.dp)
                ) 
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = GeminiTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = GeminiBlue,
                focusedTextColor = GeminiTextPrimary,
                unfocusedTextColor = GeminiTextPrimary
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
    }
}

@Composable
fun GenreGrid(onGenreClick: (String) -> Unit) {
    val genres = listOf(
        "Bollywood" to GenreBollywood, 
        "Punjabi" to GenrePunjabi, 
        "Romantic" to GenreRomantic, 
        "English Pop" to GenreEnglishPop, 
        "Dance/Party" to GenreDanceParty, 
        "Hip Hop" to GenreHipHop
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), 
        modifier = Modifier
            .height(300.dp)
            .padding(horizontal = 16.dp), 
        horizontalArrangement = Arrangement.spacedBy(12.dp), 
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(genres) { (name, color) -> GenreCard(name, color, onClick = { onGenreClick(name) }) }
    }
}

@Composable
fun GenreCard(name: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GeminiCardBg)
            .border(1.dp, GeminiBorder.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
                .align(Alignment.TopEnd)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                name, 
                color = GeminiTextPrimary, 
                fontWeight = FontWeight.SemiBold, 
                fontSize = 15.sp
            )
            Text(
                "Explore Mix", 
                color = GeminiTextSecondary, 
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun RecentSearches(recent: List<String>, onItemClick: (String) -> Unit, onDelete: (String) -> Unit, onClearAll: () -> Unit) {
    if (recent.isEmpty()) return
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = GeminiTextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Recent Searches", color = GeminiTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("Clear all", color = GeminiBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onClearAll))
        }
        Spacer(modifier = Modifier.height(10.dp))
        recent.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onItemClick(item) }
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = GeminiTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(item, color = GeminiTextPrimary, modifier = Modifier.weight(1f), fontSize = 14.sp)
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = GeminiTextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}


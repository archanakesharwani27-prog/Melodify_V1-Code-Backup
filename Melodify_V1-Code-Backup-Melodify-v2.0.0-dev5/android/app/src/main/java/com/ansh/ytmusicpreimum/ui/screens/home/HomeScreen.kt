@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ansh.ytmusicpreimum.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ansh.ytmusicpreimum.data.TrackEntity
import com.ansh.ytmusicpreimum.ui.HomeViewModel
import com.ansh.ytmusicpreimum.ui.LibraryViewModel
import com.ansh.ytmusicpreimum.ui.LoginViewModel
import com.ansh.ytmusicpreimum.ui.MusicViewModel
import com.ansh.ytmusicpreimum.ui.components.FuturisticThumbnail
import com.ansh.ytmusicpreimum.ui.components.HomeSectionSkeleton
import com.ansh.ytmusicpreimum.ui.components.SectionTitle
import com.ansh.ytmusicpreimum.ui.theme.*

@Composable
fun HomeScreen(
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit,
    onGenreClick: (String) -> Unit = {},
    onViewAll: (String) -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel()
) {
    val trending by homeViewModel.trendingSongs.collectAsState()
    val forYou by homeViewModel.forYouSongs.collectAsState()
    val moodMixes by homeViewModel.moodMixes.collectAsState()
    val evergreenMixes by homeViewModel.evergreenEmotionalMixes.collectAsState()
    val partyMixes by homeViewModel.highEnergyPartyMixes.collectAsState()
    val festiveMixes by homeViewModel.culturalFestiveMixes.collectAsState()
    val smartMixes by homeViewModel.smartMixes.collectAsState()
    val globalHits by homeViewModel.globalHits.collectAsState()
    val curatedSections by homeViewModel.curatedSections.collectAsState()
    val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()

    // Predictive Prefetch for Home
    LaunchedEffect(trending) {
        if (trending.isNotEmpty()) {
            musicViewModel.prefetchTracks(trending)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicViewModel.cancelPredictivePrefetch()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. GREETING
        item { HomeHeader(homeViewModel) }

        // 2. MELODIFY PICKS (Hero)
        item {
            val picks = curatedSections.find { it.first == "Melodify Picks" }?.second ?: emptyList()
            if (picks.isNotEmpty()) {
                CuratedHeroSection(tracks = picks, onSongPlay = onSongPlay)
            }
        }

        // 3. GENRE CHIPS
        item { GenreRow(onGenreClick) }
        
        if (isLoading && trending.isEmpty()) {
            items(5) { HomeSectionSkeleton() }
        } else {
            // 4. TRENDING NOW
            item {
                if (trending.isNotEmpty()) {
                    SectionTitle("Trending Now", icon = Icons.Default.Whatshot)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(trending) { track -> SongCard(track) { onSongPlay(track, trending) } }
                    }
                }
            }

            // 5. FOR YOU
            item {
                if (forYou.isNotEmpty()) {
                    SectionTitle("For You", icon = Icons.Default.AutoAwesome)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(forYou) { track -> SongCard(track) { onSongPlay(track, forYou) } }
                    }
                }
            }

            // 6. MOOD MIXES (Grouped)
            item {
                val workout = moodMixes.find { it.first.contains("Workout", ignoreCase = true) }?.second ?: emptyList()
                if (workout.isNotEmpty()) {
                    SectionTitle("Workout Energy", icon = Icons.Default.Bolt)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(workout) { track -> SongCard(track) { onSongPlay(track, workout) } }
                    }
                }
            }

            item {
                val chill = moodMixes.find { it.first.contains("Chill", ignoreCase = true) }?.second ?: emptyList()
                if (chill.isNotEmpty()) {
                    SectionTitle("Chill Zone", icon = Icons.Default.Nightlight)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(chill) { track -> SongCard(track) { onSongPlay(track, chill) } }
                    }
                }
            }

            item {
                val focus = moodMixes.find { it.first.contains("Focus", ignoreCase = true) }?.second ?: emptyList()
                if (focus.isNotEmpty()) {
                    SectionTitle("Focus Mode", icon = Icons.Default.MenuBook)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(focus) { track -> SongCard(track) { onSongPlay(track, focus) } }
                    }
                }
            }

            // 7. HIDDEN GEMS
            item {
                val gems = curatedSections.find { it.first == "Hidden Gems" }?.second ?: emptyList()
                if (gems.isNotEmpty()) {
                    SectionTitle("Hidden Gems", icon = Icons.Default.Diamond)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(gems) { track -> SongCard(track) { onSongPlay(track, gems) } }
                    }
                }
            }

            // 8. CULTURAL MIXES
            item {
                val bhakti = festiveMixes.find { it.first.contains("Bhakti", ignoreCase = true) }?.second ?: emptyList()
                if (bhakti.isNotEmpty()) {
                    SectionTitle("Bhakti Ras", icon = Icons.Default.SelfImprovement)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(bhakti) { track -> SongCard(track) { onSongPlay(track, bhakti) } }
                    }
                }
            }

            item {
                val shaadi = festiveMixes.find { it.first.contains("Shaadi", ignoreCase = true) }?.second ?: emptyList()
                if (shaadi.isNotEmpty()) {
                    SectionTitle("Wedding Vibes", icon = Icons.Default.Favorite)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(shaadi) { track -> SongCard(track) { onSongPlay(track, shaadi) } }
                    }
                }
            }

            // 9. GLOBAL HITS
            item {
                val india = globalHits.find { it.first == "Top India" }?.second ?: emptyList()
                if (india.isNotEmpty()) {
                    SectionTitle("Top India", icon = Icons.Default.Public)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(india) { track -> SongCard(track) { onSongPlay(track, india) } }
                    }
                }
            }

            // 10. SMART MIXES
            item {
                val radar = smartMixes.find { it.first.contains("Radar", ignoreCase = true) }?.second ?: emptyList()
                if (radar.isNotEmpty()) {
                    SectionTitle("New Releases", icon = Icons.Default.NewReleases)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(radar) { track -> SongCard(track) { onSongPlay(track, radar) } }
                    }
                }
            }

            // 11. RECENTLY PLAYED
            item {
                if (recentlyPlayed.isNotEmpty()) {
                    SectionTitle("Recently Played", icon = Icons.Default.History)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recentlyPlayed.take(10)) { track ->
                            SongCard(track) { onSongPlay(track, recentlyPlayed) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(homeViewModel: HomeViewModel = viewModel(), loginViewModel: LoginViewModel = viewModel()) {
    val greeting = remember {
        val calendar = java.util.Calendar.getInstance()
        when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MELODIFY", 
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ), 
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = greeting, 
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ), 
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Surface(
                onClick = { homeViewModel.refresh() },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "Refresh", 
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your daily dose of music is ready.", 
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun GenreRow(onGenreClick: (String) -> Unit) {
    val genres = listOf(
        "Bollywood" to GenreBollywood, 
        "Punjabi" to GenrePunjabi, 
        "Romantic" to GenreRomantic, 
        "English Pop" to GenreEnglishPop, 
        "Dance/Party" to GenreDanceParty
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp), 
        horizontalArrangement = Arrangement.spacedBy(12.dp), 
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        items(genres) { (name, color) -> 
            FilterChip(
                selected = false,
                onClick = { onGenreClick(name) },
                label = { Text(text = name, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = color.copy(alpha = 0.15f),
                    labelColor = color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = color.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun CuratedHeroSection(tracks: List<TrackEntity>, onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit) {
    if (tracks.isEmpty()) return
    
    val pagerState = rememberPagerState(pageCount = { tracks.size.coerceAtMost(4) }) 
    
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        SectionTitle("Melodify Picks", icon = Icons.Default.Star)
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp
        ) { page ->
            val track = tracks[page]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { onSongPlay(track, tracks) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box {
                    FuturisticThumbnail(model = track.thumbnailUrl, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 100f
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                "FEATURED", 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        Text(track.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(track.artist, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun SongCard(track: TrackEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true)
            )
            .padding(4.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            FuturisticThumbnail(
                model = track.thumbnailUrl, 
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title, 
            color = MaterialTheme.colorScheme.onBackground, 
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = track.artist, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

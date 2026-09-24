@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ansh.ytmusicpreimum.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        item { HomeHeader(homeViewModel, onSearchClick = { onGenreClick("Search") }) }

        // 2. MELODIFY PICKS (Hero)
        item {
            val picks = curatedSections.find { it.first == "Melodify Picks" }?.second ?: emptyList()
            if (picks.isNotEmpty()) {
                CuratedHeroSection(
                    tracks = picks,
                    onRefreshPicks = { homeViewModel.refreshMelodifyPicks() },
                    onSongPlay = onSongPlay
                )
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
fun HomeHeader(
    homeViewModel: HomeViewModel = viewModel(),
    onSearchClick: () -> Unit = {}
) {
    val greeting = remember {
        val calendar = java.util.Calendar.getInstance()
        when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(GeminiBlue, GeminiPurple, GeminiPink)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MELODIFY", 
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ), 
                        color = GeminiBlue
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = greeting, 
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ), 
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Surface(
                onClick = { homeViewModel.refresh() },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "Refresh", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "What would you like to listen to today?", 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GenreRow(onGenreClick: (String) -> Unit) {
    val genres = listOf(
        "🔥 Trending" to "Trending", 
        "💖 Romantic" to "Romantic", 
        "🎧 Bollywood" to "Bollywood", 
        "⚡ Party" to "Party", 
        "☕ Chill" to "Chill",
        "🧘 Focus" to "Focus",
        "✨ Punjabi" to "Punjabi"
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp), 
        horizontalArrangement = Arrangement.spacedBy(10.dp), 
        modifier = Modifier.padding(vertical = 10.dp)
    ) {
        items(genres) { (label, name) -> 
            Surface(
                onClick = { onGenreClick(name) },
                shape = RoundedCornerShape(20.dp),
                color = GeminiCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBorder.copy(alpha = 0.8f))
            ) {
                Text(
                    text = label, 
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = GeminiTextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CuratedHeroSection(
    tracks: List<TrackEntity>, 
    onRefreshPicks: () -> Unit,
    onSongPlay: (TrackEntity, List<TrackEntity>) -> Unit
) {
    if (tracks.isEmpty()) return
    
    val pagerState = rememberPagerState(pageCount = { tracks.size.coerceAtMost(6) }) 
    val timeLabel = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "MORNING VIBES"
            in 12..16 -> "AFTERNOON CHILL"
            in 17..20 -> "SUNSET MELODIES"
            else -> "LATE NIGHT SOUL"
        }
    }
    
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    tint = GeminiBlue, 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Melodify Picks",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Surface(
                onClick = onRefreshPicks,
                shape = RoundedCornerShape(16.dp),
                color = GeminiCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBorder.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "Refresh Picks", 
                        tint = GeminiBlue, 
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Refresh", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = GeminiTextPrimary, 
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 14.dp
        ) { page ->
            val track = tracks[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GeminiCardBg)
                    .border(1.dp, GeminiBorder.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    .clickable { onSongPlay(track, tracks) }
            ) {
                FuturisticThumbnail(model = track.thumbnailUrl, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 80f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Surface(
                        color = GeminiBlue.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBlue.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            "✨ $timeLabel", 
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GeminiBlue,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        track.title, 
                        color = Color.White, 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold, 
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist, 
                        color = Color.White.copy(alpha = 0.75f), 
                        style = MaterialTheme.typography.bodyMedium, 
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SongCard(track: TrackEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, color = GeminiBlue)
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GeminiCardBg)
                .border(1.dp, GeminiBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
        ) {
            FuturisticThumbnail(
                model = track.thumbnailUrl, 
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title, 
            color = GeminiTextPrimary, 
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = track.artist, 
            color = GeminiTextSecondary, 
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            maxLines = 1, 
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

package com.ansh.ytmusicpreimum.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMelodifyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Melodify", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF00B29A), Color(0xFF00E5FF))),
                        RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Melodify",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "v1.0.0 Stable",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Melodify is a premium music experience designed for those who value quality and simplicity.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    AboutFeatureItem("High Fidelity", "Lossless audio streaming and downloads.", Icons.Default.HighQuality)
                    AboutFeatureItem("Cloud Library", "Sync your music across all devices.", Icons.Default.CloudSync)
                    AboutFeatureItem("Smart Mixes", "Personalized music for every mood.", Icons.Default.AutoAwesome)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Developed by Ansh Kesharwani",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Subscription Plans",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            UpgradeOption("PRO Monthly", "₹49", "Basic Pro features for 1 month", Color(0xFFE91E63)) {}
            Spacer(modifier = Modifier.height(8.dp))
            UpgradeOption("PRO Annual", "₹199", "Best value with Animated Badge", Color(0xFFFFD700)) {}
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "Terms • Privacy • Licenses",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AboutFeatureItem(title: String, desc: String, icon: ImageVector) {
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text(desc, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

package com.ansh.ytmusicpreimum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Privacy Policy for Melodify",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Last Updated: September 24, 2026\nApp Version: Melodify v2.0.0\nDeveloper: Ansh Kesharwani",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SectionTitle("Introduction")
            Text(
                "Welcome to Melodify (\"we,\" \"our,\" or \"us\"). We are committed to protecting your personal privacy. This Privacy Policy details how Melodify (the \"App\") handles user information, authentication, cloud sync, local caching, and media streaming in compliance with Samsung Galaxy Store and Google Play Store policies."
            )

            SectionTitle("1. Information We Collect and Cloud Synchronization")
            Text(
                "Melodify is built with a data-minimization approach. We do not sell, rent, or monetize your personal information."
            )
            BulletPoint("Google Account & Profile Data", "When you choose 'Continue with Google', we receive standard authentication tokens (Display Name, Email Address, and Avatar URL) through official Google Identity & Firebase Auth. This is used solely to personalize your experience and secure your account.")
            BulletPoint("Cloud Sync Service", "If signed in, your user-created playlists, favorite/liked tracks, and listening history are securely synchronized with Firebase Firestore under your unique User ID. This allows you to seamlessly restore your music library across device re-installations or phone upgrades.")
            BulletPoint("Local Audio & Storage", "Melodify accesses device storage (READ_MEDIA_AUDIO) solely to discover, index, and play your locally stored music tracks. Your local audio files never leave your device and are never uploaded to external servers.")
            BulletPoint("Offline Downloads", "Songs and media downloaded for offline listening are stored in your device's private app directory. They are removed upon app uninstallation.")

            SectionTitle("2. Media Streaming & Third-Party Services")
            Text(
                "Melodify acts as an advanced audio engine and media player. Online streaming data is fetched through public, decentralized API endpoints. Melodify does not host copyrighted media files on private proprietary servers. Third-party SDKs (such as Google Play Services and Firebase) may process anonymous technical telemetry to deliver crash reports and maintain system stability."
            )

            SectionTitle("3. App Permissions Explained")
            PermissionRow("Internet Access", "Required to stream music, fetch album artwork, retrieve lyrics, and sync cloud playlists.")
            PermissionRow("Storage / Media Audio", "Required to access and play local audio files on your device.")
            PermissionRow("Haptic Feedback & Vibrate", "Provides tactile feedback for gesture controls and playback buttons.")
            PermissionRow("Foreground Service & Media Playback", "Allows uninterrupted background playback and Lockscreen media controls.")

            SectionTitle("4. Data Security & User Control")
            Text(
                "We implement industry-standard encryption practices to protect account tokens and cloud sync endpoints. You retain full control over your data:"
            )
            BulletPoint("Clear Cache", "You can clear local cache, downloaded artwork, and temporary audio files anytime from Settings.")
            BulletPoint("Account Sign-Out", "Signing out immediately disconnects Google Cloud synchronization on the device.")
            BulletPoint("Data Deletion", "To permanently delete your synced cloud library and profile data, you can email the developer or disconnect the app from your Google Account permissions.")

            SectionTitle("5. Children's Privacy")
            Text(
                "Melodify does not knowingly collect personally identifiable information from children under the age of 13. If you believe a child has provided us personal data, contact us immediately and we will delete it promptly."
            )

            SectionTitle("6. Policy Revisions")
            Text(
                "We may update this Privacy Policy from time to time to accommodate new features or regulatory requirements. Continued use of Melodify following updates signifies your acceptance."
            )

            SectionTitle("7. Contact & Developer Information")
            Text(
                "For any privacy queries, feedback, or data requests, please contact:",
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                "Developer: Ansh Kesharwani\nEmail: anshkesharwani0807@gmail.com\nProject: Melodify Music Player",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun BulletPoint(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("• $title:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(description, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
fun PermissionRow(name: String, purpose: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("• ", fontWeight = FontWeight.Bold)
        Column {
            Text(name, fontWeight = FontWeight.Bold)
            Text(purpose, style = MaterialTheme.typography.bodySmall)
        }
    }
}

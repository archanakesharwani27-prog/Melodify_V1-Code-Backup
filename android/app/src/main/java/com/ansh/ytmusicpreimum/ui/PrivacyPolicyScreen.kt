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
                "Last Updated: June 26, 2026\nApp Version: v1.0.0\nDeveloper: Ansh Kesharwani",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SectionTitle("Introduction")
            Text(
                "Welcome to Melodify (\"we,\" \"our,\" or \"us\"). We are committed to protecting your privacy. This Privacy Policy explains how we handle your information when you use our mobile application Melodify (the \"App\")."
            )

            SectionTitle("1. Information We Collect and How We Use It")
            Text(
                "Melodify is designed to be a secure, user-centric offline and online audio player. We prioritize data minimization, meaning we do not collect your personal data unless strictly necessary for core app functionalities."
            )
            
            BulletPoint("Audio and Media Files", "The App requires access to your device's storage (READ_EXTERNAL_STORAGE / READ_MEDIA_AUDIO) to scan, index, and play local audio tracks. This data remains strictly on your device and is never uploaded to external servers.")
            BulletPoint("Device Permissions (Haptic Feedback)", "The App uses the device's vibrator subsystem to provide haptic feedback micro-interactions during playback controls (play, pause, next, previous).")
            BulletPoint("Crash Reporting and Performance Data", "To ensure stability across app updates (including v1.0.0 features), anonymous technical logs, device model info, and crash traces may be processed to fix bugs.")

            SectionTitle("2. Permissions Utilized")
            Text("To deliver the full multimedia experience, including our unique 3D book-page flip views, gestures, and audio features, Melodify utilizes the following system permissions:")
            
            PermissionRow("Storage / Media Audio", "To fetch local audio tracks, album arts, and metadata for offline playback.")
            PermissionRow("Internet Access", "To stream online music playlists and load dynamic visual content.")
            PermissionRow("Vibrate / Haptic Control", "To deliver tactile responses upon double/triple tap gesture registrations.")

            SectionTitle("3. Third-Party Services")
            Text("We do not sell, trade, or rent your personal information to third parties. If the app uses standard platform services (like Google Play Services) for core app delivery, these services may collect information used to identify your device in accordance with their respective privacy frameworks.")

            SectionTitle("4. Data Security")
            Text("The security of your data is highly important to us. Melodify implements standard security practices to protect local databases and resource routing. No method of transmission over the internet or electronic storage is 100% secure, but we strive to use commercially acceptable means to protect it.")

            SectionTitle("5. Children's Privacy")
            Text("Melodify does not address anyone under the age of 13. We do not knowingly collect personally identifiable information from children under 13. If we discover that a child under 13 has provided us with personal information, we immediately delete this from our logs.")

            SectionTitle("6. Changes to This Privacy Policy")
            Text("We may update our Privacy Policy from time to time to reflect changes in app iterations or legislative requirements. We advise you to review this page periodically for any changes.")

            SectionTitle("7. Contact Us")
            Text("If you have any questions, feedback, or suggestions about Melodify's privacy practices, do not hesitate to contact the developer:")
            Text(
                "Developer Name: Ansh Kesharwani\nEmail: anshkesharwani0807@gmail.com",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
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

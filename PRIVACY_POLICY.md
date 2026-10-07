# Privacy Policy for Melodify

**Last Updated:** September 24, 2026  
**App Version:** Melodify v2.0.0  
**Developer:** Ansh Kesharwani  
**Contact:** anshkesharwani27@gmail.com  

---

## Introduction
Welcome to **Melodify** ("we," "our," or "us"). We are committed to protecting your personal privacy. This Privacy Policy details how Melodify (the "App") handles user information, authentication, cloud sync, local caching, and media streaming in compliance with Samsung Galaxy Store and Google Play Store policies.

---

## 1. Information We Collect and Cloud Synchronization
Melodify is built with a data-minimization approach. We do not sell, rent, or monetize your personal information.

- **Google Account & Profile Data:** When you choose *'Continue with Google'*, we receive standard authentication tokens (Display Name, Email Address, and Avatar URL) through official Google Identity & Firebase Auth. This is used solely to personalize your experience and secure your account.
- **Cloud Sync Service:** If signed in, your user-created playlists, favorite/liked tracks, and listening history are securely synchronized with Firebase Firestore under your unique User ID. This allows you to seamlessly restore your music library across device re-installations or phone upgrades.
- **Local Audio & Storage:** Melodify accesses device storage (`READ_MEDIA_AUDIO`) solely to discover, index, and play your locally stored music tracks. Your local audio files never leave your device and are never uploaded to external servers.
- **Offline Downloads:** Songs and media downloaded for offline listening are stored in your device's private app directory. They are removed upon app uninstallation.

---

## 2. Media Streaming & Third-Party Services
Melodify acts as an advanced audio engine and media player. Online streaming data is fetched through public, decentralized API endpoints. Melodify does not host copyrighted media files on private proprietary servers. Third-party SDKs (such as Google Play Services and Firebase) may process anonymous technical telemetry to deliver crash reports and maintain system stability.

---

## 3. App Permissions Explained
- **Internet Access (`android.permission.INTERNET`):** Required to stream music, fetch album artwork, retrieve lyrics, and sync cloud playlists.
- **Storage / Media Audio (`android.permission.READ_MEDIA_AUDIO`):** Required to access and play local audio files on your device.
- **Foreground Service (`android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`):** Allows uninterrupted background playback and notification playback controls.
- **Notifications (`android.permission.POST_NOTIFICATIONS`):** Used to display playback controls in the status bar notification tray.
- **Audio Effects (`android.permission.RECORD_AUDIO`, `android.permission.MODIFY_AUDIO_SETTINGS`):** Used strictly for client-side dynamic audio visualization and equalizer effects. No voice or ambient audio is recorded or transmitted.
- **Haptic Feedback (`android.permission.VIBRATE`):** Provides tactile feedback for gesture controls and playback buttons.

---

## 4. Data Security & User Control
We implement industry-standard encryption practices to protect account tokens and cloud sync endpoints. You retain full control over your data:
- **Clear Cache:** You can clear local cache, downloaded artwork, and temporary audio files anytime from Settings.
- **Account Sign-Out:** Signing out immediately disconnects Google Cloud synchronization on the device.
- **Data Deletion:** To permanently delete your synced cloud library and profile data, you can email the developer or disconnect the app from your Google Account permissions.

---

## 5. Children's Privacy
Melodify does not knowingly collect personally identifiable information from children under the age of 13. If you believe a child has provided us personal data, contact us immediately and we will delete it promptly.

---

## 6. Policy Revisions
We may update this Privacy Policy from time to time to accommodate new features or regulatory requirements. Continued use of Melodify following updates signifies your acceptance.

---

## 7. Contact Us
For any privacy questions, inquiries, or data deletion requests:
- **Developer:** Ansh Kesharwani  
- **Email:** anshkesharwani27@gmail.com  
- **GitHub Repository:** [Melodify GitHub](https://github.com/archanakesharwani27-prog/Melodify_V1-Code-Backup)

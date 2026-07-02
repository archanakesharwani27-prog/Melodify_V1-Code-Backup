# 🧠 Melodify App Brain (Knowledge Base)

## 📋 General Information
- **Project Name:** Melodify (formerly AuraPlayer)
- **Developer:** Ansh Kesharwani
- **App Type:** Premium Music Streaming & Offline Player
- **Core Vision:** To provide a high-quality, ad-free YouTube Music experience with a futuristic, minimalist UI and robust offline capabilities.

## 🛠 Tech Stack
- **UI Framework:** Jetpack Compose (Modern Android UI)
- **Language:** Kotlin
- **Media Engine:** Media3 (ExoPlayer) with MediaSession integration.
- **Backend/Extraction:** NewPipeExtractor (YouTube scraping)
- **Database:** Room (Local storage for Playlists, History, Liked songs, and Cache)
- **Cloud/Auth:** Firebase (Authentication, Firestore sync, Remote Config)
- **Image Loading:** Coil
- **Concurrency:** Kotlin Coroutines & Flow

## 🏗 Architecture & Structure
The app follows the **MVVM (Model-View-ViewModel)** architecture:

- **MusicViewModel:** The central hub for playback. Manages the MediaController, play/pause, seek, volume, shuffle, repeat, and current queue.
- **HomeViewModel:** Handles data fetching for the discovery screen (Trending, For You, Mood Mixes, Global Hits).
- **LibraryViewModel:** Interface for Room Database. Manages user data (Liked tracks, Playlists, Playback history).
- **SearchViewModel:** Handles debounced YouTube searching and history.
- **YouTubeExtractor:** Object responsible for resolving YouTube IDs to playable stream URLs with an integrated memory + database caching system (expiry 5 hours).
- **MelodifyDownloadManager:** Manages downloading tracks in multiple qualities (Low, Medium, High) with support for cellular/WiFi network restrictions.

## 🚀 Key Features & UI Elements
- **Predictive Prefetching:** Prefetches stream URLs for upcoming tracks in the queue and trending lists to ensure near-instant playback.
- **Futuristic UI:** Dark theme base, Glassmorphic cards, neon accents, and smooth animated transitions.
- **Enhanced Player:** 
  - **Portrait:** Immersive artwork focus with skip-gestures.
  - **Landscape:** Dedicated large vertical volume slider (1-15 steps) and easy-access like button.
- **Fly-to-Notification Animation:** Custom animation when starting a download, moving the track thumbnail toward the notification area.
- **Lyrics Support:** Integrated lyrics fetching and caching.

## 📈 Evolution Records (v2.0.0-dev)
- **Simplified UX:** Removed the Equalizer to maintain a cleaner, performance-oriented experience.
- **Official Priority:** Updated search logic to prioritize "Official Audio" to ensure high-quality content.
- **Landscape Overhaul:** Redesigned the landscape player for better ergonomics during tablet or horizontal usage.
- **Performance:** Optimized background background processing and state management to reduce UI flicker during transitions.

## 🧭 Developer/AI Onboarding
To understand the codebase quickly:
1. Start with `MainActivity.kt` to see the screen navigation and UI flow.
2. Check `MusicViewModel.kt` for core logic.
3. Review `YouTubeExtractor.kt` to understand how YouTube data is processed.
4. See `TrackEntity.kt` for the data structures used across the app.

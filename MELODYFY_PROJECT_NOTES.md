# Melodify Project Documentation & Refactoring Roadmap

## Overview
Melodify is an Android music streaming and local player application built using **Kotlin**, **Jetpack Compose**, **AndroidX Media3 / ExoPlayer**, **Room Database**, and **Firebase**.

---

## Architecture Evolution & Milestones

### Phase 1: Monolithic UI Decomposition (Completed)
- **Problem**: Monolithic `MainActivity.kt` containing 4,063 lines of code hosting all screens, dialogs, canvas animations, and players.
- **Solution**:
  - `MainActivity.kt` slimmed down to 15 lines as pure `ComponentActivity` entry.
  - `AppEntry.kt` hosts top-level app theme, splash transitions, and auth gating.
  - `ui/MainScreen.kt` hosts core navigation shell, player overlay, and bottom navigation bar.
  - Reusable components extracted into `ui/components/` (`TrackItem`, `Skeletons`, `CommonComponents`, `DownloadFlyAnimation`).
  - Feature screens decomposed into `ui/screens/` (`home/`, `search/`, `library/`, `player/`, `settings/`).

### Phase 2: ViewModels & Data Layer Decoupling (Completed)
- **HomeViewModel Architecture**:
  - Migrated from generic `ViewModel` to `AndroidViewModel(application)` with self-contained `TrackRepository`.
  - Eliminated the fragile `LaunchedEffect { homeViewModel.setRepository(libraryViewModel.repository) }` hack from `HomeScreen.kt`.
  - Added structured `HomeUiState` presentation model.
  - Added resilient per-section network error handling so single query timeouts do not break the whole home feed.
  - Added offline fallback: recently played/cached Room tracks are served if network is offline.
- **SearchViewModel Optimizations**:
  - Implemented in-memory LRU search cache (`LruCache`) to provide 0ms instant results on repeat queries and backspaces.
  - Added instant local Room database search matching while debouncing external YouTube extractor queries.
  - Introduced typed `SearchUiState` (Idle, Loading, Success, Empty, Error).
- **Library & Playlist Enhancements**:
  - Added `getPlaylistTrackCount` to `TrackDao` and `TrackRepository`.
  - Fixed `addTrackToPlaylist` in `LibraryViewModel` so new tracks append at the correct sequential position rather than hardcoded 0.
- **Data Layer Hardening**:
  - Added Room search query `searchLocalTracks` in `TrackDao` for fast indexed matching on track title and artist.

### Phase 3: Playback, Caching & Download Reliability (Completed)
- **Silent Stream Auto-Recovery**:
  - Added `onPlayerError` listener in `MusicService.kt` that detects expired YouTube stream URLs (HTTP 403 / 410 or network resets).
  - Automatically invalidates stale stream cache via `YouTubeExtractor.invalidateCache(videoId)` and `TrackDao.deleteStreamCache(videoId)`.
  - Resolves fresh audio stream and silently resumes playback from the user's exact position without crashing.
- **Foreground Service Lifecycle**:
  - Updated `onTaskRemoved` in `MusicService.kt` to preserve playback in the foreground when audio is actively playing, preventing unexpected music cutoff when the app is swiped away from recent tasks.
- **Download Manager Resilience**:
  - Added auto-retry logic (up to 2 attempts) with automatic stale URL re-resolution in `MelodifyDownloadManager.kt`.
  - Guaranteed partial/corrupted file cleanup (`file.delete()`) on download cancellation or error.
  - Set `NotificationManager.IMPORTANCE_LOW` for download notifications to prevent buzzing/beeping during download progress updates.
  - Added `retryDownload` method for manual restart of failed downloads.
- **Startup Stability**:
  - Initialized `NewPipe.init` synchronously in `MelodifyApplication` (`AuraPlayerApplication.kt`) to eliminate cold-start race conditions.

### Phase 4: Automated Testing & Production Hardening (Completed)
- **Automated Unit Tests Added**:
  - `TrackEntityTest.kt`: Unit test coverage for data model defaults, copy semantics, and download flags.
  - `HomeUiStateTest.kt`: Unit tests for Home state transitions, immutability, and data binding.
  - `SearchUiStateTest.kt`: Unit tests covering `SearchUiState` sealed classes (`Idle`, `Loading`, `Success`, `Empty`, `Error`).
  - `FormatTimeUnitTest.kt`: Unit tests for playback duration and progress formatting.
- **Production ProGuard / R8 Rules**:
  - Added explicit keep rules for Room Database and Daos.
  - Preserved Firebase Firestore serialization classes and property annotations.
  - Preserved NewPipe Extractor and AndroidX Media3 / ExoPlayer interfaces from obfuscation damage.
  - Preserved debug line numbers for crash analysis.

### Phase 5: Security Hardening, Power & Rendering Performance (Completed)
- **Hardcoded Secret Removal (P0 Security Fix)**:
  - Deprecated legacy `YouTubeSearchService.kt` and eliminated exposed hardcoded Google API key (`AIzaSy...`).
- **Database Wipe Protection (P0 Data Integrity Fix)**:
  - Replaced reckless `.fallbackToDestructiveMigration()` in `AppDatabase.kt` with `.fallbackToDestructiveMigrationOnDowngrade()`, ensuring user playlists, favorites, and downloaded songs are never wiped on future schema version bumps.
- **Audio Visualizer Battery & Garbage Collection Optimization**:
  - Refactored `LiveWaveCanvas.kt` to eliminate 1,400+ per-second `Brush` and `List` allocations inside the canvas draw loop.
  - Replaced continuous frame `FloatArray.copyOf()` allocations with in-place remembered array updates, drastically reducing CPU/GPU overhead and thermal throttling during playback.
- **Android 13+ (API 33) Runtime Notification Permissions**:
  - Implemented standard `POST_NOTIFICATIONS` runtime prompt in `MainScreen.kt`, ensuring foreground playback and download notifications appear reliably across Android 13, 14, and 15 devices.
- **Compose Recomposition & FPS Boost**:
  - Annotated core Room and domain models (`TrackEntity`, `PlaylistEntity`, `HistoryEntity`, etc.) with `@Immutable` so the Jetpack Compose compiler can skip list row recompositions during fast scrolling.
- **Network-Aware Download Queue**:
  - Integrated `ConnectivityManager.NetworkCallback` in `MelodifyDownloadManager.kt` to automatically pause downloads when off-network and resume processing seamlessly as soon as preferred network connectivity (e.g. Wi-Fi) is restored.
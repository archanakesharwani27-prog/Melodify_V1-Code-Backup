# Melodify Project Documentation & Refactoring Roadmap

## Overview
Melodify is an Android music streaming and local player application built using **Kotlin**, **Jetpack Compose**, **AndroidX Media3 / ExoPlayer**, **Room Database**, and **Firebase**.

---

## Architecture Evolution

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

---

## Remaining Phases

### Phase 3: Playback, Caching & Download Reliability
- Media3 foreground service lifecycle management and audio focus.
- Stream URL expiry handling and silent auto-refresh.
- Download queue retry mechanisms and cellular data guards.

### Phase 4: Automated Testing & Production Polish
- Unit tests for ViewModels, TrackDao, and Repository.
- UI state instrumentation tests.
- ProGuard rules and release build verification.
# Melodify

Melodify is an Android music application built with Kotlin and Jetpack Compose. It combines local library features, online discovery, Firebase sync, and media playback in a single mobile app.

## Project status

This is an active prototype / development build with the following features already present:

- Music search and discovery
- Player UI and media playback
- Local Room database for favorites, history, playlists, and caches
- Firebase authentication and Firestore sync
- Offline/download support
- NewPipe-based YouTube extraction for audio sources
- Material 3 UI and Android app structure

## Tech stack

- Kotlin
- Jetpack Compose
- Android Media3 / ExoPlayer
- Room persistence library
- Firebase Auth, Firestore, Remote Config, App Check
- NewPipe Extractor
- Coil for image loading
- OkHttp
- Gradle Kotlin DSL

## App structure

The main application code is under:

- android/app/src/main/java/com/ansh/ytmusicpreimum
- android/app/src/main/java/com/ansh/ytmusicpreimum/data
- android/app/src/main/java/com/ansh/ytmusicpreimum/ui

## Core app flow

1. The app launches from MainActivity and enters the splash/login flow.
2. After authentication, the user reaches the main screen and navigation shell.
3. Music playback is handled through Media3 and a foreground service.
4. Search and metadata fetching rely on the YouTube extractor.
5. Library and user state are persisted in Room and synced to Firebase when available.

## Important notes

- This project is feature-rich but still in a development stage.
- The UI is heavily centralized in MainActivity, which is a maintainability risk.
- The app relies on external media extraction and should be reviewed for legal and compliance concerns.
- The project is closer to a prototype architecture than a clean production architecture.

## How to run

Requirements:

- Android Studio
- JDK 17
- Android SDK with API 35 support
- Firebase project configuration if you want full auth/firestore features

Steps:

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Connect an Android device or emulator.
4. Run the app configuration for the Android app module.
5. If Firebase features are required, ensure google-services.json is configured correctly.

## Build commands

From the Android project folder:

```bash
./gradlew assembleDebug
./gradlew test
```

## Improvement roadmap

The project is currently being improved in the following order:

1. Documentation cleanup and architecture clarity
2. Refactoring large monolithic screen logic
3. Better separation between data and presentation layers
4. Stronger error handling for media extraction and downloads
5. Cleaner repository and cache design
6. Expanded automated testing
7. Production hardening and app polish

## Current focus

This repository is being improved toward:

- cleaner architecture
- more maintainable code organization
- stronger user and app reliability
- safer project documentation for future work

## Relevant files

- [android/app/build.gradle.kts](android/app/build.gradle.kts)
- [android/app/src/main/java/com/ansh/ytmusicpreimum/MainActivity.kt](android/app/src/main/java/com/ansh/ytmusicpreimum/MainActivity.kt)
- [android/app/src/main/java/com/ansh/ytmusicpreimum/YouTubeExtractor.kt](android/app/src/main/java/com/ansh/ytmusicpreimum/YouTubeExtractor.kt)
- [android/app/src/main/java/com/ansh/ytmusicpreimum/AuraPlayerApplication.kt](android/app/src/main/java/com/ansh/ytmusicpreimum/AuraPlayerApplication.kt)
- [android/app/src/main/java/com/ansh/ytmusicpreimum/data/TrackRepository.kt](android/app/src/main/java/com/ansh/ytmusicpreimum/data/TrackRepository.kt)

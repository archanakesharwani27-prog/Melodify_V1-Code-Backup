# Melodify Production ProGuard & R8 Optimization Rules

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room Persistence Library
-keep class androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Melodify Data Entities & Models
-keep class com.ansh.ytmusicpreimum.data.** { *; }
-keepclassmembers class com.ansh.ytmusicpreimum.data.** { *; }
-keep class com.ansh.ytmusicpreimum.ui.**UiState** { *; }

# NewPipe Extractor
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# Firebase & Firestore Serialization
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# AndroidX Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# ─────────────────────────────────────────────────────────────────────────────
# Proguard / R8 Optimization Rules for Blaupunkt Automotive Media Player
# ─────────────────────────────────────────────────────────────────────────────

# 1. Native Audio JNI Bridge & Callbacks
-keepclassmembers class * {
    native <methods>;
}

-keep class com.aashik.music.nativeaudio.NativeAudioBridge {
    *;
}

-keep interface com.aashik.music.nativeaudio.NativeAudioBridge$PlaybackListener {
    *;
}

# 2. Room Database Entities and DAOs
-keep class com.aashik.music.model.Song { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# 3. Android Auto / MediaSession & Compose
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    *;
}

# 4. Strip logging in release builds for maximum speed
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 5. Optimize bytecode and inline methods aggressively
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

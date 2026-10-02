# R8 / ProGuard rules for Kaggle Controller
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Service

# Compose / Material
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-keepclassmembers class androidx.compose.ui.tooling.data.UiToolingDataApi { *; }

# Hilt / Dagger
-keepclassmembers,allowobfuscation class * {
    @dagger.* <fields>;
    @javax.inject.* <fields>;
}
-keepclassmembers class * {
    @androidx.hilt.InstallIn <fields>;
}

# Retrofit / OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

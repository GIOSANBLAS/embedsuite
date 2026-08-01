# EMBED SUITE release rules
-keep class com.embedsuite.app.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn org.osmdroid.**
-dontwarn okhttp3.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
# OkHttp / JSON
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class org.json.** { *; }
# usb-serial-for-android (reflective driver probe)
-keep class com.hoho.android.usbserial.** { *; }
-dontwarn com.hoho.android.usbserial.**
# SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
# BuildConfig
-keep class com.embedsuite.app.BuildConfig { *; }

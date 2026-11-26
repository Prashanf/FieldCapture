# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson models & extraction schemas
-keep class com.fieldcapture.domain.model.** { *; }
-keep class com.fieldcapture.extraction.** { *; }
-keep class com.fieldcapture.data.remote.** { *; }
-keep class com.fieldcapture.eval.EvalRunner$** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }

# AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }

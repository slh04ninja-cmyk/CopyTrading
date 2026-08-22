# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Keep data models
-keep class com.copytrading.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod

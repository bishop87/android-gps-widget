# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK's proguard-android-optimize.txt

# Keep data classes used by Retrofit/Gson
-keepclassmembers class com.example.gpstracker.data.api.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions

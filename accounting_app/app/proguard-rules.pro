# Add project specific ProGuard rules here.

# Keep model classes
-keep class com.accounting.app.models.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp and Conscrypt
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class org.conscrypt.** { *; }
-keep class org.conscrypt.Conscrypt { *; }
-keep class org.conscrypt.Conscrypt$Version { *; }
-keep class org.conscrypt.ConscryptHostnameVerifier { *; }
-keep class org.conscrypt.ConscryptProvider { *; }
-keep class org.conscrypt.KitKatPlatformOpenSSLSocketImplAdapter { *; }
-keep class org.conscrypt.PreKitKatPlatformOpenSSLSocketImplAdapter { *; }
-keep class org.conscrypt.AbstractConscryptSocket { *; }
-keep class com.android.org.conscrypt.SSLParametersImpl { *; }
-keep class org.apache.harmony.xnet.provider.jsse.SSLParametersImpl { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Android
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class android.** { *; }
-keep interface android.** { *; }

# Keep JavaScript interface
-keepclassmembers class com.accounting.app.WebAppInterface {
    public <methods>;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep R8 rules
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep SSL/TLS related classes
-keep class javax.net.ssl.** { *; }
-keep class javax.net.** { *; }
-keep class java.security.** { *; }
-keep class java.security.cert.** { *; }

# Keep Android SSL implementation
-keep class com.android.org.conscrypt.** { *; }
-keep class org.apache.harmony.xnet.provider.jsse.** { *; } 
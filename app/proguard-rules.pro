# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep attributes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions

# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep PdfBox and related libraries
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.pdfbox.android.** { *; }
-keep class com.tom_roush.pdfbox.filter.** { *; }
-keep class com.tom_roush.pdfbox.pdmodel.** { *; }
-keep class com.tom_roush.pdfbox.cos.** { *; }
-keep class com.tom_roush.pdfbox.util.** { *; }

# Keep image processing libraries
-keep class com.gemalto.jp2.** { *; }
-keep class org.apache.pdfbox.jbig2.** { *; }
-keep class org.apache.pdfbox.filter.** { *; }

# Keep your model classes
-keep class com.hillal.hhhhhhh.data.model.** { *; }
-keep class com.hillal.hhhhhhh.data.remote.** { *; }
-keep class com.hillal.hhhhhhh.ui.** { *; }
-keep class com.hillal.hhhhhhh.utils.** { *; }
-keep class com.hillal.hhhhhhh.models.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep @androidx.room.TypeConverters class *
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.RoomDatabase$Migration

# Keep Navigation Component
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable
-keepnames class androidx.navigation.** { *; }

# Keep ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
}

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.** {
    volatile <fields>;
}

# Keep Lifecycle
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.lifecycle.AndroidViewModel
-keep class * extends androidx.lifecycle.LiveData
-keep class * extends androidx.lifecycle.MutableLiveData
-keep class * extends androidx.lifecycle.MediatorLiveData

# Keep Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep AndroidX and Android
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class android.** { *; }
-keep interface android.** { *; }

# Keep Security Libraries
-keep class org.conscrypt.** { *; }
-keep class com.android.org.conscrypt.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class org.bouncycastle.jsse.** { *; }
-keep class org.bouncycastle.jsse.provider.** { *; }
-keep class org.openjsse.** { *; }

# Keep SSL/TLS related classes
-keep class org.conscrypt.** { *; }
-keep class com.android.org.conscrypt.** { *; }
-keep class org.apache.harmony.xnet.provider.jsse.** { *; }
-keep class org.bouncycastle.jsse.** { *; }
-keep class org.bouncycastle.jsse.provider.** { *; }
-keep class org.openjsse.** { *; }
-keep class org.openjsse.javax.net.ssl.** { *; }
-keep class org.openjsse.net.ssl.** { *; }

# Keep OkHttp platform classes
-keep class okhttp3.internal.platform.** { *; }
-keep class okhttp3.internal.platform.android.** { *; }
-keep class okhttp3.internal.platform.AndroidPlatform { *; }
-keep class okhttp3.internal.platform.ConscryptPlatform { *; }
-keep class okhttp3.internal.platform.BouncyCastlePlatform { *; }
-keep class okhttp3.internal.platform.OpenJSSEPlatform { *; }

# Keep SSL/TLS interfaces
-keep interface javax.net.ssl.** { *; }
-keep interface org.conscrypt.** { *; }
-keep interface org.bouncycastle.jsse.** { *; }
-keep interface org.openjsse.javax.net.ssl.** { *; }

# Keep SSL/TLS implementations
-keep class * implements javax.net.ssl.SSLSocket { *; }
-keep class * implements javax.net.ssl.SSLServerSocket { *; }
-keep class * implements javax.net.ssl.SSLSocketFactory { *; }
-keep class * implements javax.net.ssl.SSLServerSocketFactory { *; }
-keep class * implements javax.net.ssl.SSLContext { *; }
-keep class * implements javax.net.ssl.TrustManager { *; }
-keep class * implements javax.net.ssl.X509TrustManager { *; }

# تحسينات عامة
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# تقليل حجم الكود
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# تقليل حجم الموارد
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
    public void *(android.view.Menu);
}

# تقليل حجم المكتبات
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
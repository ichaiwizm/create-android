# ============================================================================
# Create — règles R8 / ProGuard (build release minifié).
# ============================================================================

# --- Attributs génériques nécessaires à Retrofit / Moshi (réflexion) ---------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# --- Moshi (adapter réflexif Kotlin : KotlinJsonAdapterFactory) --------------
# Les DTO réseau et modèles domaine sont (dé)sérialisés par réflexion : on garde
# leurs membres et les métadonnées Kotlin indispensables au reflect.
-keep class com.wizycode.create.core.net.dto.** { *; }
-keep class com.wizycode.create.data.model.** { *; }
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn org.jetbrains.annotations.**

# Moshi interne.
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# --- Retrofit ----------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# Conserve les interfaces d'API (proxies dynamiques).
-keep,allowobfuscation interface com.wizycode.create.core.net.CreateApi
-keep,allowobfuscation interface com.wizycode.create.core.net.PbAuthApi

# --- OkHttp / Okio -----------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Coroutines --------------------------------------------------------------
-dontwarn kotlinx.coroutines.**

# --- Media3 / ExoPlayer (règles consommateurs incluses ; garde-fou) ----------
-dontwarn androidx.media3.**

# --- Firebase Cloud Messaging (M7, dormant) ----------------------------------
-dontwarn com.google.firebase.**
-keep class com.wizycode.create.core.push.** { *; }

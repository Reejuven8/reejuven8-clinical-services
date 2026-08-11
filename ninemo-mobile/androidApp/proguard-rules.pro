# Keep kotlinx.serialization models + generated serializers (obfuscate everything else).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.reejuven8.ninemo.shared.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.reejuven8.ninemo.shared.model.**$$serializer { *; }
-keepclasseswithmembers class com.reejuven8.ninemo.shared.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor / OkHttp
-dontwarn org.slf4j.**
-dontwarn okhttp3.**

# kotlinx.serialization generated serializers for navigation routes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class dev.ao3m3.navigation.** {
    *** Companion;
}
-keepclasseswithmembers class dev.ao3m3.navigation.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room generated implementations are referenced reflectively at compile time only.
-keep class dev.ao3m3.core.data.db.**_Impl { *; }

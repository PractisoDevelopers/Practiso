-dontwarn nl.adaptivity.xmlutil.jdk.StAXWriter
-optimizations !method/specialization/**
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { *; }
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}
-keep class org.sqlite.** { *; }
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
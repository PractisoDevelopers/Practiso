-dontwarn nl.adaptivity.xmlutil.jdk.StAXWriter
-dontwarn ai.onnxruntime.platform.Fp16Conversions
-dontwarn io.objectbox.ideasonly.ModelModifier$PropertyModifier
-optimizations !method/specialization/**
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { *; }
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}
-keep class org.sqlite.** { *; }
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class ai.onnxruntime.** { *; }
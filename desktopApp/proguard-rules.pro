-dontwarn nl.adaptivity.xmlutil.jdk.StAXWriter
-dontwarn ai.onnxruntime.platform.Fp16Conversions
-dontwarn io.objectbox.ideasonly.ModelModifier$PropertyModifier
-dontwarn javax.annotation.*
-dontwarn okhttp3.internal.**
-dontwarn io.ktor.network.sockets.*
-dontwarn androidx.compose.material3.internal.*
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

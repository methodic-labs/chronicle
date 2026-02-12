# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# Preserve Jackson polymorphic metadata in release builds.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Keep interface type metadata used for polymorphic serialization.
-keep interface com.openlattice.chronicle.android.ChronicleSample
-keep interface com.openlattice.chronicle.sources.SourceDevice

# Keep concrete polymorphic types that are serialized/deserialized by the app.
-keep class com.openlattice.chronicle.android.ChronicleUsageEvent { *; }
-keep class com.openlattice.chronicle.sources.AndroidDevice { *; }
-keep class com.openlattice.chronicle.models.ExtractedUsageEvent { *; }

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

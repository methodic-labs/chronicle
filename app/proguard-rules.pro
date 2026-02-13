# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# ── Kotlin runtime & metadata ────────────────────────────────────────────────
# R8 in AGP 9 aggressively strips Kotlin internals.  Intrinsics contains
# null-check helpers (checkNotNullParameter, etc.) that the compiler injects
# at every non-null parameter boundary.  Stripping them causes
# NoSuchMethodError at the very start of any Kotlin function.
-keep class kotlin.jvm.internal.Intrinsics { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# ── WorkManager Workers ─────────────────────────────────────────────────────
# WorkManager instantiates workers via reflection.  AGP 9's R8 may reorder
# field init, inline doWork(), or merge classes — keep the full class graph.
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ── Jackson polymorphic serialization ────────────────────────────────────────
# Keep interface type metadata used for polymorphic serialization.
-keep interface com.openlattice.chronicle.android.ChronicleSample
-keep interface com.openlattice.chronicle.sources.SourceDevice

# Keep concrete polymorphic types that are serialized/deserialized by the app.
-keep class com.openlattice.chronicle.android.ChronicleUsageEvent { *; }
-keep class com.openlattice.chronicle.sources.AndroidDevice { *; }
-keep class com.openlattice.chronicle.models.ExtractedUsageEvent { *; }
-keep class com.openlattice.chronicle.models.ExtractedActivities { *; }
-keep class com.openlattice.chronicle.models.ExtractUsageStat { *; }

# Keep Jackson core & module classes from being merged / renamed.
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# ── chronicle-api dependency ─────────────────────────────────────────────────
# Prevent R8 from stripping/renaming classes in the API dependency that are
# used via reflection or Jackson polymorphism.
-keep class com.openlattice.chronicle.android.** { *; }
-keep class com.openlattice.chronicle.sources.** { *; }
-keep class com.openlattice.chronicle.data.** { *; }

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

# MediaPipe reads its protobuf options by reflection; minifying them crashes the landmarkers in release builds.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
# MediaPipe logs through Flogger, which finds its caller by class name on the stack; renamed classes crash Graph's init.
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**

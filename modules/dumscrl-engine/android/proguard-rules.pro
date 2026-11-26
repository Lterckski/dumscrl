# MediaPipe reads its protobuf options by reflection; minifying them crashes the landmarkers in release builds.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**

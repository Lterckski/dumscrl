plugins {
    id("com.android.application")
}

android {
    namespace = "com.lterckski.gesturescroll"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lterckski.gesturescroll"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    androidResources {
        noCompress += "task"
    }
}

dependencies {
    implementation("androidx.camera:camera-camera2:1.7.0")
    implementation("androidx.camera:camera-lifecycle:1.7.0")
    implementation("androidx.camera:camera-view:1.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.11.0")
    implementation("com.google.mediapipe:tasks-vision:0.10.26.1")
    testImplementation("junit:junit:4.13.2")
}

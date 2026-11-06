// BlurView 4.0 (https://github.com/Dimezis/BlurView, Apache-2.0) — copied from BlurView-master.zip
plugins {
    id("com.android.library")
}

android {
    namespace = "com.eightbitlab.blurview"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
}

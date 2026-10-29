// Same sources as :sample, built on the floor versions the library supports (Compose BOM
// 2024.09.00 = Compose 1.7.0 / material3 1.3.0, compileSdk/targetSdk 35, minSdk 26).
// :sample uses the latest Compose BOM, so between them the library runs on both ends.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    // Shares sample's sources, which reference ai.asleep.compose.wheelpicker.sample.R.
    namespace = "ai.asleep.compose.wheelpicker.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.asleep.compose.wheelpicker.sample.floor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-floor"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("../sample/src/main/AndroidManifest.xml")
            // AGP 9 built-in Kotlin reads .kt files from kotlin.srcDir, not java.srcDir.
            kotlin.directories.add("../sample/src/main/java")
            res.srcDir("../sample/src/main/res")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":wheelpicker"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

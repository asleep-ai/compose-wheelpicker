import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "ai.asleep.compose.wheelpicker"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
}

// Maven Central publishing — see .github/workflows/publish.yml for CI setup.

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true,
        )
    )

    coordinates(
        groupId = "ai.asleep",
        artifactId = "compose-wheelpicker",
        version = "0.1.0",
    )

    pom {
        name.set("Compose WheelPicker")
        description.set("A cylindrical wheel picker and time picker for Jetpack Compose.")
        url.set("https://github.com/asleep-ai/compose-wheelpicker")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("asleep")
                name.set("Asleep, Inc.")
                email.set("platform-cs@asleep.ai")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/asleep-ai/compose-wheelpicker.git")
            developerConnection.set("scm:git:ssh://github.com/asleep-ai/compose-wheelpicker.git")
            url.set("https://github.com/asleep-ai/compose-wheelpicker")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()
}

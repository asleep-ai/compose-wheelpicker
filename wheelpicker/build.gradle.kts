import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "ai.asleep.compose.wheelpicker"
    // Lowest compileSdk among consuming apps (med-apnotrack). Compose 1.7 needs 34.
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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

// Keep the published artifact readable by Kotlin 1.9 consumers:
// metadata/API level 1.9 and a kotlin-stdlib 1.9.x dependency in the POM.
kotlin {
    coreLibrariesVersion = "1.9.24"
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_1_9)
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
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
        version = project.findProperty("VERSION_NAME")?.toString() ?: "0.1.0-SNAPSHOT",
    )

    pom {
        name.set("Compose WheelPicker")
        description.set("A cylindrical wheel picker and time picker for Jetpack Compose.")
        url.set("https://github.com/asleep-ai/compose-wheelpicker")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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
            connection.set("scm:git:https://github.com/asleep-ai/compose-wheelpicker.git")
            developerConnection.set("scm:git:ssh://github.com/asleep-ai/compose-wheelpicker.git")
            url.set("https://github.com/asleep-ai/compose-wheelpicker")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Release publishing (publish.yml) always provides the key. Without it (local or the
    // consumer-check CI job publishing to mavenLocal) skip signing instead of failing.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}

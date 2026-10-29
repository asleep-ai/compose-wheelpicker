# consumer-check

Builds a small app with the oldest toolchain among the apps that use compose-wheelpicker
(med-apnotrack-app-android): AGP 8.5.2, Gradle 8.7, Kotlin 1.9.24, Compose compiler 1.5.14,
compileSdk/targetSdk 35, minSdk 26, Compose 1.7.2, material3 1.3.0.

It checks that the published artifact
- compiles in a Kotlin 1.9 / Compose compiler 1.5.x app, and
- does not raise the app's kotlin-stdlib or Compose versions.

```sh
./gradlew :wheelpicker:publishToMavenLocal -PVERSION_NAME=0.0.0-consumer-check -PRELEASE_SIGNING_ENABLED=false
gradle -p consumer-check verifyResolvedVersions :app:assembleDebug   # Gradle 8.7
```

CI runs this in the `consumer-check` job.

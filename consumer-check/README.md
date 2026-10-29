# consumer-check

Builds a small app with the oldest toolchain among the apps that use compose-wheelpicker
(med-apnotrack-app-android): AGP 8.5.2, Gradle 8.7, Kotlin 1.9.24, Compose compiler 1.5.14,
compileSdk/targetSdk 35, minSdk 26, Compose 1.7.2, material3 1.3.0.

It checks that the published artifact
- compiles in a Kotlin 1.9 / Compose compiler 1.5.x app, and
- does not raise the app's kotlin-stdlib or Compose versions.

```sh
./gradlew :wheelpicker:publishToMavenLocal -PVERSION_NAME=0.0.0-consumer-check
gradle -p consumer-check -PwheelpickerVersion=0.0.0-consumer-check :app:verifyResolvedVersions :app:assembleDebug   # Gradle 8.7
```

CI runs this in the `consumer-check` job.

`PickerInteractionTest` drags `WheelPicker` and `TimePicker` and checks that the callbacks reach the app. Run it locally on an API 26 (minSdk) emulator:

```sh
gradle -p consumer-check -PwheelpickerVersion=0.0.0-consumer-check :app:connectedDebugAndroidTest
```

CI does not run it because it needs an emulator.

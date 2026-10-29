# compose-wheelpicker

[![Maven Central](https://img.shields.io/maven-central/v/ai.asleep/compose-wheelpicker)](https://central.sonatype.com/artifact/ai.asleep/compose-wheelpicker)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

iOS-style wheel picker components for Jetpack Compose — a generic `WheelPicker` and a composite `TimePicker`.

## Installation

Available on Maven Central.

```kotlin
dependencies {
    implementation("ai.asleep:compose-wheelpicker:0.3.0")
}
```

### Requirements

Minimum app setup for 0.3.0:

- `minSdk` 26, `compileSdk` 35
- Kotlin 1.9.24 (Compose compiler 1.5.14) or newer
- Compose 1.7.0 / material3 1.3.0 or newer
- AGP 8.5.2 / Gradle 8.7 or newer

0.2.0 and earlier pulled Kotlin 2.2 and Compose 1.10 into the app and needed `compileSdk` 36 (0.1.0 also needed `minSdk` 28).

Since 0.3.0 the artifact is built with Kotlin `apiVersion`/`languageVersion` 1.9 and depends on `kotlin-stdlib` 1.9.24 and Compose BOM `2024.09.00`, so it works in apps that use Kotlin 1.9 with the Compose compiler 1.5.x and does not raise the app's Kotlin or Compose versions. The [`consumer-check`](consumer-check) build verifies this in CI.

Browse releases on [Maven Central](https://central.sonatype.com/artifact/ai.asleep/compose-wheelpicker) · [mvnrepository](https://mvnrepository.com/artifact/ai.asleep/compose-wheelpicker).

> `minSdk 26` is required because the public API exposes `java.time.LocalTime`, which is part of the Android platform since API 26.

## Modules

- `:wheelpicker` — library module, exposes `WheelPicker` + `TimePicker` composables
- `:sample` — demo app showcasing usage, on the latest Compose (compileSdk 36)
- `:sample-floor` — the same demo built on the minimum supported versions (Compose 1.7.0 / material3 1.3.0, compileSdk 35)
- `consumer-check/` — standalone app on the oldest supported toolchain (Kotlin 1.9.24, AGP 8.5.2) that consumes the published artifact

## Features

- Cylindrical 3D scroll animation with per-item curvature and edge fade
- Cylindrical-aware container sizing (no dead space at top/bottom as `visibleItemCount` grows)
- Infinite / repeatable scroll mode
- 12-hour (AM/PM) and 24-hour time formats with configurable AM/PM position
- Locale-aware default formatting for hours, minutes, and period labels
- Fully themable via `MaterialTheme` defaults, with per-parameter overrides

## TimePicker

A composite picker for selecting a `LocalTime`. In 12-hour mode it shows three wheels (AM/PM, hour, minute); in 24-hour mode the AM/PM wheel is hidden.

```kotlin
var time by remember { mutableStateOf(LocalTime.of(7, 30)) }

TimePicker(
    initialTime = time,
    onTimeChange = { time = it },
    modifier = Modifier.fillMaxWidth(),
)
```

### Key parameters

| Parameter | Default | Description |
|---|---|---|
| `initialTime` | — | Initial `LocalTime`. |
| `onTimeChange` | — | Called whenever the selected time settles. |
| `visibleItemCount` | `3` | Items visible in each wheel. Must be odd. |
| `itemHeight` | `null` | Per-item height. `null` auto-derives from text styles. |
| `format` | `HOUR_12` | `HOUR_12` or `HOUR_24`. In `HOUR_24`, the AM/PM wheel is hidden. |
| `periodPosition` | `START` | `START` or `END`. Position of the AM/PM wheel (12-hour only). |
| `locale` | system default | Used for AM/PM labels and default number formatting. |
| `hourFormatter` / `minuteFormatter` | `null` | Override for hour/minute label strings. |
| `separator` | `":"` | Text drawn between hour and minute wheels. `null` hides it. |
| `dividerThickness` | `1.dp` | Top/bottom selection indicator thickness. `null` hides the indicator. |
| `cylindrical` | `true` | Apply 3D cylinder transform to each wheel. |
| `fadeEdges` | `true` | Fade the top/bottom edges of each wheel. |

## WheelPicker

A single-column wheel that picks one value from a list of strings.

```kotlin
val items = listOf("apple", "banana", "cherry", "date", "elderberry")
var index by remember { mutableIntStateOf(0) }

WheelPicker(
    items = items,
    initialIndex = index,
    onValueChange = { i, _ -> index = i },
    visibleItemCount = 5,
    repeatable = true,
)
```

Item height is auto-derived from the parent height (when bounded) or from the text style. Pass `itemHeight` explicitly to override.

### Key parameters

| Parameter | Default | Description |
|---|---|---|
| `items` | — | Strings shown on the wheel. |
| `initialIndex` | — | 0-based initial selection. |
| `onValueChange` | — | `(index: Int, value: String) -> Unit`, called after scroll settles. |
| `visibleItemCount` | `3` | Items fully visible in the center area. Must be odd. Recommended 3..7. |
| `peekItemCount` | `1` | Items partially visible (faded) beyond the visible edge. Recommended 1..3. |
| `itemHeight` | `null` | Per-item height. `null` auto-resolves from parent height or text style. |
| `repeatable` | `false` | Infinite scroll (wraps around). |
| `textStyle` | `headlineLarge` | Item text style. |
| `textColor` | `onSurface` | Item text color. Overrides `textStyle.color`. |
| `cylindrical` | `true` | Apply 3D cylinder transform. |
| `fadeEdges` | `true` | Fade the top/bottom edges of the wheel. |

## Running the sample

Open in Android Studio and run the `:sample` module on a device/emulator (minSdk 26).

## License

[Apache License 2.0](LICENSE)
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

This is an Android macro recorder app in the **planning phase**. No source code exists yet. The concept is documented in `idea.md` and the full development task list is in `todo.md`.

## App Overview

An Android APK that records touchscreen inputs (even while running in the background), saves them as macros, and replays them with configurable speed, repetitions, and scheduling. Key UX: a floating draggable overlay widget controls recording stop/pause without leaving the current app.

## Planned Tech Stack

- **Language**: Kotlin
- **Min SDK**: API 24 (Android 7.0), **Target SDK**: API 34 (Android 14)
- **Architecture**: Clean Architecture with MVVM (ViewModel + StateFlow/LiveData)
- **Database**: Room (macro metadata) + JSON files (touch event sequences)
- **DI**: (not yet decided)
- **Background work**: WorkManager + AlarmManager for scheduling, Foreground Services for recording/playback
- **UI**: Material Design 3, RecyclerView, ViewPager2, PreferenceScreen

## Planned Architecture

Five core components (see `idea.md` → "Technische Implementierung"):

1. **AccessibilityService** (`MacroRecorderAccessibilityService`) — captures and dispatches touch events; this is the foundation for both recording and playback
2. **Recording Engine** — `RecordingManager` singleton + `RecordingForegroundService` with a persistent notification; stores `TouchEvent` objects (timestamp, x, y, action, pressure, pointerId)
3. **Overlay Widget** — `OverlayWidgetService` (Foreground Service) using `WindowManager` for the floating, draggable stop/pause widget that stays visible over all apps
4. **Playback Engine** — `PlaybackManager` + `GestureDispatcher` + `ExecutionForegroundService`; replays events via `GestureDescription`, applies speed multiplier, handles repeat count, pauses between runs
5. **Scheduler** — `MacroScheduler` using `AlarmManager`/`WorkManager` for time-based and interval-based macro triggering

### Data Models

- `Macro` — id (UUID), name, createdAt, duration, eventCount, thumbnailPath, settings
- `MacroSettings` — repeatCount (-1 = infinite), speed (0.25–10x), pauseBetweenRuns, scheduledTime, intervalMinutes, selectedDays, feature toggles
- `TouchEvent` — timestamp (relative), x, y, action (DOWN/UP/MOVE/CANCEL), pressure, pointerId
- `MacroExecutionState` — sealed class: Idle | Running(currentRun, totalRuns) | Paused | Completed | Error

### Storage

- Room DB (`AppDatabase`) for macro metadata via `MacroDao`
- JSON files for raw touch event sequences (one file per macro)
- `SharedPreferences` for app-wide and per-widget settings

## Required Android Permissions

All must be declared in the manifest and checked at every app start with guided deep-links to system settings:

| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Floating overlay widget |
| `BIND_ACCESSIBILITY_SERVICE` | Capture and simulate touch events |
| `FOREGROUND_SERVICE` | Recording and playback services |
| `POST_NOTIFICATIONS` (API 33+) | Status notifications |
| `READ/WRITE_EXTERNAL_STORAGE` | Macro export/import |

## Build Commands

Once the Android project is initialized (standard Gradle-based Android project):

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run instrumented (on-device) tests
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.example.macrorecorder.SomeTest"

# Lint
./gradlew lint

# Install debug APK on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

## Key Implementation Notes

- **Touch simulation** requires `AccessibilityService.dispatchGesture()` with `GestureDescription` — this only works if the Accessibility Service is active and the app has the `BIND_ACCESSIBILITY_SERVICE` permission granted by the user in system settings (cannot be auto-granted).
- **Overlay widget** requires `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` to be granted; use `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- **Event timing**: use `System.nanoTime()` for precise inter-event intervals during recording.
- **Playback timing**: use `Handler.postDelayed()` scaled by the speed multiplier from `MacroSettings`.
- **Emergency stop**: volume button triple-press should abort any running macro.
- **Battery optimization**: the app should request exemption from Doze mode for reliable scheduled execution.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## App Overview

Android macro recorder that captures touchscreen input via `AccessibilityService`, saves macros to Room DB + JSON files, and replays them with configurable speed, repetitions, and scheduling. A floating overlay widget controls recording without leaving the current app.

- **Min SDK**: 24 (Android 7.0) · **Target SDK**: 34 (Android 14)
- **Package**: `com.macrorecorder.app`

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all JVM unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.macrorecorder.app.service.recording.RecordingManagerTest"

# Run instrumented (on-device) tests
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Install debug APK on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

## Architecture

Clean Architecture with MVVM. Three main layers:

**`domain/`** — pure Kotlin data models, repository interface (no Android deps)
- `Macro`, `MacroSettings`, `TouchEvent`, `TouchAction`, `MacroExecutionState`
- `MacroRepository` interface

**`data/`** — Room DB, JSON file storage, repository implementation
- `AppDatabase` (Room) → `MacroDao` → `MacroEntity`
- `MacroRepositoryImpl`: macro metadata in Room; touch event sequences in JSON files via `TouchEventStorage`
- `MacroMapper`: `Macro ↔ MacroEntity` with Gson-serialised `settingsJson` column

**`presentation/`** — Activities + ViewModels (ViewBinding, StateFlow)
- `MainActivity` + `MainViewModel`: macro list via `StateFlow<List<Macro>>` (stateIn WhileSubscribed 5000), import flow via `SharedFlow<ImportResult>`
- `MacroDetailActivity` + `MacroDetailViewModel`: per-macro settings, scheduling, export

**`service/`** — Background services
- `recording/`: `RecordingForegroundService`, `RecordingManager` (singleton object), `GestureRedispatcher`
- `execution/`: `ExecutionForegroundService` (extends `LifecycleService`), `PlaybackManager` (singleton object), `MacroScheduler`, `ScheduledMacroReceiver`, `BootReceiver`
- `accessibility/`: `MacroRecorderAccessibilityService`
- `overlay/`: `OverlayWidgetService`

**`util/`** — `NotificationHelper`, `PermissionManager`, `MacroExporter`

## Key Implementation Patterns

### RecordingManager
Kotlin `object` singleton. `start()` clears state; `addEvent()` is ignored when not recording or when paused; `stop()` returns a `RecordingResult` and stores it in `lastResult`.

### PlaybackManager
Kotlin `object` singleton. Groups `TouchEvent` list by `pointerId` into strokes, builds `GestureDescription` paths, dispatches via `AccessibilityService.dispatchGesture()` using `suspendCancellableCoroutine`. Supports speed multiplier (divides all delays), `repeatCount` (-1 = infinite), `pauseBetweenRunsMs`. Exposes `val state: StateFlow<MacroExecutionState>`.

### MacroScheduler
Uses `AlarmManager.setExactAndAllowWhileIdle` (falls back to `setAndAllowWhileIdle` if `canScheduleExactAlarms()` returns false on API 31+). One-time vs. interval alarms use distinct request codes via bit masking:
- once: `hashCode and 0x0FFFFFFF` (bit 28 always 0)
- interval: `(hashCode and 0x0FFFFFFF) or 0x10000000` (bit 28 always 1)

### ScheduledMacroReceiver
Stores `intervalMinutes` and `selectedDays` as PendingIntent extras to avoid synchronous DB access in `onReceive`. Uses `goAsync()` + coroutine to reschedule interval alarms. `isTodayActive(selectedDays)`: empty list = every day.

### BootReceiver
`goAsync()` + `CoroutineScope(Dispatchers.IO)` coroutine; calls `repo.getAllMacrosOnce()`, reschedules macros with upcoming `scheduledTimeMs` or active `intervalMinutes`.

### MacroExporter
MIME type `application/x-macro-recorder`. JSON bundle: `{version: 1, macro: {...}, events: [...]}`. `fromJson()` assigns fresh UUID + current `createdAt`; returns null for blank / malformed / wrong version / empty events. `internal fun toJson()` / `fromJson()` are pure JVM (no Android Context) — this is what unit tests call directly.

### MacroDetailActivity
`MaterialDatePicker → MaterialTimePicker` chain for scheduling. `chipDayMap: LinkedHashMap<Int, Int>` maps chip view IDs to `Calendar` day constants. `saveAndFinish()` calls `MacroScheduler.schedule()` or `MacroScheduler.cancel()` based on whether a time is set.

## Tech Stack

| Concern | Library |
|---|---|
| Database | Room (KSP) |
| Serialisation | Gson (settings column + event files + export bundles) |
| Async | Kotlin Coroutines + StateFlow/SharedFlow |
| Lifecycle services | `lifecycle-service` (LifecycleService) |
| UI | Material Design 3, ViewBinding, RecyclerView ListAdapter |
| Scheduling | AlarmManager (exact alarms) |
| Tests | JUnit 4, MockK, kotlinx-coroutines-test |

## Unit Test Classes

All in `app/src/test/java/com/macrorecorder/app/`:

| Class | Cases | What it tests |
|---|---|---|
| `RecordingManagerTest` | 17 | State machine: start/stop/pause/resume/addEvent |
| `MacroMapperTest` | 16 | Gson roundtrip for all `MacroSettings` fields |
| `MacroExporterTest` | 21 | `toJson`/`fromJson`, fresh UUID on import, null on invalid input |
| `MacroSchedulerTest` | 10 | Request code distinctness, determinism, non-negative, bit-28 invariant |
| `ScheduledMacroReceiverTest` | 10 | Day-of-week filter (`isTodayActive`) |

## Required Permissions

| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Floating overlay widget |
| `BIND_ACCESSIBILITY_SERVICE` | Capture and simulate touch events |
| `FOREGROUND_SERVICE` | Recording and playback services |
| `POST_NOTIFICATIONS` (API 33+) | Status notifications |
| `SCHEDULE_EXACT_ALARM` | Precise scheduled macro execution |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule alarms after reboot |

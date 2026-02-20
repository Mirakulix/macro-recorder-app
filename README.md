# Macro Recorder

An Android app that records touchscreen inputs and replays them automatically — even while running in the background.

## Features

- **Background recording** — capture touch events from any app using Android's Accessibility Service
- **Floating widget** — a small, draggable overlay button lets you stop/pause recording without leaving your current app
- **Flexible playback** — replay macros at custom speed (0.25x–10x), any number of times (including infinite loop), with configurable pauses between runs
- **Scheduling** — trigger macros at a specific time, on a recurring interval, or on selected weekdays
- **Per-macro settings** — every macro stores its own playback configuration persistently
- **Emergency stop** — triple-press volume buttons to abort any running macro
- **Export/Import** — share macros as files between devices

## Requirements

- Android 7.0+ (API 24)
- The following permissions must be granted manually in system settings:
  - **Accessibility Service** — required for capturing and simulating touch events
  - **Display over other apps** — required for the floating overlay widget
  - **Notifications** — for status updates during recording/playback (Android 13+)

The app checks for these permissions on every launch and guides you step-by-step to the correct system settings screen.

## Architecture

See [CLAUDE.md](CLAUDE.md) for a detailed technical breakdown of the planned architecture, data models, and component responsibilities.

## Project Status

Currently in active development. The full task list is tracked in [todo.md](todo.md).

## License

[CC BY-NC 4.0](LICENSE) — free to use and adapt for non-commercial purposes with attribution.

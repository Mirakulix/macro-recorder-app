package com.macrorecorder.app.domain.model

/**
 * Per-macro playback configuration. All fields are persisted alongside the macro.
 *
 * @param repeatCount         How many times to run the macro. -1 = infinite loop.
 * @param speed               Playback speed multiplier. Valid range: 0.25 – 10.0.
 * @param pauseBetweenRunsMs  Delay between consecutive runs in milliseconds.
 * @param scheduledTimeMs     Epoch ms for a one-time scheduled start; null = manual start only.
 * @param intervalMinutes     Repeat every N minutes; null = no interval.
 * @param selectedDays        Days of the week the interval is active (java.util.Calendar constants:
 *                            1 = Sunday … 7 = Saturday). Empty list = every day.
 * @param emergencyStopEnabled  Triple-press volume buttons aborts playback.
 * @param vibrationEnabled    Haptic feedback at start and end of each run.
 * @param visualIndicatorEnabled  Show a translucent dot at each touch position during playback.
 */
data class MacroSettings(
    val repeatCount: Int = 1,
    val speed: Float = 1.0f,
    val pauseBetweenRunsMs: Long = 0L,
    val scheduledTimeMs: Long? = null,
    val intervalMinutes: Int? = null,
    val selectedDays: List<Int> = emptyList(),
    val emergencyStopEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val visualIndicatorEnabled: Boolean = false
)

package com.macrorecorder.app.service.recording

import com.macrorecorder.app.domain.model.TouchAction
import com.macrorecorder.app.domain.model.TouchEvent

/**
 * In-memory store for the touch events captured during a recording session.
 *
 * Lifecycle: [start] → [addEvent]* → ([pause] / [resume])* → [stop]
 * The result of the last session is held in [lastResult] until overwritten.
 *
 * Thread-safety: [isRecording]/[isPaused] are @Volatile; the event list is
 * protected by `synchronized(events)` for safe concurrent writes from the
 * touch capture layer.
 */
object RecordingManager {

    data class RecordingResult(
        val events: List<TouchEvent>,
        val durationMs: Long
    )

    @Volatile var isRecording: Boolean = false
        private set

    @Volatile var isPaused: Boolean = false
        private set

    /** Holds the result of the most-recent recording session until it is saved. */
    @Volatile var lastResult: RecordingResult? = null
        private set

    val eventCount: Int get() = synchronized(events) { events.size }

    val durationMs: Long
        get() = if (startTimeNs == 0L) 0L
                else (System.nanoTime() - startTimeNs) / 1_000_000

    private val events = mutableListOf<TouchEvent>()
    private var startTimeNs = 0L

    fun start() {
        synchronized(events) { events.clear() }
        startTimeNs = System.nanoTime()
        lastResult   = null
        isRecording  = true
        isPaused     = false
    }

    fun addEvent(
        x: Float,
        y: Float,
        action: TouchAction,
        pressure: Float = 1.0f,
        pointerId: Int = 0
    ) {
        if (!isRecording || isPaused) return
        val timestamp = (System.nanoTime() - startTimeNs) / 1_000_000
        synchronized(events) {
            events.add(TouchEvent(timestamp, x, y, action, pressure, pointerId))
        }
    }

    fun pause()  { isPaused = true  }
    fun resume() { isPaused = false }

    /**
     * Stops the current session, stores the result in [lastResult], and returns it.
     * Safe to call even if no recording is in progress.
     */
    fun stop(): RecordingResult {
        val finalDurationMs = durationMs
        isRecording = false
        isPaused    = false
        return RecordingResult(
            events    = synchronized(events) { events.toList() },
            durationMs = finalDurationMs
        ).also { lastResult = it }
    }
}

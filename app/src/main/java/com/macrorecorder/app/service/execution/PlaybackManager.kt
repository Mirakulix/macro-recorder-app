package com.macrorecorder.app.service.execution

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.MacroExecutionState
import com.macrorecorder.app.domain.model.TouchAction
import com.macrorecorder.app.domain.model.TouchEvent
import com.macrorecorder.app.service.accessibility.MacroRecorderAccessibilityService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Replays recorded [TouchEvent] sequences via [MacroRecorderAccessibilityService.dispatchGesture].
 *
 * Stroke grouping: events are grouped per [TouchEvent.pointerId] into complete
 * DOWN → MOVE* → UP sequences, then sorted by start time so multi-touch works correctly.
 *
 * Timing: each stroke is scheduled relative to the run start, scaled by [Macro.settings.speed].
 * A speed of 2.0 halves all delays (twice as fast); 0.5 doubles them (half speed).
 */
object PlaybackManager {

    private data class PlaybackStroke(
        val path: Path,
        val startMs: Long,   // relative to recording start
        val durationMs: Long // actual touch duration
    )

    private val _state = MutableStateFlow<MacroExecutionState>(MacroExecutionState.Idle)
    val state: StateFlow<MacroExecutionState> = _state.asStateFlow()

    @Volatile private var isPaused = false

    // Snapshotted for resume() to re-emit correct Running state
    @Volatile private var snapshotRun = 1
    @Volatile private var snapshotTotal = 1

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun execute(macro: Macro, events: List<TouchEvent>) {
        val service = MacroRecorderAccessibilityService.instance ?: run {
            _state.value = MacroExecutionState.Error("Accessibility service is not active")
            return
        }

        job?.cancel()
        isPaused = false

        val totalRuns = macro.settings.repeatCount  // -1 = infinite

        job = scope.launch {
            try {
                val strokes = buildStrokes(events)
                var run = 1

                while (totalRuns == -1 || run <= totalRuns) {
                    snapshotRun = run
                    snapshotTotal = totalRuns
                    _state.value = MacroExecutionState.Running(run, totalRuns)

                    playRun(strokes, macro.settings.speed, service)

                    if (totalRuns != -1 && run >= totalRuns) break

                    if (macro.settings.pauseBetweenRunsMs > 0) {
                        delay(macro.settings.pauseBetweenRunsMs)
                    }
                    run++
                }
                _state.value = MacroExecutionState.Completed

            } catch (e: CancellationException) {
                _state.value = MacroExecutionState.Idle
                throw e
            } catch (e: Exception) {
                _state.value = MacroExecutionState.Error(e.message ?: "Playback failed")
            }
        }
    }

    fun pause() {
        isPaused = true
        _state.value = MacroExecutionState.Paused
    }

    fun resume() {
        isPaused = false
        _state.value = MacroExecutionState.Running(snapshotRun, snapshotTotal)
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value = MacroExecutionState.Idle
    }

    // ── Stroke building ───────────────────────────────────────────────────────

    private fun buildStrokes(events: List<TouchEvent>): List<PlaybackStroke> {
        data class Accum(val path: Path, val startMs: Long, var endMs: Long)

        val active = mutableMapOf<Int, Accum>()
        val strokes = mutableListOf<PlaybackStroke>()

        for (event in events) {
            when (event.action) {
                TouchAction.DOWN -> {
                    active[event.pointerId] = Accum(
                        path    = Path().apply { moveTo(event.x, event.y) },
                        startMs = event.timestampMs,
                        endMs   = event.timestampMs
                    )
                }
                TouchAction.MOVE -> {
                    active[event.pointerId]?.also {
                        it.path.lineTo(event.x, event.y)
                        it.endMs = event.timestampMs
                    }
                }
                TouchAction.UP, TouchAction.CANCEL -> {
                    active.remove(event.pointerId)?.also { a ->
                        a.path.lineTo(event.x, event.y)
                        strokes += PlaybackStroke(
                            path      = a.path,
                            startMs   = a.startMs,
                            durationMs = maxOf(event.timestampMs - a.startMs, 1L)
                        )
                    }
                }
            }
        }
        return strokes.sortedBy { it.startMs }
    }

    // ── Run execution ─────────────────────────────────────────────────────────

    private suspend fun playRun(
        strokes: List<PlaybackStroke>,
        speed: Float,
        service: MacroRecorderAccessibilityService
    ) {
        if (strokes.isEmpty()) return
        val runStart = System.currentTimeMillis()

        for (stroke in strokes) {
            awaitResumed()

            val scheduledAt = runStart + (stroke.startMs / speed).toLong()
            val waitMs = scheduledAt - System.currentTimeMillis()
            if (waitMs > 0) delay(waitMs)

            awaitResumed()  // check again after the delay (user might have paused)

            val adjustedDuration = maxOf((stroke.durationMs / speed).toLong(), 16L)
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(stroke.path, 0L, adjustedDuration))
                .build()

            dispatchAndAwait(service, gesture)
        }
    }

    /** Suspends until [isPaused] becomes false. */
    private suspend fun awaitResumed() {
        while (isPaused) delay(50)
    }

    /**
     * Dispatches a [GestureDescription] via the accessibility service and suspends
     * until the gesture completes or is cancelled.
     */
    private suspend fun dispatchAndAwait(
        service: MacroRecorderAccessibilityService,
        gesture: GestureDescription
    ): Boolean = suspendCancellableCoroutine { cont ->
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (cont.isActive) cont.resume(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (cont.isActive) cont.resume(false)
            }
        }
        val dispatched = service.dispatchGesture(gesture, callback, null)
        if (!dispatched && cont.isActive) cont.resume(false)
    }
}

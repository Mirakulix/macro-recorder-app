package com.macrorecorder.app.domain.model

/**
 * Represents the current state of the macro playback engine.
 * Emitted as a [kotlinx.coroutines.flow.StateFlow] from the ViewModel.
 */
sealed class MacroExecutionState {

    /** No macro is running. */
    object Idle : MacroExecutionState()

    /**
     * A macro is actively replaying touch events.
     *
     * @param currentRun 1-based index of the current run.
     * @param totalRuns  Total planned runs, or -1 for an infinite loop.
     */
    data class Running(val currentRun: Int, val totalRuns: Int) : MacroExecutionState()

    /** Playback is temporarily suspended (user pressed pause). */
    object Paused : MacroExecutionState()

    /** All planned runs finished successfully. */
    object Completed : MacroExecutionState()

    /** Playback stopped due to an error. */
    data class Error(val message: String) : MacroExecutionState()
}

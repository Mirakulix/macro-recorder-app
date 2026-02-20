package com.macrorecorder.app.domain.repository

import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.TouchEvent
import kotlinx.coroutines.flow.Flow

interface MacroRepository {

    /** Live stream of all macros, ordered by creation date descending. */
    fun getAllMacros(): Flow<List<Macro>>

    /** Live stream of macros whose name contains [query] (case-insensitive). */
    fun searchMacros(query: String): Flow<List<Macro>>

    suspend fun getMacroById(id: String): Macro?

    /**
     * Persists a new macro.
     * Touch events are written to a JSON file; metadata goes into the Room database.
     * Both operations must succeed — callers should handle exceptions and clean up if needed.
     */
    suspend fun saveMacro(macro: Macro, events: List<TouchEvent>)

    /** Updates name, settings, or thumbnail of an existing macro. Does not touch the event file. */
    suspend fun updateMacro(macro: Macro)

    /** Deletes the database row and the corresponding touch-event JSON file. */
    suspend fun deleteMacro(id: String)

    /** Loads the recorded touch-event sequence for playback. Returns empty list if not found. */
    suspend fun loadEvents(macroId: String): List<TouchEvent>

    /** One-shot read of all macros. Used by [BootReceiver] to re-register alarms after reboot. */
    suspend fun getAllMacrosOnce(): List<Macro>
}

package com.macrorecorder.app.domain.model

/**
 * Core domain model representing a saved macro.
 *
 * The actual touch-event sequence is stored separately (as a JSON file) and loaded
 * on demand via [com.macrorecorder.app.domain.repository.MacroRepository.loadEvents].
 *
 * @param id            UUID string; unique identifier.
 * @param name          User-visible name.
 * @param createdAt     Unix epoch timestamp (ms) of creation.
 * @param duration      Total duration of the original recording in milliseconds.
 * @param eventCount    Number of touch events captured.
 * @param thumbnailPath Absolute path to a screenshot thumbnail, or null.
 * @param settings      Per-macro playback configuration.
 */
data class Macro(
    val id: String,
    val name: String,
    val createdAt: Long,
    val duration: Long,
    val eventCount: Int,
    val thumbnailPath: String? = null,
    val settings: MacroSettings = MacroSettings()
)

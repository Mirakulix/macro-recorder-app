package com.macrorecorder.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for macro metadata.
 * The touch-event sequence is stored separately as a JSON file (see TouchEventStorage).
 */
@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    /** Duration of the original recording in milliseconds. */
    val duration: Long,
    val eventCount: Int,
    val thumbnailPath: String?,
    /** MacroSettings serialised to JSON by MacroMapper. */
    val settingsJson: String
)

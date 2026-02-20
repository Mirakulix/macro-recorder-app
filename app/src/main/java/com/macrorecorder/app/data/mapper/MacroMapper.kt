package com.macrorecorder.app.data.mapper

import com.google.gson.Gson
import com.macrorecorder.app.data.local.db.entity.MacroEntity
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.MacroSettings

fun MacroEntity.toDomain(gson: Gson): Macro = Macro(
    id = id,
    name = name,
    createdAt = createdAt,
    duration = duration,
    eventCount = eventCount,
    thumbnailPath = thumbnailPath,
    settings = runCatching {
        gson.fromJson(settingsJson, MacroSettings::class.java)
    }.getOrDefault(MacroSettings())
)

fun Macro.toEntity(gson: Gson): MacroEntity = MacroEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    duration = duration,
    eventCount = eventCount,
    thumbnailPath = thumbnailPath,
    settingsJson = gson.toJson(settings)
)

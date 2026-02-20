package com.macrorecorder.app.data.local.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.macrorecorder.app.domain.model.TouchEvent
import java.io.File

/**
 * Reads and writes the touch-event sequences that back each macro.
 *
 * Storage location: [Context.filesDir]/macros/{macroId}.json
 * This directory is private to the app — no storage permission is needed.
 */
class TouchEventStorage(private val context: Context) {

    private val gson = Gson()

    private val macrosDir: File
        get() = File(context.filesDir, "macros").also { it.mkdirs() }

    fun save(macroId: String, events: List<TouchEvent>) {
        eventsFile(macroId).writeText(gson.toJson(events))
    }

    fun load(macroId: String): List<TouchEvent> {
        val file = eventsFile(macroId)
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<TouchEvent>>() {}.type
        return runCatching {
            gson.fromJson<List<TouchEvent>>(file.readText(), type)
        }.getOrDefault(emptyList())
    }

    fun delete(macroId: String) {
        eventsFile(macroId).delete()
    }

    private fun eventsFile(macroId: String) = File(macrosDir, "$macroId.json")
}

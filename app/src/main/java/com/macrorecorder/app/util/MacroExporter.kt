package com.macrorecorder.app.util

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.TouchEvent
import java.util.UUID

/**
 * Serialises and deserialises macro bundles (metadata + touch events) to/from a
 * single JSON file.
 *
 * ### File format
 * ```json
 * {
 *   "version": 1,
 *   "macro":  { …Macro fields… },
 *   "events": [ …TouchEvent array… ]
 * }
 * ```
 *
 * The MIME type [MIME_TYPE] is used both for `CreateDocument` and for the
 * `<intent-filter>` that allows the app to open `.macro` files from file managers.
 */
object MacroExporter {

    const val MIME_TYPE = "application/x-macro-recorder"

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private data class Bundle(
        val version: Int,
        val macro: Macro,
        val events: List<TouchEvent>
    )

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Serialises [macro] + [events] to JSON and writes the result to [uri].
     * Throws [IllegalStateException] if the output stream cannot be opened.
     */
    fun export(context: Context, macro: Macro, events: List<TouchEvent>, uri: Uri) {
        val bundle = Bundle(version = 1, macro = macro, events = events)
        val json = gson.toJson(bundle).toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.use { it.write(json) }
            ?: throw IllegalStateException("Cannot open output stream for $uri")
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Reads a macro bundle from [uri].
     *
     * @return A pair of the imported [Macro] (with a **fresh UUID** and the
     *   current timestamp) and its [TouchEvent] list, or `null` if the file is
     *   missing, unreadable, or not a valid macro bundle.
     */
    fun import(context: Context, uri: Uri): Pair<Macro, List<TouchEvent>>? {
        val json = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes().decodeToString() }
            ?: return null

        val type = object : TypeToken<Bundle>() {}.type
        val bundle: Bundle = runCatching {
            gson.fromJson<Bundle>(json, type)
        }.getOrNull() ?: return null

        if (bundle.version != 1 || bundle.events.isEmpty()) return null

        val imported = bundle.macro.copy(
            id        = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis()
        )
        return imported to bundle.events
    }

    // ── Filename helper ───────────────────────────────────────────────────────

    /** Returns a safe filename for [macroName] with the `.macro` extension. */
    fun suggestedFilename(macroName: String): String {
        val safe = macroName.trim().replace("[^a-zA-Z0-9_\\-. ]".toRegex(), "_")
        return "$safe.macro"
    }
}

package com.macrorecorder.app.data.local.storage

import android.content.Context
import android.graphics.Bitmap
import java.io.File

/**
 * Reads and writes the PNG thumbnail files that represent each macro visually.
 *
 * Storage location: [Context.filesDir]/thumbnails/{macroId}.png
 * Private to the app — no storage permission needed.
 */
class ThumbnailStorage(private val context: Context) {

    private val dir get() = File(context.filesDir, "thumbnails").also { it.mkdirs() }

    /** Compresses [bitmap] as PNG and writes it to disk. Returns the absolute file path. */
    fun save(macroId: String, bitmap: Bitmap): String {
        val file = File(dir, "$macroId.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
        return file.absolutePath
    }

    fun delete(macroId: String) {
        File(dir, "$macroId.png").delete()
    }
}

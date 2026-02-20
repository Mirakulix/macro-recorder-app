package com.macrorecorder.app

import android.app.Application
import com.google.gson.Gson
import com.macrorecorder.app.data.local.db.AppDatabase
import com.macrorecorder.app.data.local.storage.ThumbnailStorage
import com.macrorecorder.app.data.local.storage.TouchEventStorage
import com.macrorecorder.app.data.repository.MacroRepositoryImpl
import com.macrorecorder.app.domain.repository.MacroRepository

/**
 * Application singleton that holds shared, lazily-initialised infrastructure.
 * Activities, Services, and BroadcastReceivers access the repository via:
 *   (applicationContext as MacroRecorderApp).repository
 */
class MacroRecorderApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: MacroRepository by lazy {
        MacroRepositoryImpl(
            macroDao = database.macroDao(),
            touchEventStorage = TouchEventStorage(this),
            thumbnailStorage = ThumbnailStorage(this),
            gson = Gson()
        )
    }
}

package com.macrorecorder.app.service.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service that manages the floating overlay widget.
 *
 * The widget is added to [android.view.WindowManager] with
 * TYPE_APPLICATION_OVERLAY so it stays visible over all other apps.
 *
 * Responsibilities:
 *  - Display a draggable stop/pause button during recording.
 *  - Intercept touch events on the transparent capture layer and forward
 *    them to RecordingForegroundService as TouchEvent objects.
 *  - Persist the last widget position in SharedPreferences.
 */
class OverlayWidgetService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "channel_recording"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: inflate overlay_widget.xml, add to WindowManager, set up drag + touch capture
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: remove overlay view from WindowManager
    }
}

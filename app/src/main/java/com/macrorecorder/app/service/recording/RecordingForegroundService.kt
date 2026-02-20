package com.macrorecorder.app.service.recording

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service that keeps the app alive during recording.
 * Shows a persistent notification with a Stop action.
 *
 * Lifecycle:
 *  start → show notification → coordinate with OverlayWidgetService
 *  stop  → save collected TouchEvents → broadcast result to MainActivity
 */
class RecordingForegroundService : Service() {

    companion object {
        const val ACTION_STOP_RECORDING = "com.macrorecorder.app.STOP_RECORDING"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "channel_recording"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_RECORDING) {
            stopSelf()
            return START_NOT_STICKY
        }
        // TODO: create notification channel, call startForeground(), start event collection
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: flush recorded events to storage, notify UI
    }
}

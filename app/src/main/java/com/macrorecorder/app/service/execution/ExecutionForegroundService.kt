package com.macrorecorder.app.service.execution

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service that runs during macro playback.
 * Shows a notification with a Stop action and a progress indicator.
 *
 * Flow:
 *  receive macroId via Intent extra → load TouchEvents from storage →
 *  hand off to PlaybackManager → update notification on each repeat →
 *  stop when done or when Stop action is tapped.
 */
class ExecutionForegroundService : Service() {

    companion object {
        const val EXTRA_MACRO_ID = "macro_id"
        const val ACTION_STOP_PLAYBACK = "com.macrorecorder.app.STOP_PLAYBACK"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "channel_playback"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_PLAYBACK) {
            stopSelf()
            return START_NOT_STICKY
        }
        val macroId = intent?.getStringExtra(EXTRA_MACRO_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        // TODO: startForeground(), load macro, start PlaybackManager
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: cancel PlaybackManager coroutine
    }
}

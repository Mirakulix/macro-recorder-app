package com.macrorecorder.app.service.execution

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.macrorecorder.app.MacroRecorderApp
import com.macrorecorder.app.domain.model.MacroExecutionState
import com.macrorecorder.app.util.NotificationHelper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Foreground service that drives macro playback.
 *
 * Flow:
 *  Start intent with [EXTRA_MACRO_ID] → load [Macro] + events from repository
 *  → call [PlaybackManager.execute] → observe [PlaybackManager.state] to update
 *  the notification and stop the service when playback finishes.
 *
 * Send [ACTION_STOP_PLAYBACK] / [ACTION_PAUSE_PLAYBACK] / [ACTION_RESUME_PLAYBACK]
 * intents to control playback from the notification or other components.
 */
class ExecutionForegroundService : LifecycleService() {

    companion object {
        const val EXTRA_MACRO_ID       = "macro_id"
        const val ACTION_STOP_PLAYBACK   = "com.macrorecorder.app.STOP_PLAYBACK"
        const val ACTION_PAUSE_PLAYBACK  = "com.macrorecorder.app.PAUSE_PLAYBACK"
        const val ACTION_RESUME_PLAYBACK = "com.macrorecorder.app.RESUME_PLAYBACK"
        const val NOTIFICATION_ID = 2001

        fun startIntent(context: Context, macroId: String): Intent =
            Intent(context, ExecutionForegroundService::class.java)
                .putExtra(EXTRA_MACRO_ID, macroId)

        fun stopIntent(context: Context): Intent =
            Intent(context, ExecutionForegroundService::class.java)
                .apply { action = ACTION_STOP_PLAYBACK }
    }

    private var macroName = ""

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP_PLAYBACK   -> { PlaybackManager.stop(); stopSelf() }
            ACTION_PAUSE_PLAYBACK  -> PlaybackManager.pause()
            ACTION_RESUME_PLAYBACK -> PlaybackManager.resume()
            else -> {
                val macroId = intent?.getStringExtra(EXTRA_MACRO_ID) ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startPlayback(macroId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        PlaybackManager.stop()
    }

    private fun startPlayback(macroId: String) {
        NotificationHelper.createChannels(this)
        startForeground(NOTIFICATION_ID, buildNotification("", 1, 1))

        val repo = (applicationContext as MacroRecorderApp).repository

        lifecycleScope.launch {
            val macro = repo.getMacroById(macroId) ?: run { stopSelf(); return@launch }
            val events = repo.loadEvents(macroId)
            macroName = macro.name

            // Observe state → update notification / auto-stop
            PlaybackManager.state.onEach { state ->
                when (state) {
                    is MacroExecutionState.Running -> updateNotification(state.currentRun, state.totalRuns)
                    is MacroExecutionState.Completed -> stopSelf()
                    is MacroExecutionState.Error    -> stopSelf()
                    else -> Unit
                }
            }.launchIn(lifecycleScope)

            PlaybackManager.execute(macro, events)
        }
    }

    private fun buildNotification(name: String, currentRun: Int, totalRuns: Int) =
        NotificationHelper.buildPlaybackNotification(
            context          = this,
            macroName        = name,
            currentRun       = currentRun,
            totalRuns        = totalRuns,
            stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

    private fun updateNotification(currentRun: Int, totalRuns: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(macroName, currentRun, totalRuns))
    }
}

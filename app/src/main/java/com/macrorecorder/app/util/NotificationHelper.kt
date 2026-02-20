package com.macrorecorder.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.macrorecorder.app.R
import com.macrorecorder.app.presentation.main.MainActivity
import com.macrorecorder.app.service.recording.RecordingForegroundService

object NotificationHelper {

    const val CHANNEL_RECORDING_ID = "channel_recording"
    const val CHANNEL_PLAYBACK_ID  = "channel_playback"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RECORDING_ID,
                    context.getString(R.string.notification_channel_recording_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notification_channel_recording_desc)
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PLAYBACK_ID,
                    context.getString(R.string.notification_channel_playback_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.notification_channel_playback_desc)
                    setShowBadge(false)
                }
            )
        }
    }

    fun buildRecordingNotification(context: Context): Notification {
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            RecordingForegroundService.stopIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_RECORDING_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_recording_title))
            .setContentText(context.getString(R.string.notification_recording_text))
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun buildPlaybackNotification(
        context: Context,
        macroName: String,
        currentRun: Int,
        totalRuns: Int
    ): Notification {
        val progress = if (totalRuns == -1) "run $currentRun ∞" else "$currentRun / $totalRuns"
        return NotificationCompat.Builder(context, CHANNEL_PLAYBACK_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_playback_title))
            .setContentText("$macroName — $progress")
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}

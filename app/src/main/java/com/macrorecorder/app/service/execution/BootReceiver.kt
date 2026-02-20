package com.macrorecorder.app.service.execution

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.macrorecorder.app.MacroRecorderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-registers scheduled macro alarms after a device reboot.
 *
 * AlarmManager alarms are cleared by the OS on reboot. This receiver fires on
 * [Intent.ACTION_BOOT_COMPLETED] and re-schedules every macro that has either
 * a [MacroSettings.scheduledTimeMs] in the future or an active [MacroSettings.intervalMinutes].
 *
 * [goAsync] is used to safely offload the database read onto a background coroutine
 * without triggering an ANR (receivers have a 10-second budget on the main thread).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val now = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = (context.applicationContext as MacroRecorderApp).repository
                val macros = repo.getAllMacrosOnce()

                macros.forEach { macro ->
                    val s = macro.settings
                    val hasUpcomingOnce     = s.scheduledTimeMs?.let { it > now } ?: false
                    val hasActiveInterval   = (s.intervalMinutes ?: 0) > 0

                    if (hasUpcomingOnce || hasActiveInterval) {
                        MacroScheduler.schedule(context, macro)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

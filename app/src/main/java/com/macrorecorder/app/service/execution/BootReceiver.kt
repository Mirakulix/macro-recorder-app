package com.macrorecorder.app.service.execution

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restores scheduled macros after the device reboots.
 * AlarmManager alarms are cleared on reboot, so we re-register them here.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO: load all macros with scheduledTime != null from the repository
            //       and re-register their alarms via MacroScheduler
        }
    }
}

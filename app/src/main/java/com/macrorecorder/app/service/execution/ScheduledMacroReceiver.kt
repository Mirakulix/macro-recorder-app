package com.macrorecorder.app.service.execution

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.macrorecorder.app.MacroRecorderApp
import com.macrorecorder.app.domain.model.MacroSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Receives AlarmManager broadcasts and starts [ExecutionForegroundService] for the
 * scheduled macro.
 *
 * For **interval** macros the receiver also reschedules the next alarm via
 * [MacroScheduler.rescheduleInterval]. Day-of-week filtering ([MacroSettings.selectedDays])
 * is applied before execution: if today is not an active day the alarm is silently
 * rescheduled without running the macro.
 */
class ScheduledMacroReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RUN             = "com.macrorecorder.app.RUN_SCHEDULED_MACRO"
        const val EXTRA_MACRO_ID         = "macro_id"
        const val EXTRA_INTERVAL_MINUTES = "interval_minutes"
        const val EXTRA_SELECTED_DAYS    = "selected_days"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RUN) return

        val macroId         = intent.getStringExtra(EXTRA_MACRO_ID) ?: return
        val intervalMinutes = intent.getIntExtra(EXTRA_INTERVAL_MINUTES, -1)
        val selectedDays    = intent.getIntArrayExtra(EXTRA_SELECTED_DAYS)?.toList()
            ?: emptyList()

        val isInterval = intervalMinutes > 0
        val todayActive = isTodayActive(selectedDays)

        if (todayActive) {
            // Start playback
            ContextCompat.startForegroundService(
                context,
                ExecutionForegroundService.startIntent(context, macroId)
            )
        }

        // Reschedule next interval alarm even if today was skipped
        if (isInterval) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = (context.applicationContext as MacroRecorderApp).repository
                    val macro = repo.getMacroById(macroId)
                    if (macro != null) {
                        MacroScheduler.rescheduleInterval(context, macro)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }

    /**
     * Returns true if [selectedDays] is empty (= every day) or contains today's
     * [Calendar.DAY_OF_WEEK] value (Sunday=1 … Saturday=7). Internal for testing.
     */
    internal fun isTodayActive(selectedDays: List<Int>): Boolean {
        if (selectedDays.isEmpty()) return true
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return today in selectedDays
    }
}

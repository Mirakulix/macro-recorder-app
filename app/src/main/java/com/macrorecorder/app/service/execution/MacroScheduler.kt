package com.macrorecorder.app.service.execution

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.macrorecorder.app.domain.model.Macro

/**
 * Schedules and cancels AlarmManager alarms that trigger macro playback.
 *
 * Two scheduling modes (both optional, per [Macro.settings]):
 *  - **One-time**: fires once at [MacroSettings.scheduledTimeMs] (skipped if already past).
 *  - **Interval**: fires every [MacroSettings.intervalMinutes] minutes, filtered by
 *    [MacroSettings.selectedDays]. The receiver reschedules the next interval alarm.
 *
 * Each macro gets two distinct request codes (derived from its ID hash) so one-time and
 * interval alarms can coexist and be cancelled independently.
 *
 * On API 31+ the exact alarm requires the user to grant [SCHEDULE_EXACT_ALARM]; if not
 * granted we fall back to [AlarmManager.setAndAllowWhileIdle] (still reliable, ±1 min).
 */
object MacroScheduler {

    // ── Public API ────────────────────────────────────────────────────────────

    /** Schedules whatever alarms [macro.settings] demands, replacing any existing ones. */
    fun schedule(context: Context, macro: Macro) {
        val am = context.getSystemService(AlarmManager::class.java)
        val settings = macro.settings
        val now = System.currentTimeMillis()

        // One-time alarm
        settings.scheduledTimeMs?.takeIf { it > now }?.let { at ->
            setExact(context, am, at, onceIntent(context, macro))
        }

        // Interval alarm — first trigger is one interval from now
        settings.intervalMinutes?.takeIf { it > 0 }?.let { mins ->
            val firstTrigger = now + mins * 60_000L
            setExact(context, am, firstTrigger, intervalIntent(context, macro))
        }
    }

    /** Cancels all alarms for this macro (both one-time and interval). */
    fun cancel(context: Context, macroId: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(oncePendingIntent(context, macroId))
        am.cancel(intervalPendingIntent(context, macroId))
    }

    /**
     * Reschedules the next interval alarm for a macro.
     * Called from [ScheduledMacroReceiver] after each interval fires.
     */
    fun rescheduleInterval(context: Context, macro: Macro) {
        val mins = macro.settings.intervalMinutes?.takeIf { it > 0 } ?: return
        val am = context.getSystemService(AlarmManager::class.java)
        val nextTrigger = System.currentTimeMillis() + mins * 60_000L
        setExact(context, am, nextTrigger, intervalIntent(context, macro))
    }

    // ── Intent builders ───────────────────────────────────────────────────────

    private fun onceIntent(context: Context, macro: Macro): PendingIntent =
        oncePendingIntent(context, macro.id)

    private fun oncePendingIntent(context: Context, macroId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCodeOnce(macroId),
            baseIntent(context, macroId, intervalMinutes = -1, selectedDays = intArrayOf()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun intervalIntent(context: Context, macro: Macro): PendingIntent =
        intervalPendingIntent(context, macro.id, macro.settings)

    private fun intervalPendingIntent(context: Context, macroId: String): PendingIntent =
        intervalPendingIntent(context, macroId, settings = null)

    private fun intervalPendingIntent(
        context: Context,
        macroId: String,
        settings: com.macrorecorder.app.domain.model.MacroSettings?
    ): PendingIntent {
        val intent = baseIntent(
            context,
            macroId,
            intervalMinutes = settings?.intervalMinutes ?: -1,
            selectedDays    = settings?.selectedDays?.toIntArray() ?: intArrayOf()
        )
        return PendingIntent.getBroadcast(
            context,
            requestCodeInterval(macroId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun baseIntent(
        context: Context,
        macroId: String,
        intervalMinutes: Int,
        selectedDays: IntArray
    ): Intent = Intent(context, ScheduledMacroReceiver::class.java).apply {
        action = ScheduledMacroReceiver.ACTION_RUN
        putExtra(ScheduledMacroReceiver.EXTRA_MACRO_ID, macroId)
        putExtra(ScheduledMacroReceiver.EXTRA_INTERVAL_MINUTES, intervalMinutes)
        putExtra(ScheduledMacroReceiver.EXTRA_SELECTED_DAYS, selectedDays)
    }

    // ── AlarmManager helpers ──────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun setExact(
        context: Context,
        am: AlarmManager,
        triggerAtMs: Long,
        pi: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                // Fallback: inexact but still fires in Doze
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    // ── Request codes (must be stable across process restarts) ────────────────

    /** One-time alarm request code for [macroId]. */
    private fun requestCodeOnce(macroId: String): Int = macroId.hashCode() and 0x0FFFFFFF

    /** Interval alarm request code for [macroId] (offset to avoid collision with once). */
    private fun requestCodeInterval(macroId: String): Int =
        (macroId.hashCode() and 0x0FFFFFFF) or 0x10000000
}

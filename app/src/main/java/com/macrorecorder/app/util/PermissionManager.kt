package com.macrorecorder.app.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.macrorecorder.app.R
import com.macrorecorder.app.service.accessibility.MacroRecorderAccessibilityService

/**
 * Single source of truth for all permission checks and the deep-link intents
 * that guide the user to the correct system settings screen.
 *
 * Critical permissions (OVERLAY + ACCESSIBILITY) are required before any
 * recording or playback can start. Non-critical ones improve the experience.
 */
class PermissionManager(private val context: Context) {

    enum class Type(
        @StringRes val labelRes: Int,
        @StringRes val descRes: Int,
        /** If true the app cannot function without this permission. */
        val isCritical: Boolean
    ) {
        OVERLAY(
            R.string.permission_overlay_label,
            R.string.permission_overlay_desc,
            isCritical = true
        ),
        ACCESSIBILITY(
            R.string.permission_accessibility_label,
            R.string.permission_accessibility_desc,
            isCritical = true
        ),
        NOTIFICATIONS(
            R.string.permission_notification_label,
            R.string.permission_notification_desc,
            isCritical = false
        ),
        EXACT_ALARM(
            R.string.permission_exact_alarm_label,
            R.string.permission_exact_alarm_desc,
            isCritical = false
        )
    }

    data class State(val type: Type, val isGranted: Boolean)

    /** Returns the current grant state of every managed permission. */
    fun checkAll(): List<State> = Type.values().map { State(it, isGranted(it)) }

    /** Returns true only when every critical permission is granted. */
    fun allCriticalGranted(): Boolean =
        Type.values().filter { it.isCritical }.all { isGranted(it) }

    fun isGranted(type: Type): Boolean = when (type) {
        Type.OVERLAY      -> Settings.canDrawOverlays(context)
        Type.ACCESSIBILITY -> isAccessibilityServiceEnabled()
        Type.NOTIFICATIONS -> isNotificationGranted()
        Type.EXACT_ALARM   -> isExactAlarmGranted()
    }

    /**
     * Returns an [Intent] that opens the exact system settings page needed to
     * grant the given permission. Handles API-level differences internally.
     */
    fun settingsIntent(type: Type): Intent = when (type) {
        Type.OVERLAY -> Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        Type.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        Type.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            appDetailsIntent()
        }
        Type.EXACT_ALARM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            appDetailsIntent()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        // Format in the setting: "pkg/fully.qualified.ClassName:pkg2/..."
        val target = "${context.packageName}/" +
            MacroRecorderAccessibilityService::class.java.canonicalName
        return enabled.split(":").any { it.equals(target, ignoreCase = true) }
    }

    private fun isNotificationGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    private fun isExactAlarmGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java)
                ?.canScheduleExactAlarms() ?: true
        } else true

    private fun appDetailsIntent() = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )
}

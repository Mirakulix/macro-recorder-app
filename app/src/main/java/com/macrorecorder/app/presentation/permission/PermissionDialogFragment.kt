package com.macrorecorder.app.presentation.permission

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.macrorecorder.app.R
import com.macrorecorder.app.util.PermissionManager

/**
 * Modal dialog shown whenever critical permissions are missing.
 *
 * Behaviour:
 *  - Lists all managed permissions with their current grant state.
 *  - Each denied permission shows a "Fix" button that deep-links to the
 *    correct system settings screen.
 *  - Automatically dismisses itself in [onResume] once all critical
 *    permissions are granted (i.e. after the user returns from settings).
 *  - "Later" button allows the user to dismiss it manually; it will
 *    reappear on the next [onResume] of the host activity.
 */
class PermissionDialogFragment : DialogFragment() {

    private lateinit var permissionManager: PermissionManager
    private var permissionsContainer: LinearLayout? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        permissionManager = PermissionManager(requireContext())

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_permissions, null)
        permissionsContainer = view.findViewById(R.id.permissionsContainer)
        bindPermissions()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_dialog_title)
            .setView(view)
            .setNegativeButton(R.string.btn_later, null)
            .create()
    }

    override fun onResume() {
        super.onResume()
        if (::permissionManager.isInitialized) {
            bindPermissions()
            if (permissionManager.allCriticalGranted()) dismiss()
        }
    }

    private fun bindPermissions() {
        val container = permissionsContainer ?: return
        container.removeAllViews()

        for (state in permissionManager.checkAll()) {
            val row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_permission, container, false)

            row.findViewById<ImageView>(R.id.ivStatus).apply {
                setImageResource(
                    if (state.isGranted) R.drawable.ic_check_circle
                    else R.drawable.ic_error_circle
                )
                imageTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    if (state.isGranted) R.color.permission_granted
                    else R.color.permission_denied
                )
            }
            row.findViewById<TextView>(R.id.tvPermissionLabel).setText(state.type.labelRes)
            row.findViewById<TextView>(R.id.tvPermissionDesc).setText(state.type.descRes)
            row.findViewById<MaterialButton>(R.id.btnFix).apply {
                visibility = if (state.isGranted) View.GONE else View.VISIBLE
                setOnClickListener {
                    startActivity(permissionManager.settingsIntent(state.type))
                }
            }

            container.addView(row)
        }
    }

    companion object {
        const val TAG = "PermissionDialog"
        fun newInstance() = PermissionDialogFragment()
    }
}

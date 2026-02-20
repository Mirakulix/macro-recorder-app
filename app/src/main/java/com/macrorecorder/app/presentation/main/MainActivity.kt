package com.macrorecorder.app.presentation.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.macrorecorder.app.MacroRecorderApp
import com.macrorecorder.app.R
import com.macrorecorder.app.databinding.ActivityMainBinding
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.MacroSettings
import com.macrorecorder.app.presentation.permission.PermissionDialogFragment
import com.macrorecorder.app.service.recording.RecordingForegroundService
import com.macrorecorder.app.service.recording.RecordingManager
import com.macrorecorder.app.util.PermissionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        /** Sent by [RecordingForegroundService] after recording stops. */
        const val ACTION_SHOW_SAVE_DIALOG = "com.macrorecorder.app.SHOW_SAVE_DIALOG"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        permissionManager = PermissionManager(this)

        binding.recyclerViewMacros.layoutManager = LinearLayoutManager(this)

        binding.fabNewRecording.setOnClickListener {
            if (permissionManager.allCriticalGranted()) {
                startRecording()
            } else {
                showPermissionDialog()
            }
        }

        // Handle the case where the activity was launched via ACTION_SHOW_SAVE_DIALOG
        // (e.g. cold start while recording was running)
        if (intent?.action == ACTION_SHOW_SAVE_DIALOG) {
            showSaveDialog()
        }

        // TODO: observe MainViewModel.macroList → update adapter + empty-state visibility
    }

    override fun onResume() {
        super.onResume()
        if (!permissionManager.allCriticalGranted()) {
            showPermissionDialog()
        }
    }

    /** Called when the activity is already running and receives a new intent (singleTop). */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_SHOW_SAVE_DIALOG) {
            showSaveDialog()
        }
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    private fun startRecording() {
        startService(RecordingForegroundService.startIntent(this))
    }

    // ── Save dialog ───────────────────────────────────────────────────────────

    private fun showSaveDialog() {
        val result = RecordingManager.lastResult
        if (result == null || result.events.isEmpty()) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_macro, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etMacroName).apply {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            setText(getString(R.string.dialog_save_macro_default_name, timestamp))
            selectAll()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_save_macro_title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val name = etName.text?.toString()?.ifBlank { null }
                    ?: getString(R.string.dialog_save_macro_default_name,
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
                saveMacro(name, result)
            }
            .setNegativeButton(R.string.dialog_discard, null)
            .show()
    }

    private fun saveMacro(name: String, result: RecordingManager.RecordingResult) {
        val repo = (applicationContext as MacroRecorderApp).repository
        lifecycleScope.launch {
            val macro = Macro(
                id         = UUID.randomUUID().toString(),
                name       = name,
                createdAt  = System.currentTimeMillis(),
                duration   = result.durationMs,
                eventCount = result.events.size,
                settings   = MacroSettings()
            )
            repo.saveMacro(macro, result.events)
            // TODO: trigger list refresh via ViewModel
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun showPermissionDialog() {
        if (supportFragmentManager.findFragmentByTag(PermissionDialogFragment.TAG) == null) {
            PermissionDialogFragment.newInstance()
                .show(supportFragmentManager, PermissionDialogFragment.TAG)
        }
    }
}

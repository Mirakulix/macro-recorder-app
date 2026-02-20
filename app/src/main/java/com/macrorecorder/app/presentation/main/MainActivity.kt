package com.macrorecorder.app.presentation.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.macrorecorder.app.MacroRecorderApp
import com.macrorecorder.app.R
import com.macrorecorder.app.databinding.ActivityMainBinding
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.MacroSettings
import com.macrorecorder.app.presentation.permission.PermissionDialogFragment
import com.macrorecorder.app.presentation.detail.MacroDetailActivity
import com.macrorecorder.app.service.execution.ExecutionForegroundService
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
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: MacroAdapter

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        permissionManager = PermissionManager(this)

        setupRecyclerView()
        observeMacros()

        binding.fabNewRecording.setOnClickListener {
            if (permissionManager.allCriticalGranted()) {
                startRecording()
            } else {
                showPermissionDialog()
            }
        }

        if (intent?.action == ACTION_SHOW_SAVE_DIALOG) {
            showSaveDialog()
        }
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

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = MacroAdapter(
            onEdit   = { macro -> openDetail(macro) },
            onPlay   = { macro -> startPlayback(macro) },
            onDelete = { macro -> confirmDelete(macro) }
        )
        binding.recyclerViewMacros.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMacros.adapter = adapter
    }

    private fun observeMacros() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.macros.collect { macros ->
                    adapter.submitList(macros)
                    binding.tvEmptyState.visibility =
                        if (macros.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    private fun startRecording() {
        startService(RecordingForegroundService.startIntent(this))
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    private fun openDetail(macro: Macro) {
        startActivity(MacroDetailActivity.startIntent(this, macro.id))
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    private fun startPlayback(macro: Macro) {
        startService(ExecutionForegroundService.startIntent(this, macro.id))
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private fun confirmDelete(macro: Macro) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_macro_title)
            .setMessage(R.string.dialog_delete_macro_message)
            .setPositiveButton(R.string.btn_delete) { _, _ -> viewModel.deleteMacro(macro) }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
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

package com.macrorecorder.app.presentation.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.macrorecorder.app.presentation.detail.MacroDetailActivity
import com.macrorecorder.app.service.execution.ExecutionForegroundService
import com.macrorecorder.app.service.recording.RecordingForegroundService
import com.macrorecorder.app.service.recording.RecordingManager
import com.macrorecorder.app.util.MacroExporter
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

    // ── Activity result launchers ─────────────────────────────────────────────

    private val importLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri ?: return@registerForActivityResult
            viewModel.importMacro(uri)
        }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        permissionManager = PermissionManager(this)

        setupRecyclerView()
        observeMacros()
        observeImportResult()

        binding.fabNewRecording.setOnClickListener {
            if (permissionManager.allCriticalGranted()) {
                startRecording()
            } else {
                showPermissionDialog()
            }
        }

        handleIntent(intent)
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
        intent ?: return
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_SAVE_DIALOG -> showSaveDialog()
            Intent.ACTION_VIEW      -> intent.data?.let { viewModel.importMacro(it) }
        }
    }

    // ── Options menu ──────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_import) {
            importLauncher.launch(
                arrayOf(MacroExporter.MIME_TYPE, "application/json", "*/*")
            )
            return true
        }
        return super.onOptionsItemSelected(item)
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

    private fun observeImportResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.importResult.collect { result ->
                    val msg = when (result) {
                        is MainViewModel.ImportResult.Success ->
                            getString(R.string.toast_import_success, result.macroName)
                        is MainViewModel.ImportResult.Error ->
                            getString(R.string.toast_import_error)
                    }
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
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

        val dialogView = layoutInflater.inflate(R.layout.dialog_save_macro, null)
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
        if (supportFragmentManager.findFragmentByTag(
                com.macrorecorder.app.presentation.permission.PermissionDialogFragment.TAG) == null
        ) {
            com.macrorecorder.app.presentation.permission.PermissionDialogFragment.newInstance()
                .show(supportFragmentManager,
                    com.macrorecorder.app.presentation.permission.PermissionDialogFragment.TAG)
        }
    }
}

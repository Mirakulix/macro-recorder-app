package com.macrorecorder.app.presentation.detail

import android.content.Context
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
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.macrorecorder.app.R
import com.macrorecorder.app.databinding.ActivityMacroDetailBinding
import com.macrorecorder.app.util.MacroExporter
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.MacroSettings
import com.macrorecorder.app.service.execution.MacroScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Per-macro settings screen.
 *
 * Opens with a [EXTRA_MACRO_ID] intent extra, loads the macro via [MacroDetailViewModel],
 * lets the user edit name / playback / behavior / schedule, then on "Save":
 *  1. Persists the updated macro via the repository.
 *  2. Updates AlarmManager via [MacroScheduler].
 */
class MacroDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_MACRO_ID = "macro_id"

        fun startIntent(context: Context, macroId: String): Intent =
            Intent(context, MacroDetailActivity::class.java)
                .putExtra(EXTRA_MACRO_ID, macroId)
    }

    private lateinit var binding: ActivityMacroDetailBinding
    private val viewModel: MacroDetailViewModel by viewModels {
        MacroDetailViewModel.factory(
            application,
            intent.getStringExtra(EXTRA_MACRO_ID) ?: error("No macro ID provided")
        )
    }

    // Picked one-time scheduled timestamp; null = not set / cleared
    private var pickedScheduledTimeMs: Long? = null

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(MacroExporter.MIME_TYPE)) { uri ->
            uri ?: return@registerForActivityResult
            viewModel.export(this, uri) { success ->
                val msg = if (success) R.string.toast_export_success else R.string.toast_export_error
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

    // Map chip view-ID → Calendar.DAY_OF_WEEK constant (Mon=2 … Sun=1)
    private val chipDayMap = linkedMapOf(
        R.id.chipMon to Calendar.MONDAY,
        R.id.chipTue to Calendar.TUESDAY,
        R.id.chipWed to Calendar.WEDNESDAY,
        R.id.chipThu to Calendar.THURSDAY,
        R.id.chipFri to Calendar.FRIDAY,
        R.id.chipSat to Calendar.SATURDAY,
        R.id.chipSun to Calendar.SUNDAY
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacroDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSpeedSlider()
        setupInfiniteSwitch()
        setupSchedulePickers()
        setupSaveButton()
        setupExportMenu()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.macro.collect { macro ->
                    if (macro != null) populateFields(macro)
                }
            }
        }
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    private fun setupExportMenu() {
        binding.toolbar.inflateMenu(R.menu.menu_macro_detail)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_export) {
                val name = viewModel.macro.value?.name ?: "macro"
                exportLauncher.launch(MacroExporter.suggestedFilename(name))
                true
            } else false
        }
    }

    // ── Field setup ───────────────────────────────────────────────────────────

    private fun setupSpeedSlider() {
        binding.sliderSpeed.addOnChangeListener { _, value, _ ->
            binding.tvSpeedValue.text = "%.2f×".format(value)
        }
    }

    private fun setupInfiniteSwitch() {
        binding.switchInfinite.setOnCheckedChangeListener { _, checked ->
            binding.layoutRepeatCount.visibility = if (checked) View.GONE else View.VISIBLE
        }
    }

    private fun setupSchedulePickers() {
        binding.btnPickDateTime.setOnClickListener { showDatePicker() }
        binding.btnClearSchedule.setOnClickListener {
            pickedScheduledTimeMs = null
            updateScheduleDisplay(null)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener { saveAndFinish() }
    }

    // ── Populate ──────────────────────────────────────────────────────────────

    private fun populateFields(macro: Macro) {
        val s = macro.settings

        binding.etName.setText(macro.name)

        // Repeat
        val infinite = s.repeatCount == -1
        binding.switchInfinite.isChecked = infinite
        binding.layoutRepeatCount.visibility = if (infinite) View.GONE else View.VISIBLE
        if (!infinite) binding.etRepeatCount.setText(s.repeatCount.toString())

        // Speed (clamp to slider range)
        val clampedSpeed = s.speed.coerceIn(0.25f, 4.0f)
        binding.sliderSpeed.value = clampedSpeed
        binding.tvSpeedValue.text = "%.2f×".format(clampedSpeed)

        // Pause between runs → seconds
        val pauseSecs = s.pauseBetweenRunsMs / 1_000.0
        if (pauseSecs > 0) binding.etPauseSecs.setText("%.1f".format(pauseSecs))

        // Behavior toggles
        binding.switchEmergencyStop.isChecked   = s.emergencyStopEnabled
        binding.switchVibration.isChecked       = s.vibrationEnabled
        binding.switchVisualIndicator.isChecked = s.visualIndicatorEnabled

        // Schedule — one-time
        pickedScheduledTimeMs = s.scheduledTimeMs
        updateScheduleDisplay(s.scheduledTimeMs)

        // Schedule — interval
        s.intervalMinutes?.let { binding.etIntervalMinutes.setText(it.toString()) }

        // Schedule — selected days
        chipDayMap.forEach { (chipId, calDay) ->
            binding.chipGroupDays.findViewById<Chip>(chipId)?.isChecked =
                calDay in s.selectedDays
        }
    }

    // ── Date / time picker ────────────────────────────────────────────────────

    private fun showDatePicker() {
        val initialMs = pickedScheduledTimeMs
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.picker_select_date)
            .setSelection(initialMs)
            .build()
            .also { picker ->
                picker.addOnPositiveButtonClickListener { dateMs ->
                    showTimePicker(dateMs)
                }
            }
            .show(supportFragmentManager, "date_picker")
    }

    private fun showTimePicker(dateMs: Long) {
        val now = Calendar.getInstance()
        MaterialTimePicker.Builder()
            .setTitleText(R.string.picker_select_time)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(now.get(Calendar.HOUR_OF_DAY))
            .setMinute(now.get(Calendar.MINUTE))
            .build()
            .also { picker ->
                picker.addOnPositiveButtonClickListener {
                    val combined = dateMs +
                        picker.hour * 3_600_000L +
                        picker.minute * 60_000L
                    pickedScheduledTimeMs = combined
                    updateScheduleDisplay(combined)
                }
            }
            .show(supportFragmentManager, "time_picker")
    }

    private fun updateScheduleDisplay(ms: Long?) {
        if (ms == null) {
            binding.tvScheduledTime.text = getString(R.string.label_not_scheduled)
            binding.btnClearSchedule.visibility = View.GONE
        } else {
            binding.tvScheduledTime.text =
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(ms))
            binding.btnClearSchedule.visibility = View.VISIBLE
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private fun saveAndFinish() {
        val macro = viewModel.macro.value ?: return

        val name = binding.etName.text?.toString()?.trim()?.ifEmpty { macro.name } ?: macro.name

        val repeatCount = if (binding.switchInfinite.isChecked) {
            -1
        } else {
            binding.etRepeatCount.text?.toString()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        }

        val speed = binding.sliderSpeed.value

        val pauseMs = binding.etPauseSecs.text?.toString()?.toDoubleOrNull()
            ?.times(1_000.0)?.toLong() ?: 0L

        val intervalMinutes = binding.etIntervalMinutes.text?.toString()
            ?.toIntOrNull()?.takeIf { it > 0 }

        val selectedDays = chipDayMap
            .filter { (chipId, _) ->
                binding.chipGroupDays.findViewById<Chip>(chipId)?.isChecked == true
            }
            .values.toList()

        val updated = macro.copy(
            name = name,
            settings = MacroSettings(
                repeatCount            = repeatCount,
                speed                  = speed,
                pauseBetweenRunsMs     = pauseMs,
                scheduledTimeMs        = pickedScheduledTimeMs,
                intervalMinutes        = intervalMinutes,
                selectedDays           = selectedDays,
                emergencyStopEnabled   = binding.switchEmergencyStop.isChecked,
                vibrationEnabled       = binding.switchVibration.isChecked,
                visualIndicatorEnabled = binding.switchVisualIndicator.isChecked
            )
        )

        viewModel.save(updated)

        // Keep AlarmManager in sync
        val hasSchedule = updated.settings.scheduledTimeMs != null ||
            updated.settings.intervalMinutes != null
        if (hasSchedule) {
            MacroScheduler.schedule(this, updated)
        } else {
            MacroScheduler.cancel(this, updated.id)
        }

        finish()
    }
}

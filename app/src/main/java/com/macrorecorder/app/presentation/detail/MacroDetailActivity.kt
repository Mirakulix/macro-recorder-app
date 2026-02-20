package com.macrorecorder.app.presentation.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.macrorecorder.app.databinding.ActivityMacroDetailBinding
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.MacroSettings
import kotlinx.coroutines.launch

/**
 * Per-macro settings screen.
 *
 * Opens with a [EXTRA_MACRO_ID] intent extra, loads the macro, lets the user
 * edit name / playback settings / behavior toggles, then saves on "Save".
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacroDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSpeedSlider()
        setupInfiniteSwitch()
        setupSaveButton()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.macro.collect { macro ->
                    if (macro != null) populateFields(macro)
                }
            }
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

        // Speed (clamp to slider range 0.25–4.0)
        val clampedSpeed = s.speed.coerceIn(0.25f, 4.0f)
        binding.sliderSpeed.value = clampedSpeed
        binding.tvSpeedValue.text = "%.2f×".format(clampedSpeed)

        // Pause between runs → convert ms to seconds
        val pauseSecs = s.pauseBetweenRunsMs / 1_000.0
        if (pauseSecs > 0) binding.etPauseSecs.setText("%.1f".format(pauseSecs))

        // Behavior toggles
        binding.switchEmergencyStop.isChecked    = s.emergencyStopEnabled
        binding.switchVibration.isChecked        = s.vibrationEnabled
        binding.switchVisualIndicator.isChecked  = s.visualIndicatorEnabled
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

        val updated = macro.copy(
            name = name,
            settings = MacroSettings(
                repeatCount          = repeatCount,
                speed                = speed,
                pauseBetweenRunsMs   = pauseMs,
                scheduledTimeMs      = macro.settings.scheduledTimeMs,
                intervalMinutes      = macro.settings.intervalMinutes,
                selectedDays         = macro.settings.selectedDays,
                emergencyStopEnabled = binding.switchEmergencyStop.isChecked,
                vibrationEnabled     = binding.switchVibration.isChecked,
                visualIndicatorEnabled = binding.switchVisualIndicator.isChecked
            )
        )

        viewModel.save(updated)
        finish()
    }
}

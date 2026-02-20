package com.macrorecorder.app.presentation.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.macrorecorder.app.databinding.ActivityMainBinding
import com.macrorecorder.app.presentation.permission.PermissionDialogFragment
import com.macrorecorder.app.util.PermissionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        permissionManager = PermissionManager(this)

        binding.recyclerViewMacros.layoutManager = LinearLayoutManager(this)

        binding.fabNewRecording.setOnClickListener {
            if (permissionManager.allCriticalGranted()) {
                // TODO: start RecordingForegroundService + OverlayWidgetService
            } else {
                showPermissionDialog()
            }
        }

        // TODO: observe MainViewModel.macroList → update adapter + empty-state visibility
    }

    override fun onResume() {
        super.onResume()
        // Re-check every time the user returns to the app (e.g. from system settings).
        if (!permissionManager.allCriticalGranted()) {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        if (supportFragmentManager.findFragmentByTag(PermissionDialogFragment.TAG) == null) {
            PermissionDialogFragment.newInstance()
                .show(supportFragmentManager, PermissionDialogFragment.TAG)
        }
    }
}

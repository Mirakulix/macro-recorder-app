package com.macrorecorder.app.presentation.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.macrorecorder.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.recyclerViewMacros.layoutManager = LinearLayoutManager(this)

        binding.fabNewRecording.setOnClickListener {
            // TODO: check permissions, then start RecordingForegroundService + OverlayWidgetService
        }

        // TODO: observe MainViewModel.macroList and update adapter / empty-state visibility
    }
}

package com.macrorecorder.app.presentation.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.macrorecorder.app.MacroRecorderApp
import com.macrorecorder.app.domain.model.Macro
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as MacroRecorderApp).repository

    val macros: StateFlow<List<Macro>> = repository.getAllMacros()
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000),
            initialValue  = emptyList()
        )

    fun deleteMacro(macro: Macro) {
        viewModelScope.launch {
            repository.deleteMacro(macro.id)
        }
    }
}

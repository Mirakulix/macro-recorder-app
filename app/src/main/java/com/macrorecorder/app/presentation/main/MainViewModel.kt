package com.macrorecorder.app.presentation.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.macrorecorder.app.MacroRecorderApp
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.service.execution.MacroScheduler
import com.macrorecorder.app.util.MacroExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as MacroRecorderApp).repository

    val macros: StateFlow<List<Macro>> = repository.getAllMacros()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Import feedback ───────────────────────────────────────────────────────

    sealed class ImportResult {
        data class Success(val macroName: String) : ImportResult()
        data class Error(val message: String)     : ImportResult()
    }

    private val _importResult = MutableSharedFlow<ImportResult>(extraBufferCapacity = 1)
    val importResult: SharedFlow<ImportResult> = _importResult.asSharedFlow()

    // ── Actions ───────────────────────────────────────────────────────────────

    fun deleteMacro(macro: Macro) {
        viewModelScope.launch {
            MacroScheduler.cancel(getApplication(), macro.id)
            repository.deleteMacro(macro.id)
        }
    }

    fun importMacro(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val (macro, events) = MacroExporter.import(getApplication(), uri)
                        ?: throw IllegalArgumentException("Invalid or corrupt macro file")
                    repository.saveMacro(macro, events)
                    macro.name
                }
            }
            _importResult.emit(
                result.fold(
                    onSuccess = { ImportResult.Success(it) },
                    onFailure = { ImportResult.Error(it.message ?: "Import failed") }
                )
            )
        }
    }
}

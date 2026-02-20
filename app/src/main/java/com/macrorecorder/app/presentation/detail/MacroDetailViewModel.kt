package com.macrorecorder.app.presentation.detail

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.macrorecorder.app.MacroRecorderApp
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.util.MacroExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MacroDetailViewModel(
    app: Application,
    private val macroId: String
) : AndroidViewModel(app) {

    private val repository = (app as MacroRecorderApp).repository

    private val _macro = MutableStateFlow<Macro?>(null)
    val macro: StateFlow<Macro?> = _macro.asStateFlow()

    init {
        viewModelScope.launch {
            _macro.value = repository.getMacroById(macroId)
        }
    }

    fun save(updated: Macro) {
        viewModelScope.launch {
            repository.updateMacro(updated)
            _macro.value = updated
        }
    }

    /** Loads touch events for the current macro and exports them to [uri]. */
    fun export(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val macro = _macro.value ?: error("Macro not loaded")
                    val events = repository.loadEvents(macro.id)
                    MacroExporter.export(context, macro, events, uri)
                }.isSuccess
            }
            onResult(success)
        }
    }

    companion object {
        fun factory(app: Application, macroId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MacroDetailViewModel(app, macroId) as T
            }
    }
}

package com.github.abelnightroad.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.abelnightroad.data.CardRepository
import com.github.abelnightroad.data.CsvImport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ImportViewModel(private val repository: CardRepository) : ViewModel() {

    sealed interface ImportState {
        object Idle : ImportState
        object Importing : ImportState
        data class Done(val imported: Int, val skipped: Int) : ImportState
        data class Error(val message: String) : ImportState
    }

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    fun importUri(context: Context, uri: Uri, tag: String) {
        viewModelScope.launch {
            _state.value = ImportState.Importing
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val result = repository.importCsv(stream, tag)
                    _state.value = ImportState.Done(result.cards.size, result.skipped)
                } ?: run {
                    _state.value = ImportState.Error("Could not open file")
                }
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }
}

package com.gitlab.abelnightroad.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.ScryfallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScryfallImportViewModel(private val scryfall: ScryfallRepository) : ViewModel() {

    sealed interface State {
        object Idle : State
        object Importing : State
        data class Done(val inserted: Int) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    fun importUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = State.Importing
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val inserted = scryfall.importBulk(stream)
                    _state.value = State.Done(inserted)
                } ?: run {
                    _state.value = State.Error("Could not open file")
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Import failed")
            }
        }
    }
}

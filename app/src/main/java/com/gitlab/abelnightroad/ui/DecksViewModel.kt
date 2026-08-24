package com.gitlab.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.db.DeckEntity
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.db.FormatCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DecksViewModel(private val repository: DeckRepository) : ViewModel() {

    private val _selectedFormat = MutableStateFlow("All")
    val selectedFormat: StateFlow<String> = _selectedFormat

    val formatCounts: StateFlow<List<FormatCount>> = repository.formatCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val decks: StateFlow<List<DeckEntity>> = _selectedFormat
        .flatMapLatest { format ->
            if (format == "All") repository.allDecks()
            else repository.decksByFormat(format)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFormat(format: String) {
        _selectedFormat.value = format
    }

    fun createDeck(name: String, format: String, source: String = "manual", onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createDeck(name, format, source)
            onCreated(id)
        }
    }

    fun importFromMeta(name: String, format: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createDeck(name, format, "meta")
            onCreated(id)
        }
    }

    fun deleteDeck(deck: DeckEntity) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
        }
    }

    fun cloneDeck(deckId: Long, onCloned: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.cloneDeck(deckId)
            onCloned(newId)
        }
    }

    fun deleteDecksByFormat(format: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDecksByFormat(format)
            onDone()
        }
    }
}

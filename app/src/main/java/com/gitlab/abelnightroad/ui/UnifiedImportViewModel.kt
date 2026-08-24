package com.gitlab.abelnightroad.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.MtgGoldfishCsvParser
import com.gitlab.abelnightroad.data.MoxfieldApiClient
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.TappedOutCsvParser
import com.gitlab.abelnightroad.data.TappedOutDckParser
import com.gitlab.abelnightroad.data.UniversalDecklistParser
import com.gitlab.abelnightroad.data.MetaDeckCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ImportSource(val label: String) {
    MOXFIELD("Moxfield"),
    MTG_GOLDFISH("MTG Goldfish"),
    TAPPED_OUT("TappedOut"),
    EDH_PLAY("EDH Play")
}

sealed interface ImportState {
    data object Idle : ImportState
    data object Parsing : ImportState
    data class Preview(val cards: List<MetaDeckCard>, val source: ImportSource) : ImportState
    data object Importing : ImportState
    data class Success(val deckId: Long) : ImportState
    data class Error(val message: String) : ImportState
}

class UnifiedImportViewModel(
    application: Application,
    private val deckRepository: DeckRepository,
    private val scryfall: ScryfallRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state

    private val _deckName = MutableStateFlow("")
    val deckName: StateFlow<String> = _deckName

    private val _format = MutableStateFlow("Commander")
    val format: StateFlow<String> = _format

    fun setDeckName(name: String) { _deckName.value = name }
    fun setFormat(format: String) { _format.value = format }

    fun parseMoxfieldUrl(url: String) {
        val deckId = MoxfieldApiClient.extractDeckId(url)
        if (deckId == null) {
            _state.value = ImportState.Error("Invalid Moxfield URL")
            return
        }
        _state.value = ImportState.Parsing
        viewModelScope.launch {
            try {
                val deck = MoxfieldApiClient.fetchDeck(deckId)
                val cards = MoxfieldApiClient.toMetaDeckCards(deck)
                if (cards.isEmpty()) {
                    _state.value = ImportState.Error("No cards found in Moxfield deck")
                    return@launch
                }
                _deckName.value = _deckName.value.ifBlank { deck.name }
                _state.value = ImportState.Preview(cards, ImportSource.MOXFIELD)
            } catch (e: Exception) {
                _state.value = ImportState.Error("Failed to fetch Moxfield deck: ${e.message}")
            }
        }
    }

    fun parseText(text: String, source: ImportSource) {
        if (text.isBlank()) {
            _state.value = ImportState.Error("No text provided")
            return
        }
        _state.value = ImportState.Parsing
        viewModelScope.launch {
            try {
                val cards = when (source) {
                    ImportSource.MTG_GOLDFISH -> {
                        if (text.contains(",")) MtgGoldfishCsvParser.parse(text)
                        else UniversalDecklistParser.parse(text)
                    }
                    ImportSource.TAPPED_OUT -> {
                        if (text.contains(",")) TappedOutCsvParser.parse(text)
                        else TappedOutDckParser.parse(text)
                    }
                    ImportSource.EDH_PLAY -> {
                        com.gitlab.abelnightroad.data.EdhPlayDecklistParser.parse(text)
                    }
                    ImportSource.MOXFIELD -> {
                        UniversalDecklistParser.parse(text)
                    }
                }
                if (cards.isEmpty()) {
                    _state.value = ImportState.Error("No cards found in $source")
                    return@launch
                }
                _state.value = ImportState.Preview(cards, source)
            } catch (e: Exception) {
                _state.value = ImportState.Error("Parse error: ${e.message}")
            }
        }
    }

    fun parseFile(uri: Uri, source: ImportSource) {
        _state.value = ImportState.Parsing
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw Exception("Could not read file")

                val cards = when (source) {
                    ImportSource.MTG_GOLDFISH -> {
                        if (text.contains(",")) MtgGoldfishCsvParser.parse(text)
                        else UniversalDecklistParser.parse(text)
                    }
                    ImportSource.TAPPED_OUT -> {
                        if (text.contains(",")) TappedOutCsvParser.parse(text)
                        else TappedOutDckParser.parse(text)
                    }
                    ImportSource.EDH_PLAY -> {
                        com.gitlab.abelnightroad.data.EdhPlayDecklistParser.parse(text)
                    }
                    ImportSource.MOXFIELD -> {
                        UniversalDecklistParser.parse(text)
                    }
                }
                if (cards.isEmpty()) {
                    _state.value = ImportState.Error("No cards found in file")
                    return@launch
                }
                _state.value = ImportState.Preview(cards, source)
            } catch (e: Exception) {
                _state.value = ImportState.Error("File read error: ${e.message}")
            }
        }
    }

    fun importDeck() {
        val current = _state.value
        if (current !is ImportState.Preview) return

        _state.value = ImportState.Importing
        viewModelScope.launch {
            try {
                val name = _deckName.value.ifBlank { "${current.source.label} Import" }
                val deckId = deckRepository.createDeck(name, _format.value, current.source.name.lowercase())
                importDeckCards(deckId, current.cards, deckRepository, scryfall, _format.value)
                _state.value = ImportState.Success(deckId)
            } catch (e: Exception) {
                _state.value = ImportState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun reset() { _state.value = ImportState.Idle }
}

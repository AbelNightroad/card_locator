package com.gitlab.abelnightroad.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ImportSource(val label: String, val urlHost: String) {
    MOXFIELD("Moxfield", "moxfield.com"),
    EDHREC("EDHREC", "edhrec.com"),
    ARCHIDEKT("Archidekt", "archidekt.com"),
    MTG_GOLDFISH("MTG Goldfish", "mtggoldfish.com"),
    TAPPED_OUT("TappedOut", "tappedout.net");

    companion object {
        fun detect(url: String): ImportSource? {
            val host = url.trim().lowercase()
                .substringAfter("://")
                .substringBefore("/")
                .substringBefore("?")
                .substringBefore("#")
                .removePrefix("www.")
            return entries.firstOrNull { host == it.urlHost || host.endsWith(".${it.urlHost}") }
        }
    }
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

    fun parseText(text: String) {
        if (text.isBlank()) {
            _state.value = ImportState.Error("No text provided")
            return
        }
        _state.value = ImportState.Parsing
        viewModelScope.launch {
            try {
                // Auto-detect source from text
                val firstLine = text.trim().lineSequence().firstOrNull()
                val detectedSource = firstLine?.takeIf { it.isNotBlank() }
                    ?.let { ImportSource.detect(it) }
                    ?: ImportSource.MOXFIELD // default if no text

                val cards = when (detectedSource) {
                    ImportSource.MTG_GOLDFISH -> {
                        if (text.contains(",")) MtgGoldfishCsvParser.parse(text)
                        else UniversalDecklistParser.parse(text)
                    }
                    ImportSource.TAPPED_OUT -> {
                        // TappedOut is blocked, show error
                        throw Exception("TappedOut URLs are blocked (Cloudflare) — copy the deck list and paste it instead")
                    }
                    ImportSource.MOXFIELD,
                    ImportSource.EDHREC,
                    ImportSource.ARCHIDEKT -> {
                        UniversalDecklistParser.parse(text)
                    }
                }
                if (cards.isEmpty()) {
                    _state.value = ImportState.Error("No cards found in $detectedSource")
                    return@launch
                }
                _state.value = ImportState.Preview(cards, detectedSource)
            } catch (e: Exception) {
                _state.value = ImportState.Error("Parse error: ${e.message}")
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
    fun parseFile(uri: String, source: ImportSource) { _state.value = ImportState.Error("File import from source chips disabled — use auto-detection by pasting decklist text") }
    fun parseFile(uri: Any, source: ImportSource) {
        _state.value = ImportState.Error("File import from source chips is disabled — use auto-detection by pasting decklist text")
    }
}
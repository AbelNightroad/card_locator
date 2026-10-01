package com.gitlab.abelnightroad.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.ArchidektApiClient
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.EdhrecApiClient
import com.gitlab.abelnightroad.data.MtgGoldfishCsvParser
import com.gitlab.abelnightroad.data.MoxfieldApiClient
import com.gitlab.abelnightroad.data.MtgGoldfishApiClient
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
    EDHREC("EDHREC"),
    ARCHIDEKT("Archidekt"),
    MTG_GOLDFISH("MTG Goldfish"),
    TAPPED_OUT("TappedOut")
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

    fun parseUrl(url: String, source: ImportSource) {
        if (url.isBlank()) {
            _state.value = ImportState.Error("No URL provided")
            return
        }
        _state.value = ImportState.Parsing
        viewModelScope.launch {
            try {
                when (source) {
                    ImportSource.MOXFIELD -> {
                        val deckId = MoxfieldApiClient.extractDeckId(url)
                            ?: throw Exception(
                                "Invalid Moxfield URL — expected: https://www.moxfield.com/decks/<id>"
                            )
                        val deck = MoxfieldApiClient.fetchDeck(deckId)
                        val cards = MoxfieldApiClient.toMetaDeckCards(deck)
                        if (cards.isEmpty()) throw Exception("No cards found in Moxfield deck")
                        _deckName.value = _deckName.value.ifBlank { deck.name }
                        _state.value = ImportState.Preview(cards, ImportSource.MOXFIELD)
                    }
                    ImportSource.EDHREC -> {
                        val deck = EdhrecApiClient.fetchFromUrl(url)
                        val cards = EdhrecApiClient.toMetaDeckCards(deck)
                        if (cards.isEmpty()) throw Exception("No cards found in EDHREC deck")
                        _state.value = ImportState.Preview(cards, ImportSource.EDHREC)
                    }
                    ImportSource.ARCHIDEKT -> {
                        val deckId = ArchidektApiClient.extractDeckId(url)
                            ?: throw Exception(
                                "Invalid Archidekt URL — expected: https://archidekt.com/decks/<id>"
                            )
                        val deck = ArchidektApiClient.fetchDeck(deckId)
                        val cards = ArchidektApiClient.toMetaDeckCards(deck)
                        if (cards.isEmpty()) throw Exception("No cards found in Archidekt deck")
                        _deckName.value = _deckName.value.ifBlank { deck.name }
                        _state.value = ImportState.Preview(cards, ImportSource.ARCHIDEKT)
                    }
                    ImportSource.MTG_GOLDFISH -> {
                        val deckId = MtgGoldfishApiClient.extractDeckId(url)
                            ?: throw Exception(
                                "Invalid MTG Goldfish URL — expected: https://www.mtggoldfish.com/deck/<id>"
                            )
                        val text = MtgGoldfishApiClient.fetchDecklist(deckId)
                        val cards = UniversalDecklistParser.parse(text)
                        if (cards.isEmpty()) throw Exception("No cards found in MTG Goldfish deck")
                        _state.value = ImportState.Preview(cards, ImportSource.MTG_GOLDFISH)
                    }
                    else -> {
                        _state.value = ImportState.Error("URL import not supported for ${source.label}")
                    }
                }
            } catch (e: Exception) {
                _state.value = ImportState.Error("Failed to fetch deck: ${e.message}")
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
                    ImportSource.MOXFIELD,
                    ImportSource.EDHREC,
                    ImportSource.ARCHIDEKT -> {
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
                    ImportSource.MOXFIELD,
                    ImportSource.EDHREC,
                    ImportSource.ARCHIDEKT -> {
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

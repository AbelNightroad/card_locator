package com.gitlab.abelnightroad.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.ArchidektApiClient
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.EdhrecApiClient
import com.gitlab.abelnightroad.data.MetaDeckCard
import com.gitlab.abelnightroad.data.MoxfieldApiClient
import com.gitlab.abelnightroad.data.MtgGoldfishApiClient
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.UniversalDecklistParser
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
    data class Preview(
        val cards: List<MetaDeckCard>,
        val sourceLabel: String,
        val sourceKey: String
    ) : ImportState
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

    fun parseUrl(url: String) {
        if (url.isBlank()) {
            _state.value = ImportState.Error("No URL provided")
            return
        }
        val source = ImportSource.detect(url)
        if (source == null) {
            _state.value = ImportState.Error("Unsupported deck URL — paste the decklist text instead")
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
                        _state.value = ImportState.Preview(cards, source.label, source.name.lowercase())
                    }
                    ImportSource.EDHREC -> {
                        val deck = EdhrecApiClient.fetchFromUrl(url)
                        val cards = EdhrecApiClient.toMetaDeckCards(deck)
                        if (cards.isEmpty()) throw Exception("No cards found in EDHREC deck")
                        _state.value = ImportState.Preview(cards, source.label, source.name.lowercase())
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
                        _state.value = ImportState.Preview(cards, source.label, source.name.lowercase())
                    }
                    ImportSource.MTG_GOLDFISH -> {
                        val deckId = MtgGoldfishApiClient.extractDeckId(url)
                            ?: throw Exception(
                                "Invalid MTG Goldfish URL — expected: https://www.mtggoldfish.com/deck/<id>"
                            )
                        val text = MtgGoldfishApiClient.fetchDecklist(deckId)
                        val cards = UniversalDecklistParser.parse(text)
                        if (cards.isEmpty()) throw Exception("No cards found in MTG Goldfish deck")
                        _state.value = ImportState.Preview(cards, source.label, source.name.lowercase())
                    }
                    ImportSource.TAPPED_OUT -> {
                        throw Exception("TappedOut URLs are blocked (Cloudflare) — copy the deck list and paste it instead")
                    }
                }
            } catch (e: Exception) {
                _state.value = ImportState.Error("Failed to fetch deck: ${e.message}")
            }
        }
    }

    fun parseText(text: String) {
        if (text.isBlank()) {
            _state.value = ImportState.Error("No text provided")
            return
        }
        _state.value = ImportState.Parsing
        viewModelScope.launch {
            try {
                val cards = UniversalDecklistParser.parse(text)
                if (cards.isEmpty()) {
                    _state.value = ImportState.Error("No cards found in the decklist")
                    return@launch
                }
                _state.value = ImportState.Preview(cards, "Text", "text")
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
                val name = _deckName.value.ifBlank { "${current.sourceLabel} Import" }
                val deckId = deckRepository.createDeck(name, _format.value, current.sourceKey)
                importDeckCards(deckId, current.cards, deckRepository, scryfall, _format.value)
                _state.value = ImportState.Success(deckId)
            } catch (e: Exception) {
                _state.value = ImportState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun reset() { _state.value = ImportState.Idle }

    fun clearInputs() {
        _deckName.value = ""
        _format.value = "Commander"
        _state.value = ImportState.Idle
    }

    fun parseFile(uri: Uri) { _state.value = ImportState.Error("File import is not supported — paste the decklist text instead") }
}
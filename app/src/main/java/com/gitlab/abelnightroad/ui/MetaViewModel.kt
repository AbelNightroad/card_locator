package com.gitlab.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.MetaDeckCard
import com.gitlab.abelnightroad.data.MetaDecklistLoader
import com.gitlab.abelnightroad.data.ScryfallImage
import com.gitlab.abelnightroad.data.ScryfallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MetaDeckEntry(
    val rank: Int,
    val name: String,
    val coverImageUrl: String,
    val metaPercentage: String,
    val url: String = ""
)

sealed interface MetaState {
    data object Idle : MetaState
    data object Loading : MetaState
    data class Success(val format: String, val decks: List<MetaDeckEntry>) : MetaState
    data class Error(val message: String) : MetaState
}

sealed interface DecklistState {
    data object Loading : DecklistState
    data class Success(val cards: List<MetaDeckCard>) : DecklistState
    data class Error(val message: String) : DecklistState
}

class MetaViewModel(private val scryfall: ScryfallRepository) : ViewModel() {

    private val _state = MutableStateFlow<MetaState>(MetaState.Idle)
    val state: StateFlow<MetaState> = _state

    private val _decklistState = MutableStateFlow<DecklistState?>(null)
    val decklistState: StateFlow<DecklistState?> = _decklistState

    /** archetype url -> resolved Scryfall artCrop url ("" = failed, keep thumb). Session-only. */
    private val _covers = MutableStateFlow<Map<String, String>>(emptyMap())
    val covers: StateFlow<Map<String, String>> = _covers

    private val coverMutex = Mutex()

    fun loadFormat(format: String) {
        _state.value = MetaState.Loading
        _decklistState.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val archetypes = MetaDecklistLoader.loadFormat(format)
                val decks = archetypes.mapIndexed { idx, arch ->
                    MetaDeckEntry(
                        rank = idx + 1,
                        name = arch.name,
                        coverImageUrl = arch.coverUrl,
                        metaPercentage = arch.metaPercent,
                        url = arch.url
                    )
                }
                _state.value = MetaState.Success(format, decks)
            } catch (e: Exception) {
                _state.value = MetaState.Error(e.message ?: "Failed to load metagame data")
            }
        }
    }

    fun loadDecklist(archetypeUrl: String) {
        _decklistState.value = DecklistState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cards = MetaDecklistLoader.load(archetypeUrl)
                _decklistState.value = DecklistState.Success(cards)
            } catch (e: Exception) {
                _decklistState.value = DecklistState.Error(e.message ?: "Failed to load decklist")
            }
        }
    }

    /**
     * Resolves a Scryfall artCrop cover for an archetype: exact-name local
     * lookup first (zero network), then a throttled mtgtop8 fetch of the
     * archetype's first deck to get real card names, looked up locally.
     * Failures cache "" so the mtgtop8 thumb stays and is never re-fetched.
     */
    fun resolveCover(entry: MetaDeckEntry) {
        val key = entry.url
        if (key.isBlank() || _covers.value.containsKey(key)) return
        viewModelScope.launch(Dispatchers.IO) {
            val resolved = try {
                fetchCover(entry)
            } catch (e: Exception) {
                ""
            }
            _covers.value = _covers.value + (key to resolved)
        }
    }

    private suspend fun fetchCover(entry: MetaDeckEntry): String {
        scryfall.lookupByName(entry.name)?.let {
            if (!it.typeLine.contains("Land", ignoreCase = true)) return ScryfallImage.artCrop(it.id)
        }
        val names = coverMutex.withLock {
            delay(400)
            MetaDecklistLoader.coverCardNames(entry.url)
        }
        var firstHit: String? = null
        for (name in names) {
            val card = scryfall.lookupByName(name) ?: continue
            if (firstHit == null) firstHit = card.id
            if (!card.typeLine.contains("Land", ignoreCase = true)) return ScryfallImage.artCrop(card.id)
        }
        return firstHit?.let { ScryfallImage.artCrop(it) } ?: ""
    }
}

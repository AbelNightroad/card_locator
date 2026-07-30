package com.gitlab.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.MetaDeckCard
import com.gitlab.abelnightroad.data.MetaDecklistLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

class MetaViewModel : ViewModel() {

    private val _state = MutableStateFlow<MetaState>(MetaState.Idle)
    val state: StateFlow<MetaState> = _state

    private val _decklistState = MutableStateFlow<DecklistState?>(null)
    val decklistState: StateFlow<DecklistState?> = _decklistState

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
}

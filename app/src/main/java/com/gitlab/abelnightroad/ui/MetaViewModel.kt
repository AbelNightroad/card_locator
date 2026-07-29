package com.gitlab.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.MetaDeckCard
import com.gitlab.abelnightroad.data.MetaDecklistLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.io.IOException

data class MetaDeckEntry(
    val rank: Int,
    val name: String,
    val coverImageUrl: String,
    val metaPercentage: String,
    val winRate: String,
    val cost: String,
    val tournaments: String,
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
                val doc = Jsoup.connect("https://www.mtggoldfish.com/metagame/${format.lowercase()}#paper")
                    .userAgent("MtGCardTracker/1.0").get()
                val container = doc.selectFirst("#metagame-decks-container")
                    ?: throw IOException("Could not find metagame data on page")
                val decks = mutableListOf<MetaDeckEntry>()
                val tiles = container.select(".archetype-tile")
                for ((i, tile) in tiles.withIndex()) {
                    val titleEl = tile.selectFirst(".archetype-tile-title a")
                    val deckName = titleEl?.text()?.trim() ?: continue
                    if (deckName.isBlank()) continue
                    val deckUrl = titleEl?.attr("href") ?: ""
                    val imgEl = tile.selectFirst(".card-image-tile")
                    val imgSrc = imgEl?.attr("style")?.let { style ->
                        val regex = Regex("""url\s*\(\s*['"]?\s*(.*?)\s*['"]?\s*\)""", RegexOption.IGNORE_CASE)
                        regex.find(style)?.groupValues?.get(1)?.trimEnd('\'')
                    } ?: ""
                    val metaEl = tile.selectFirst(".metagame-percentage .archetype-tile-statistic-value")
                    val metaPct = metaEl?.ownText()?.trim() ?: ""
                    val priceEl = tile.selectFirst(".deck-price-paper .archetype-tile-statistic-value")
                    val cost = priceEl?.wholeText()?.trim() ?: ""
                    decks.add(MetaDeckEntry(i + 1, deckName, imgSrc, metaPct, "", cost, "", deckUrl))
                }
                if (decks.isEmpty()) {
                    _state.value = MetaState.Error("No metagame data found for $format")
                } else {
                    val sorted = decks.sortedByDescending {
                        it.metaPercentage.removeSuffix("%").toDoubleOrNull() ?: 0.0
                    }.mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }
                    _state.value = MetaState.Success(format, sorted)
                }
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

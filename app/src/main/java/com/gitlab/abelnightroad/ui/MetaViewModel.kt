package com.gitlab.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

data class MetaDeckEntry(
    val rank: Int,
    val name: String,
    val metaPercentage: String,
    val winRate: String,
    val tournaments: String
)

sealed interface MetaState {
    data object Idle : MetaState
    data object Loading : MetaState
    data class Success(val format: String, val decks: List<MetaDeckEntry>) : MetaState
    data class Error(val message: String) : MetaState
}

class MetaViewModel : ViewModel() {

    private val _state = MutableStateFlow<MetaState>(MetaState.Idle)
    val state: StateFlow<MetaState> = _state

    fun loadFormat(format: String) {
        _state.value = MetaState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect("https://www.mtggoldfish.com/metagame/${format.lowercase()}#paper").get()
                val decks = mutableListOf<MetaDeckEntry>()
                val tables = doc.select("table")
                for (table in tables) {
                    val headers = table.select("thead th").map { it.text().trim() }
                    val deckIdx = headers.indexOfFirst { it.contains("Deck", ignoreCase = true) }
                    val metaIdx = headers.indexOfFirst { it.contains("Meta", ignoreCase = true) }
                    val winIdx = headers.indexOfFirst { it.contains("Win", ignoreCase = true) }
                    val eventIdx = headers.indexOfFirst { it.contains("Event", ignoreCase = true) || it.contains("Tournament", ignoreCase = true) || it.contains("Match", ignoreCase = true) }
                    val rankIdx = headers.indexOfFirst { it == "#" || it.contains("Rank", ignoreCase = true) }

                    if (deckIdx == -1) continue

                    val rows = table.select("tbody tr")
                    for ((i, row) in rows.withIndex()) {
                        val cols = row.select("td")
                        val rank = if (rankIdx != -1 && rankIdx < cols.size)
                            cols[rankIdx].text().trim().toIntOrNull() ?: (i + 1)
                        else i + 1
                        val deckName = if (deckIdx < cols.size) {
                            val link = cols[deckIdx].select("a").first()
                            link?.text()?.trim() ?: cols[deckIdx].text().trim()
                        } else continue
                        val metaPct = if (metaIdx != -1 && metaIdx < cols.size) cols[metaIdx].text().trim() else ""
                        val winRate = if (winIdx != -1 && winIdx < cols.size) cols[winIdx].text().trim() else ""
                        val events = if (eventIdx != -1 && eventIdx < cols.size) cols[eventIdx].text().trim() else ""

                        if (deckName.isNotBlank()) {
                            decks.add(MetaDeckEntry(rank, deckName, metaPct, winRate, events))
                        }
                    }
                    if (decks.isNotEmpty()) break
                }

                if (decks.isEmpty()) {
                    _state.value = MetaState.Error("No metagame data found for $format")
                } else {
                    _state.value = MetaState.Success(format, decks)
                }
            } catch (e: Exception) {
                _state.value = MetaState.Error(e.message ?: "Failed to load metagame data")
            }
        }
    }
}

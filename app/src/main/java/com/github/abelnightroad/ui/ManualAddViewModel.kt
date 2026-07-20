package com.github.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.abelnightroad.data.CardRepository
import com.github.abelnightroad.data.ScryfallRepository
import com.github.abelnightroad.db.CardEntity
import com.github.abelnightroad.db.ScryfallCardEntity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ManualAddViewModel(
    private val repository: CardRepository,
    private val scryfall: ScryfallRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /** Autocomplete suggestions sourced live from the Scryfall reference table. */
    val suggestions: Flow<List<ScryfallCardEntity>> = _query
        .debounce(150)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { q -> if (q.length >= 2) scryfall.autocomplete(q) else kotlinx.coroutines.flow.flowOf(emptyList()) }

    private val _selected = MutableStateFlow<ScryfallCardEntity?>(null)
    val selected: StateFlow<ScryfallCardEntity?> = _selected

    val quantity = MutableStateFlow("1")
    val foil = MutableStateFlow("")
    val condition = MutableStateFlow("Near Mint")
    val tag = MutableStateFlow("")

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun setQuery(value: String) {
        _query.value = value
        val sel = _selected.value
        if (sel != null && value != sel.name) _selected.value = null
    }

    fun select(card: ScryfallCardEntity) {
        _selected.value = card
        _query.value = card.name
    }

    fun save() {
        val card = _selected.value ?: return
        val qty = quantity.value.toIntOrNull()?.coerceAtLeast(1) ?: 1
        viewModelScope.launch {
            repository.addCard(
                CardEntity(
                    name = card.name,
                    setCode = card.setCode,
                    setName = card.setName,
                    collectorNumber = card.collectorNumber,
                    foil = foil.value.ifBlank { "normal" },
                    rarity = card.rarity,
                    quantity = qty,
                    manaBoxId = "",
                    scryfallId = card.id,
                    purchasePrice = 0.0,
                    misprint = false,
                    altered = false,
                    condition = condition.value.ifBlank { "Near Mint" },
                    language = "en",
                    purchasePriceCurrency = "USD",
                    added = LocalDateTime.now().toString(),
                    tag = tag.value.ifBlank { "Manual" }
                )
            )
            _saved.value = true
        }
    }
}

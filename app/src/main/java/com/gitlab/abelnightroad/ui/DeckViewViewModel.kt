package com.gitlab.abelnightroad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.db.DeckCardEntity
import com.gitlab.abelnightroad.db.ScryfallCardEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckViewViewModel(
    private val deckRepository: DeckRepository,
    private val scryfall: ScryfallRepository,
    private val deckId: Long
) : ViewModel() {

    val deckWithCards = deckRepository.getDeckWithCards(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateCardQuantity(cardId: Long, newQuantity: Int) {
        viewModelScope.launch {
            deckRepository.updateCardQuantity(cardId, newQuantity)
        }
    }

    fun removeCard(cardId: Long) {
        viewModelScope.launch {
            deckRepository.removeCard(cardId)
        }
    }

    fun setCover(scryfallId: String) {
        viewModelScope.launch {
            deckRepository.updateDeckCover(deckId, scryfallId)
        }
    }

    fun addCardToDeck(
        card: ScryfallCardEntity,
        slot: String,
        hasCommander: Boolean,
        hasCompanion: Boolean
    ) {
        viewModelScope.launch {
            if (slot == "commander" && hasCommander) {
                deckRepository.removeCardsBySlot(deckId, "commander")
            }
            if (slot == "companion" && hasCompanion) {
                deckRepository.removeCardsBySlot(deckId, "companion")
            }
            deckRepository.addCardToDeck(
                deckId = deckId,
                scryfallId = card.id,
                cardName = card.name,
                setCode = card.setCode,
                setName = card.setName,
                collectorNumber = card.collectorNumber,
                rarity = card.rarity,
                manaCost = card.manaCost,
                typeLine = card.typeLine,
                slot = slot
            )
        }
    }

    fun autocomplete(query: String) = scryfall.autocomplete(query)
}

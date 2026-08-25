package com.gitlab.abelnightroad.ui

import android.content.Context
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

    data class RemovedCard(val card: DeckCardEntity, val position: Int)
    private val _removedCard = MutableStateFlow<RemovedCard?>(null)
    val removedCard: StateFlow<RemovedCard?> = _removedCard

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

    fun removeCardWithUndo(cardId: Long, cards: List<DeckCardEntity>) {
        val index = cards.indexOfFirst { it.id == cardId }
        val card = cards.find { it.id == cardId } ?: return
        _removedCard.value = RemovedCard(card, index)
        removeCard(cardId)
    }

    fun undoRemove() {
        val removed = _removedCard.value ?: return
        viewModelScope.launch {
            deckRepository.addCardToDeck(
                deckId = deckId,
                scryfallId = removed.card.scryfallId,
                cardName = removed.card.cardName,
                setCode = removed.card.setCode,
                setName = removed.card.setName,
                collectorNumber = removed.card.collectorNumber,
                rarity = removed.card.rarity,
                quantity = removed.card.quantity,
                manaCost = removed.card.manaCost,
                typeLine = removed.card.typeLine,
                slot = removed.card.slot,
                condition = removed.card.condition,
                priceUsd = removed.card.priceUsd
            )
        }
        _removedCard.value = null
    }

    fun clearRemovedCard() { _removedCard.value = null }

    fun setCover(scryfallId: String) {
        viewModelScope.launch {
            deckRepository.updateDeckCover(deckId, scryfallId)
        }
    }

    fun addCardToDeck(
        card: ScryfallCardEntity,
        slot: String,
        hasCommander: Boolean,
        hasCompanion: Boolean,
        condition: String = "NM"
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
                slot = slot,
                condition = condition,
                priceUsd = card.priceUsd ?: 0.0
            )
        }
    }

    fun exportDeck(context: Context, deckName: String, cards: List<DeckCardEntity>) {
        val content = exportDeckToTxt(cards)
        shareDeckFile(context, deckName, content)
    }

    fun autocomplete(query: String) = scryfall.autocomplete(query)
}

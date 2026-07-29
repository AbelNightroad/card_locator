package com.gitlab.abelnightroad.data

import android.content.Context
import com.gitlab.abelnightroad.db.AppDatabase
import com.gitlab.abelnightroad.db.DeckDao
import com.gitlab.abelnightroad.db.DeckEntity
import com.gitlab.abelnightroad.db.DeckCardEntity
import com.gitlab.abelnightroad.db.DeckWithCards
import com.gitlab.abelnightroad.db.FormatCount
import com.gitlab.abelnightroad.db.SlotCount
import com.gitlab.abelnightroad.db.ScryfallCardDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DeckRepository(private val dao: DeckDao, private val scryfallDao: ScryfallCardDao) {

    fun allDecks(): Flow<List<DeckEntity>> = dao.allDecks()

    fun decksByFormat(format: String): Flow<List<DeckEntity>> = dao.decksByFormat(format)

    fun formatCounts(): Flow<List<FormatCount>> = dao.formatCounts()

    fun getDeckWithCards(deckId: Long): Flow<DeckWithCards?> = dao.getDeckWithCards(deckId)

    suspend fun createDeck(name: String, format: String, source: String = "manual"): Long {
        val deck = DeckEntity(name = name, format = format, source = source)
        return dao.insertDeck(deck)
    }

    suspend fun deleteDeck(deck: DeckEntity) = dao.deleteDeck(deck)

    suspend fun addCardToDeck(deckId: Long, scryfallId: String, cardName: String, setCode: String, setName: String, collectorNumber: String, rarity: String, quantity: Int = 1, manaCost: String = "", typeLine: String = "", slot: String = "mainboard"): Long {
        val ci = withContext(Dispatchers.IO) {
            scryfallDao.byId(scryfallId)?.colorIdentity ?: ""
        }
        val card = DeckCardEntity(
            deckId = deckId,
            scryfallId = scryfallId,
            cardName = cardName,
            setCode = setCode,
            setName = setName,
            collectorNumber = collectorNumber,
            rarity = rarity,
            quantity = quantity,
            manaCost = manaCost,
            typeLine = typeLine,
            slot = slot,
            colorIdentity = ci
        )
        return dao.insertCard(card)
    }

    suspend fun updateDeckCover(deckId: Long, scryfallId: String?) = dao.updateDeckCover(deckId, scryfallId)

    suspend fun removeCard(cardId: Long) = dao.deleteCard(cardId)

    suspend fun updateCardQuantity(cardId: Long, quantity: Int) = dao.updateCardQuantity(cardId, quantity)

    suspend fun deleteAllCards(deckId: Long) = dao.deleteAllCards(deckId)

    suspend fun removeCardsBySlot(deckId: Long, slot: String) = dao.deleteCardsBySlot(deckId, slot)

    suspend fun deleteDecksByFormat(format: String) = dao.deleteDecksByFormat(format)

    suspend fun cloneDeck(deckId: Long): Long {
        val dwc = dao.getDeckWithCards(deckId).first()
            ?: throw Exception("Deck not found")
        val newId = dao.insertDeck(
            dwc.deck.copy(id = 0, name = "${dwc.deck.name} (copy)", createdAt = System.currentTimeMillis())
        )
        for (card in dwc.cards) {
            dao.insertCard(card.copy(id = 0, deckId = newId))
        }
        return newId
    }

    suspend fun validateDeck(deckId: Long): ValidationResult {
        val dwc = dao.getDeckWithCards(deckId).first() ?: return ValidationResult.Invalid(listOf("Deck not found"))
        val validator = FormatValidator.forFormat(dwc.deck.format)
        return validator.validate(dwc.deck.format, dwc.cards)
    }

    suspend fun cardCount(deckId: Long): Int = dao.cardCount(deckId)

    fun cardCountFlow(deckId: Long): Flow<Int> = dao.cardCountFlow(deckId)

    companion object {
        fun create(context: Context): DeckRepository {
            val db = AppDatabaseProvider.get(context)
            return DeckRepository(db.deckDao(), db.scryfallCardDao())
        }

        fun isColorIdentityValid(cardColors: String, commanderColors: String): Boolean {
            if (commanderColors.isBlank()) return true
            if (cardColors.isBlank()) return true
            val cardSet = cardColors.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            val cmdSet = commanderColors.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            return cardSet.all { it in cmdSet }
        }
    }
}

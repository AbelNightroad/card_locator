package com.gitlab.abelnightroad.data

import android.content.Context
import com.gitlab.abelnightroad.db.AppDatabase
import com.gitlab.abelnightroad.db.DeckDao
import com.gitlab.abelnightroad.db.DeckEntity
import com.gitlab.abelnightroad.db.DeckCardEntity
import com.gitlab.abelnightroad.db.DeckWithCards
import kotlinx.coroutines.flow.Flow

class DeckRepository(private val dao: DeckDao) {

    fun allDecks(): Flow<List<DeckEntity>> = dao.allDecks()

    fun decksByFormat(format: String): Flow<List<DeckEntity>> = dao.decksByFormat(format)

    fun getDeckWithCards(deckId: Long): Flow<DeckWithCards?> = dao.getDeckWithCards(deckId)

    suspend fun createDeck(name: String, format: String, source: String = "manual"): Long {
        val deck = DeckEntity(name = name, format = format, source = source)
        return dao.insertDeck(deck)
    }

    suspend fun deleteDeck(deck: DeckEntity) = dao.deleteDeck(deck)

    suspend fun addCardToDeck(deckId: Long, scryfallId: String, cardName: String, setCode: String, setName: String, collectorNumber: String, rarity: String, quantity: Int = 1, manaCost: String = "", typeLine: String = ""): Long {
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
            typeLine = typeLine
        )
        return dao.insertCard(card)
    }

    suspend fun removeCard(cardId: Long) = dao.deleteCard(cardId)

    suspend fun updateCardQuantity(cardId: Long, quantity: Int) = dao.updateCardQuantity(cardId, quantity)

    suspend fun deleteAllCards(deckId: Long) = dao.deleteAllCards(deckId)

    suspend fun cardCount(deckId: Long): Int = dao.cardCount(deckId)

    fun cardCountFlow(deckId: Long): Flow<Int> = dao.cardCountFlow(deckId)

    companion object {
        fun create(context: Context): DeckRepository {
            val db = AppDatabaseProvider.get(context)
            return DeckRepository(db.deckDao())
        }
    }
}

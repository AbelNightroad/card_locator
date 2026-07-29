package com.gitlab.abelnightroad.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DeckDao {

    @Insert
    suspend fun insertDeck(deck: DeckEntity): Long

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE ASC")
    fun allDecks(): kotlinx.coroutines.flow.Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE format = :format ORDER BY name COLLATE NOCASE ASC")
    fun decksByFormat(format: String): kotlinx.coroutines.flow.Flow<List<DeckEntity>>

    @Transaction
    @Query("SELECT * FROM decks WHERE id = :deckId")
    fun getDeckWithCards(deckId: Long): kotlinx.coroutines.flow.Flow<DeckWithCards?>

    @Insert
    suspend fun insertCard(card: DeckCardEntity): Long

    @Query("DELETE FROM deck_cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Long)

    @Query("DELETE FROM deck_cards WHERE deck_id = :deckId")
    suspend fun deleteAllCards(deckId: Long)

    @Query("UPDATE deck_cards SET quantity = :quantity WHERE id = :cardId")
    suspend fun updateCardQuantity(cardId: Long, quantity: Int)

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM deck_cards WHERE deck_id = :deckId")
    suspend fun cardCount(deckId: Long): Int

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM deck_cards WHERE deck_id = :deckId")
    fun cardCountFlow(deckId: Long): kotlinx.coroutines.flow.Flow<Int>
}

data class DeckWithCards(
    @Embedded val deck: DeckEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "deck_id"
    )
    val cards: List<DeckCardEntity>
)
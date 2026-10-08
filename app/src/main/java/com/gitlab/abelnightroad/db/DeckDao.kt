package com.gitlab.abelnightroad.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

data class FormatCount(
    val format: String,
    val deckCount: Int
)

data class SlotCount(
    val slot: String,
    val cnt: Int
)

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

    @Query("UPDATE decks SET cover_scryfall_id = :scryfallId WHERE id = :deckId")
    suspend fun updateDeckCover(deckId: Long, scryfallId: String?)

    @Query("UPDATE decks SET name = :name WHERE id = :deckId")
    suspend fun renameDeck(deckId: Long, name: String)

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM deck_cards WHERE deck_id = :deckId")
    suspend fun cardCount(deckId: Long): Int

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM deck_cards WHERE deck_id = :deckId")
    fun cardCountFlow(deckId: Long): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT slot, COUNT(*) AS cnt FROM deck_cards WHERE deck_id = :deckId GROUP BY slot")
    fun slotCounts(deckId: Long): kotlinx.coroutines.flow.Flow<List<SlotCount>>

    @Query("SELECT format, CAST(COUNT(*) AS INTEGER) AS deckCount FROM decks GROUP BY format HAVING COUNT(*) > 0 ORDER BY format COLLATE NOCASE ASC")
    fun formatCounts(): kotlinx.coroutines.flow.Flow<List<FormatCount>>

    @Query("DELETE FROM deck_cards WHERE deck_id = :deckId AND slot = :slot")
    suspend fun deleteCardsBySlot(deckId: Long, slot: String)

    @Query("SELECT * FROM deck_cards WHERE deck_id = :deckId AND slot = :slot LIMIT 1")
    suspend fun findCardBySlot(deckId: Long, slot: String): DeckCardEntity?

    @Query("SELECT * FROM deck_cards WHERE deck_id = :deckId AND slot = 'commander' ORDER BY id LIMIT 1")
    suspend fun firstCommander(deckId: Long): DeckCardEntity?

    @Query("SELECT * FROM deck_cards WHERE deck_id = :deckId AND slot = 'mainboard' AND type_line NOT LIKE '%Land%' ORDER BY RANDOM() LIMIT 1")
    suspend fun randomNonLandMain(deckId: Long): DeckCardEntity?

    @Query("SELECT * FROM deck_cards WHERE deck_id = :deckId")
    suspend fun getCardsForDeck(deckId: Long): List<DeckCardEntity>

    @Query("DELETE FROM decks WHERE format = :format")
    suspend fun deleteDecksByFormat(format: String)
}

data class DeckWithCards(
    @Embedded val deck: DeckEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "deck_id"
    )
    val cards: List<DeckCardEntity>
)
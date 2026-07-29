package com.gitlab.abelnightroad.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScryfallCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<ScryfallCardEntity>)

    @Query("DELETE FROM scryfall_cards")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM scryfall_cards")
    suspend fun count(): Int

    /** Prefix autocomplete for the manual card-add screen. */
    @Query(
        "SELECT id, name, set_code, set_name, collector_number, rarity, " +
            "mana_cost, type_line, oracle_text, price_usd, color_identity FROM scryfall_cards " +
            "WHERE name LIKE :query || '%' COLLATE NOCASE " +
            "ORDER BY name COLLATE NOCASE ASC LIMIT :limit"
    )
    fun autocomplete(query: String, limit: Int = 50): Flow<List<ScryfallCardEntity>>

    @Query("SELECT * FROM scryfall_cards WHERE id = :id")
    suspend fun byId(id: String): ScryfallCardEntity?

    @Query("SELECT * FROM scryfall_cards WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun byName(name: String): ScryfallCardEntity?
}

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
            "mana_cost, type_line, oracle_text, price_usd, color_identity, image_url, " +
            "cmc, legalities, reserved, game_changer FROM scryfall_cards " +
            "WHERE name LIKE :query || '%' COLLATE NOCASE " +
            "ORDER BY name COLLATE NOCASE ASC LIMIT :limit"
    )
    fun autocomplete(query: String, limit: Int = 50): Flow<List<ScryfallCardEntity>>

    @Query("SELECT * FROM scryfall_cards WHERE id = :id")
    suspend fun byId(id: String): ScryfallCardEntity?

    @Query("SELECT * FROM scryfall_cards WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun byName(name: String): ScryfallCardEntity?

    @Query("SELECT * FROM scryfall_cards WHERE name LIKE :name || '%' COLLATE NOCASE LIMIT 1")
    suspend fun byNamePrefix(name: String): ScryfallCardEntity?

    /** Exact printing lookup (set codes are stored lowercase; compare case-insensitively). */
    @Query(
        "SELECT * FROM scryfall_cards WHERE set_code = :setCode COLLATE NOCASE " +
            "AND collector_number = :collectorNumber LIMIT 1"
    )
    suspend fun bySetAndCollector(setCode: String, collectorNumber: String): ScryfallCardEntity?

    /** Same set, any collector number — fallback when the exact printing is missing. */
    @Query(
        "SELECT * FROM scryfall_cards WHERE name = :name COLLATE NOCASE " +
            "AND set_code = :setCode COLLATE NOCASE LIMIT 1"
    )
    suspend fun byNameAndSet(name: String, setCode: String): ScryfallCardEntity?

    @Query("SELECT id, legalities FROM scryfall_cards WHERE id IN (:ids)")
    suspend fun getLegalities(ids: List<String>): List<ScryfallCardLegality>
}

data class ScryfallCardLegality(
    val id: String,
    val legalities: String
)

package com.gitlab.abelnightroad.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class TagCount(
    val tag: String,
    val cardCount: Long,
    val totalValue: Double = 0.0
)

data class CardSearchResult(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "set_code") val setCode: String,
    @ColumnInfo(name = "set_name") val setName: String,
    @ColumnInfo(name = "collector_number") val collectorNumber: String,
    val foil: String,
    val rarity: String,
    val quantity: Int,
    @ColumnInfo(name = "scryfall_id") val scryfallId: String,
    val tag: String
)

data class MultiCopyCard(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "set_code") val setCode: String,
    @ColumnInfo(name = "set_name") val setName: String,
    @ColumnInfo(name = "scryfall_id") val scryfallId: String,
    @ColumnInfo(name = "total_quantity") val totalQuantity: Int
)

@Dao
interface CardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CardEntity)

    @Query("UPDATE cards SET quantity = quantity + 1 WHERE id = :id")
    suspend fun incrementQuantity(id: Long)

    @Query("DELETE FROM cards WHERE id = :id AND quantity = 1")
    suspend fun deleteIfQuantityOne(id: Long)

    @Query("UPDATE cards SET quantity = quantity - 1 WHERE id = :id AND quantity > 1")
    suspend fun decrementQuantity(id: Long)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cards WHERE tag = :tag")
    suspend fun deleteByTag(tag: String)

    @Query("UPDATE cards SET tag = :newTag WHERE tag = :oldTag")
    suspend fun renameTag(oldTag: String, newTag: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Query("UPDATE cards SET quantity = quantity + :delta WHERE id = :id")
    suspend fun addQuantity(id: Long, delta: Int)

    /**
     * Candidate rows for duplicate detection: same tag, name, set, collector
     * number and finish. The caller compares the remaining fields, because two
     * rows that differ anywhere else are different cards.
     */
    @Query(
        "SELECT * FROM cards WHERE tag = :tag AND name = :name COLLATE NOCASE " +
            "AND set_code = :setCode COLLATE NOCASE AND collector_number = :collectorNumber " +
            "AND foil = :foil LIMIT 25"
    )
    suspend fun findByDuplicateKey(
        tag: String,
        name: String,
        setCode: String,
        collectorNumber: String,
        foil: String
    ): List<CardEntity>


    @Query("DELETE FROM cards")
    suspend fun clear()

    @Query("SELECT * FROM cards")
    suspend fun getAll(): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun count(): Int

    @Query(
        "SELECT tag, SUM(cardCount) AS cardCount, SUM(totalValue) AS totalValue FROM (" +
            "SELECT tag, 0 AS cardCount, 0.0 AS totalValue FROM tags " +
            "UNION ALL " +
            "SELECT tag, CAST(SUM(quantity) AS INTEGER) AS cardCount, SUM(quantity * purchase_price) AS totalValue FROM cards GROUP BY tag" +
        ") GROUP BY tag ORDER BY tag COLLATE NOCASE ASC"
    )
    fun tagCounts(): Flow<List<TagCount>>

    @Query(
        "SELECT id, name, set_code, set_name, collector_number, foil, rarity, " +
            "quantity, scryfall_id, tag FROM cards WHERE tag = :tag " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun cardsByTag(tag: String): Flow<List<CardSearchResult>>

    @Query(
        "SELECT id, name, set_code, set_name, collector_number, foil, rarity, " +
            "quantity, scryfall_id, tag FROM cards " +
            "WHERE name LIKE '%' || :query || '%' COLLATE NOCASE " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun searchByName(query: String): Flow<List<CardSearchResult>>

    @Query(
        "SELECT id, name, set_code, set_name, collector_number, foil, rarity, " +
            "quantity, scryfall_id, tag FROM cards " +
            "WHERE name LIKE '%' || :query || '%' COLLATE NOCASE " +
            "AND (:color IS NULL OR scryfall_id IN (SELECT id FROM scryfall_cards WHERE color_identity LIKE '%' || :color || '%')) " +
            "AND (:type IS NULL OR scryfall_id IN (SELECT id FROM scryfall_cards WHERE type_line LIKE '%' || :type || '%')) " +
            "AND (:rarity IS NULL OR rarity = :rarity) " +
            "ORDER BY name COLLATE NOCASE ASC"
    )
    fun searchAdvanced(query: String, color: String?, type: String?, rarity: String?): Flow<List<CardSearchResult>>

    @Transaction
    @Query(
        "SELECT name, set_code, set_name, scryfall_id, " +
            "SUM(quantity) AS total_quantity FROM cards " +
            "GROUP BY name, set_code HAVING SUM(quantity) > 4 " +
            "ORDER BY total_quantity DESC, name COLLATE NOCASE ASC"
    )
    fun multiCopyCards(): Flow<List<MultiCopyCard>>
}

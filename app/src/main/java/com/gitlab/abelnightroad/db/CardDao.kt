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
    val cardCount: Int
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Query("DELETE FROM cards")
    suspend fun clear()

    @Query("SELECT * FROM cards")
    suspend fun getAll(): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun count(): Int

    @Query(
        "SELECT tag, COUNT(*) AS cardCount FROM cards " +
            "GROUP BY tag ORDER BY tag COLLATE NOCASE ASC"
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

    @Transaction
    @Query(
        "SELECT name, set_code, set_name, scryfall_id, " +
            "SUM(quantity) AS total_quantity FROM cards " +
            "GROUP BY name, set_code HAVING SUM(quantity) > 4 " +
            "ORDER BY total_quantity DESC, name COLLATE NOCASE ASC"
    )
    fun multiCopyCards(): Flow<List<MultiCopyCard>>
}

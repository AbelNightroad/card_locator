package com.gitlab.abelnightroad.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reference card pulled from the Scryfall "Default Cards" bulk data file.
 * Used for autocomplete when a user adds a card manually. Keyed by Scryfall
 * [id] (UUID). Only the fields needed for lookup/display are stored.
 */
@Entity(
    tableName = "scryfall_cards",
    indices = [Index("name")]
)
data class ScryfallCardEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "set_code") val setCode: String,
    @ColumnInfo(name = "set_name") val setName: String,
    @ColumnInfo(name = "collector_number") val collectorNumber: String,
    @ColumnInfo(name = "rarity") val rarity: String,
    @ColumnInfo(name = "mana_cost") val manaCost: String,
    @ColumnInfo(name = "type_line") val typeLine: String,
    @ColumnInfo(name = "oracle_text") val oracleText: String,
    @ColumnInfo(name = "price_usd") val priceUsd: Double? = null
)

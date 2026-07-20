package com.github.abelnightroad.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single card copy stored in a Tag (storage location).
 * Card identity across the collection is [name] + [setCode]; this row is one
 * physical instance/quantity in a given [tag].
 */
@Entity(
    tableName = "cards",
    indices = [Index("tag"), Index("name", "set_code")]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "set_code") val setCode: String,
    @ColumnInfo(name = "set_name") val setName: String,
    @ColumnInfo(name = "collector_number") val collectorNumber: String,
    @ColumnInfo(name = "foil") val foil: String,
    @ColumnInfo(name = "rarity") val rarity: String,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "mana_box_id") val manaBoxId: String,
    @ColumnInfo(name = "scryfall_id") val scryfallId: String,
    @ColumnInfo(name = "purchase_price") val purchasePrice: Double,
    @ColumnInfo(name = "misprint") val misprint: Boolean,
    @ColumnInfo(name = "altered") val altered: Boolean,
    @ColumnInfo(name = "condition") val condition: String,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "purchase_price_currency") val purchasePriceCurrency: String,
    @ColumnInfo(name = "added") val added: String,
    @ColumnInfo(name = "tag") val tag: String
)

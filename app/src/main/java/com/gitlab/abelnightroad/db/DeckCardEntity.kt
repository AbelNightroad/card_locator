package com.gitlab.abelnightroad.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deck_cards",
    foreignKeys = [ForeignKey(
        entity = DeckEntity::class,
        parentColumns = ["id"],
        childColumns = ["deck_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("deck_id")]
)
data class DeckCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    @ColumnInfo(name = "scryfall_id") val scryfallId: String,
    @ColumnInfo(name = "card_name") val cardName: String,
    @ColumnInfo(name = "set_code") val setCode: String,
    @ColumnInfo(name = "set_name") val setName: String,
    @ColumnInfo(name = "collector_number") val collectorNumber: String,
    val rarity: String,
    val quantity: Int,
    @ColumnInfo(name = "mana_cost") val manaCost: String,
    @ColumnInfo(name = "type_line") val typeLine: String,
    val slot: String = "mainboard",
    @ColumnInfo(name = "color_identity") val colorIdentity: String = ""
)

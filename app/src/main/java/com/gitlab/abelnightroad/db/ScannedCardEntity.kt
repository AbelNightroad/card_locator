package com.gitlab.abelnightroad.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scanned_cards",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ScannedCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val setCode: String,
    val setName: String,
    val collectorNumber: String,
    val rarity: String,
    val manaCost: String,
    val typeLine: String,
    val oracleText: String,
    val colorIdentity: String,
    val scryfallId: String,
    val priceUsd: Double,
    val language: String
)

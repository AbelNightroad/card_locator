package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.CardEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupCard(
    val name: String,
    val setCode: String,
    val setName: String,
    val collectorNumber: String,
    val foil: String,
    val rarity: String,
    val quantity: Int,
    val manaBoxId: String,
    val scryfallId: String,
    val purchasePrice: Double,
    val misprint: Boolean,
    val altered: Boolean,
    val condition: String,
    val language: String,
    val purchasePriceCurrency: String,
    val added: String,
    val tag: String
)

@Serializable
data class BackupData(
    val version: Int = 1,
    val cards: List<BackupCard>
)

object BackupStore {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encode(cards: List<CardEntity>): String {
        val backup = BackupData(
            cards = cards.map { it.toBackup() }
        )
        return json.encodeToString(backup)
    }

    fun decode(text: String): List<CardEntity> {
        val data = json.decodeFromString<BackupData>(text)
        return data.cards.map { it.toEntity() }
    }

    private fun CardEntity.toBackup() = BackupCard(
        name = name, setCode = setCode, setName = setName,
        collectorNumber = collectorNumber, foil = foil, rarity = rarity,
        quantity = quantity, manaBoxId = manaBoxId, scryfallId = scryfallId,
        purchasePrice = purchasePrice, misprint = misprint, altered = altered,
        condition = condition, language = language,
        purchasePriceCurrency = purchasePriceCurrency, added = added, tag = tag
    )

    private fun BackupCard.toEntity() = CardEntity(
        name = name, setCode = setCode, setName = setName,
        collectorNumber = collectorNumber, foil = foil, rarity = rarity,
        quantity = quantity, manaBoxId = manaBoxId, scryfallId = scryfallId,
        purchasePrice = purchasePrice, misprint = misprint, altered = altered,
        condition = condition, language = language,
        purchasePriceCurrency = purchasePriceCurrency, added = added, tag = tag
    )
}

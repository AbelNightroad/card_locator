package com.github.abelnightroad.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape of a single object in Scryfall's "Default Cards" bulk JSON file
 * (https://scryfall.com/docs/api/bulk-data). Only the fields we persist are
 * modelled; unknown keys are ignored by the JSON decoder.
 */
@Serializable
data class ScryfallBulkCard(
    val id: String = "",
    val name: String = "",
    @SerialName("set") val setCode: String = "",
    @SerialName("set_name") val setName: String = "",
    @SerialName("collector_number") val collectorNumber: String = "",
    val rarity: String = "",
    @SerialName("mana_cost") val manaCost: String = "",
    @SerialName("type_line") val typeLine: String = "",
    @SerialName("oracle_text") val oracleText: String = ""
)

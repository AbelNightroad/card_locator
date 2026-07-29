package com.gitlab.abelnightroad.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape of a single object in Scryfall's "Default Cards" bulk JSON file
 * (https://scryfall.com/docs/api/bulk-data). Only the fields we persist are
 * modelled; unknown keys are ignored by the JSON decoder.
 */
@Serializable
data class ScryfallPrices(
    val usd: String? = null,
    @SerialName("usd_foil") val usdFoil: String? = null,
    @SerialName("usd_etched") val usdEtched: String? = null
)

@Serializable
data class ScryfallImageUris(
    val normal: String = ""
)

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
    @SerialName("oracle_text") val oracleText: String = "",
    @SerialName("color_identity") val colorIdentity: List<String> = emptyList(),
    @SerialName("image_uris") val imageUris: ScryfallImageUris? = null,
    val prices: ScryfallPrices = ScryfallPrices()
)

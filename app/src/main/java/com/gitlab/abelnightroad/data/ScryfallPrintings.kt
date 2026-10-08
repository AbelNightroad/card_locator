package com.gitlab.abelnightroad.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Serializable
data class PrintingInfo(
    @SerialName("set") val setCode: String,
    @SerialName("set_name") val setName: String,
    @SerialName("collector_number") val collectorNumber: String,
    @SerialName("rarity") val rarity: String,
    @SerialName("id") val id: String,
    @SerialName("released_at") val releasedAt: String = "",
    @SerialName("prices") val prices: PrintingPrices? = null
) {
    val priceUsd: Double? get() = prices?.usd?.toDoubleOrNull()
}

@Serializable
data class PrintingPrices(@SerialName("usd") val usd: String? = null)

/**
 * Alternate printings for a card from the live Scryfall API (the local
 * reference table keeps one printing per name). Pure JVM: URL building and
 * JSON parsing are separable from the HttpURLConnection fetch so they are
 * unit-testable against recorded fixtures.
 */
object ScryfallPrintings {

    private const val USER_AGENT = "MtGCardTracker/1.0"
    private const val BASE_URL = "https://api.scryfall.com/cards/search"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SearchResponse(@SerialName("data") val data: List<PrintingInfo>)

    /** Exact-name search: `q=!"Name"` returns every printing of that card. */
    fun printingsUrl(name: String): String {
        val query = URLEncoder.encode("!\"$name\"", "UTF-8")
        return "$BASE_URL?q=$query"
    }

    fun parsePrintings(jsonText: String): List<PrintingInfo> = try {
        json.decodeFromString<SearchResponse>(jsonText).data
            .sortedByDescending { it.releasedAt }
    } catch (e: Exception) {
        throw IllegalArgumentException("Malformed Scryfall printings response", e)
    }

    fun fetchPrintings(name: String): List<PrintingInfo> {
        val conn = (URL(printingsUrl(name)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = conn.responseCode
            when (code) {
                200 -> parsePrintings(conn.inputStream.bufferedReader().readText())
                429 -> throw IllegalStateException("Scryfall rate limit — try again in a moment")
                else -> throw IllegalStateException("Scryfall returned HTTP $code")
            }
        } finally {
            conn.disconnect()
        }
    }
}

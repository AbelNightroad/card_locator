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

/** One page of the printings search plus the next page URL when `has_more`. */
data class PrintingsPage(
    val printings: List<PrintingInfo>,
    val nextPage: String?
)

/**
 * Alternate printings for a card from the live Scryfall API (the local
 * reference table keeps one printing per name). Pure JVM: URL building and
 * JSON parsing are separable from the HttpURLConnection fetch so they are
 * unit-testable against recorded fixtures.
 */
object ScryfallPrintings {

    private const val USER_AGENT = "MtGCardTracker/1.0"
    private const val BASE_URL = "https://api.scryfall.com/cards/search"
    private const val MAX_PAGES = 5

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SearchResponse(
        @SerialName("data") val data: List<PrintingInfo>,
        @SerialName("has_more") val hasMore: Boolean = false,
        @SerialName("next_page") val nextPage: String? = null
    )

    /**
     * Exact-name search across every printing (`unique=prints` — the API
     * default `unique=cards` collapses to one result per oracle card),
     * ordered newest first.
     */
    fun printingsUrl(name: String): String {
        val query = URLEncoder.encode("!\"$name\"", "UTF-8")
        return "$BASE_URL?q=$query&unique=prints&order=released&dir=desc"
    }

    /** Parses one search page; [PrintingsPage.nextPage] is set only when more pages exist. */
    fun parsePage(jsonText: String): PrintingsPage = try {
        val resp = json.decodeFromString<SearchResponse>(jsonText)
        PrintingsPage(
            printings = resp.data.sortedByDescending { it.releasedAt },
            nextPage = if (resp.hasMore) resp.nextPage else null
        )
    } catch (e: Exception) {
        throw IllegalArgumentException("Malformed Scryfall printings response", e)
    }

    fun parsePrintings(jsonText: String): List<PrintingInfo> = parsePage(jsonText).printings

    fun fetchPrintings(name: String): List<PrintingInfo> {
        var nextUrl: String? = printingsUrl(name)
        val byId = LinkedHashMap<String, PrintingInfo>()
        var pages = 0
        while (nextUrl != null && pages < MAX_PAGES) {
            val page = fetchPage(nextUrl)
            page.printings.forEach { byId.putIfAbsent(it.id, it) }
            nextUrl = page.nextPage
            pages++
        }
        return byId.values.toList()
    }

    private fun fetchPage(url: String): PrintingsPage {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = conn.responseCode
            when (code) {
                200 -> parsePage(conn.inputStream.bufferedReader().readText())
                429 -> throw IllegalStateException("Scryfall rate limit — try again in a moment")
                else -> throw IllegalStateException("Scryfall returned HTTP $code")
            }
        } finally {
            conn.disconnect()
        }
    }
}

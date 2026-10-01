package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class EdhrecResponse(
    val deck: EdhrecDeck? = null
)

@Serializable
data class EdhrecDeck(
    @SerialName("commander_v2")
    val commander: List<List<kotlinx.serialization.json.JsonElement>> = emptyList(),
    val cards: Map<String, List<List<kotlinx.serialization.json.JsonElement>>> = emptyMap()
)

object EdhrecApiClient {

    private const val BASE_URL = "https://json.edhrec.com/pages/average-decks"
    private const val PREVIEW_URL = "https://edhrec.com/deckpreview"
    private const val USER_AGENT = "MtGCardTracker/1.0"

    private val json = Json { ignoreUnknownKeys = true }

    fun extractSlug(url: String): String? {
        val pattern = Regex("""edhrec\.com/average-decks/([a-z0-9\-]+)""", RegexOption.IGNORE_CASE)
        return pattern.find(url)?.groupValues?.get(1)
    }

    fun extractDeckPreviewId(url: String): String? {
        val pattern = Regex("""edhrec\.com/deckpreview/([a-zA-Z0-9\-_]+)""", RegexOption.IGNORE_CASE)
        return pattern.find(url)?.groupValues?.get(1)
    }

    suspend fun fetchFromUrl(url: String): EdhrecDeck {
        val previewId = extractDeckPreviewId(url)
        if (previewId != null) return fetchDeckPreview(previewId)
        val slug = extractSlug(url)
            ?: throw Exception(
                "Invalid EDHREC URL — expected: https://edhrec.com/average-decks/<slug> " +
                    "or https://edhrec.com/deckpreview/<id>"
            )
        return fetchAverageDeck(slug)
    }

    suspend fun fetchAverageDeck(slug: String): EdhrecDeck = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/$slug.json")
        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15000
            readTimeout = 15000
        }

        try {
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                throw Exception("EDHREC returned HTTP $responseCode — deck may be private/deleted")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val response = json.decodeFromString<EdhrecResponse>(body)
            response.deck ?: throw Exception("No deck data in EDHREC response")
        } finally {
            conn.disconnect()
        }
    }

    suspend fun fetchDeckPreview(deckId: String): EdhrecDeck = withContext(Dispatchers.IO) {
        val url = URL("$PREVIEW_URL/$deckId")
        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            connectTimeout = 15000
            readTimeout = 15000
        }

        try {
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                throw Exception("EDHREC returned HTTP $responseCode — deck may be private/deleted")
            }
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            parseNextDataDeck(html)
                ?: throw Exception("Could not find deck data in EDHREC page")
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseNextDataDeck(html: String): EdhrecDeck? {
        val match = Regex(
            """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""",
            RegexOption.DOT_MATCHES_ALL
        ).find(html) ?: return null
        return try {
            val root = json.parseToJsonElement(match.groupValues[1]).jsonObject
            val deck = root["props"]?.jsonObject
                ?.get("pageProps")?.jsonObject
                ?.get("data")?.jsonObject
                ?.get("deck") ?: return null
            json.decodeFromJsonElement<EdhrecDeck>(deck)
        } catch (_: Exception) {
            null
        }
    }

    fun toMetaDeckCards(deck: EdhrecDeck): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()

        for (entry in deck.commander) {
            if (entry.size >= 2) {
                val name = entry[0].toString().removeSurrounding("\"")
                val qty = entry[1].toString().toIntOrNull() ?: 1
                cards.add(MetaDeckCard(qty, name, "commander"))
            }
        }

        for ((_, categoryCards) in deck.cards) {
            for (entry in categoryCards) {
                if (entry.size >= 2) {
                    val name = entry[0].toString().removeSurrounding("\"")
                    val qty = entry[1].toString().toIntOrNull() ?: 1
                    cards.add(MetaDeckCard(qty, name, "mainboard"))
                }
            }
        }

        return cards
    }
}

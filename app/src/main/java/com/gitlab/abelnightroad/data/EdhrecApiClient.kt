package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class EdhrecResponse(
    val deck: EdhrecDeck? = null
)

@Serializable
data class EdhrecDeck(
    val commander: List<List<kotlinx.serialization.json.JsonElement>> = emptyList(),
    val cards: Map<String, List<List<kotlinx.serialization.json.JsonElement>>> = emptyMap()
)

object EdhrecApiClient {

    private const val BASE_URL = "https://json.edhrec.com/pages/average-decks"
    private const val USER_AGENT = "MtGCardTracker/1.0"

    private val json = Json { ignoreUnknownKeys = true }

    fun extractSlug(url: String): String? {
        val pattern = Regex("""edhrec\.com/average-decks/([a-z0-9\-]+)""", RegexOption.IGNORE_CASE)
        return pattern.find(url)?.groupValues?.get(1)
    }

    fun isEdhrecUrl(url: String): Boolean {
        return Regex("""edhrec\.com/average-decks/""", RegexOption.IGNORE_CASE).containsMatchIn(url)
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
                throw Exception("EDHREC API error: HTTP $responseCode")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val response = json.decodeFromString<EdhrecResponse>(body)
            response.deck ?: throw Exception("No deck data in EDHREC response")
        } finally {
            conn.disconnect()
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

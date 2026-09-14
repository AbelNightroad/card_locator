package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class ArchidektResponse(
    val name: String = "",
    val cards: List<ArchidektCard> = emptyList()
)

@Serializable
data class ArchidektCard(
    val quantity: Int = 1,
    val card: ArchidektCardData? = null
)

@Serializable
data class ArchidektCardData(
    val oracleCard: ArchidektOracleCard? = null
)

@Serializable
data class ArchidektOracleCard(
    val name: String = ""
)

object ArchidektApiClient {

    private const val BASE_URL = "https://archidekt.com/api/decks"
    private const val USER_AGENT = "MtGCardTracker/1.0"

    private val json = Json { ignoreUnknownKeys = true }

    fun extractDeckId(url: String): Long? {
        val pattern = Regex("""archidekt\.com/decks/(\d+)""", RegexOption.IGNORE_CASE)
        return pattern.find(url)?.groupValues?.get(1)?.toLongOrNull()
    }

    fun isArchidektUrl(url: String): Boolean {
        return Regex("""archidekt\.com/decks/\d+""", RegexOption.IGNORE_CASE).containsMatchIn(url)
    }

    suspend fun fetchDeck(deckId: Long): ArchidektResponse = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/$deckId/")
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
                throw Exception("Archidekt API error: HTTP $responseCode")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<ArchidektResponse>(body)
        } finally {
            conn.disconnect()
        }
    }

    fun toMetaDeckCards(deck: ArchidektResponse): List<MetaDeckCard> {
        return deck.cards.map { card ->
            val name = card.card?.oracleCard?.name ?: "Unknown"
            MetaDeckCard(card.quantity, name, "mainboard")
        }
    }
}

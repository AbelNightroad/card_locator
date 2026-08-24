package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class MoxfieldDeckResponse(
    val data: MoxfieldDeckData? = null
)

@Serializable
data class MoxfieldDeckData(
    val name: String = "",
    val format: String = "",
    val mainboard: Map<String, MoxfieldEntry> = emptyMap(),
    val sideboard: Map<String, MoxfieldEntry> = emptyMap(),
    val commanders: Map<String, MoxfieldEntry> = emptyMap(),
    val companions: Map<String, MoxfieldEntry> = emptyMap()
)

@Serializable
data class MoxfieldEntry(
    val quantity: Int = 1,
    val card: MoxfieldCard? = null
)

@Serializable
data class MoxfieldCard(
    val name: String = "",
    val set: String = ""
)

object MoxfieldApiClient {

    private const val BASE_URL = "https://api2.moxfield.com/v3/decks/all"
    private const val USER_AGENT = "MtGCardTracker/1.0"

    private val json = Json { ignoreUnknownKeys = true }

    fun extractDeckId(url: String): String? {
        val pattern = Regex("""moxfield\.com/decks/([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE)
        return pattern.find(url)?.groupValues?.get(1)
    }

    suspend fun fetchDeck(deckId: String): MoxfieldDeckData = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/$deckId")
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
                throw Exception("Moxfield API error: HTTP $responseCode")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val response = json.decodeFromString<MoxfieldDeckResponse>(body)
            response.data ?: throw Exception("Empty response from Moxfield")
        } finally {
            conn.disconnect()
        }
    }

    fun toMetaDeckCards(deck: MoxfieldDeckData): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()

        for ((_, entry) in deck.commanders) {
            val name = entry.card?.name ?: continue
            cards.add(MetaDeckCard(entry.quantity, name, "commander"))
        }

        for ((_, entry) in deck.companions) {
            val name = entry.card?.name ?: continue
            cards.add(MetaDeckCard(entry.quantity, name, "companion"))
        }

        for ((_, entry) in deck.mainboard) {
            val name = entry.card?.name ?: continue
            cards.add(MetaDeckCard(entry.quantity, name, "mainboard"))
        }

        for ((_, entry) in deck.sideboard) {
            val name = entry.card?.name ?: continue
            cards.add(MetaDeckCard(entry.quantity, name, "sideboard"))
        }

        return cards
    }
}

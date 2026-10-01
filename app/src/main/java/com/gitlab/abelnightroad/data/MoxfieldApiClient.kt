package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class MoxfieldDeckData(
    val name: String = "",
    val format: String = "",
    val boards: MoxfieldBoards = MoxfieldBoards()
)

@Serializable
data class MoxfieldBoards(
    val mainboard: MoxfieldBoard = MoxfieldBoard(),
    val sideboard: MoxfieldBoard = MoxfieldBoard(),
    val commanders: MoxfieldBoard = MoxfieldBoard(),
    val companions: MoxfieldBoard = MoxfieldBoard()
)

@Serializable
data class MoxfieldBoard(
    val cards: Map<String, MoxfieldEntry> = emptyMap()
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
        val pattern = Regex("""moxfield\.com/decks/([a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE)
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
                throw Exception("Moxfield returned HTTP $responseCode — deck may be private/deleted")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<MoxfieldDeckData>(body)
        } finally {
            conn.disconnect()
        }
    }

    fun toMetaDeckCards(deck: MoxfieldDeckData): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()

        fun addBoard(board: MoxfieldBoard, slot: String) {
            for ((_, entry) in board.cards) {
                val name = entry.card?.name ?: continue
                cards.add(MetaDeckCard(entry.quantity, name, slot))
            }
        }

        addBoard(deck.boards.commanders, "commander")
        addBoard(deck.boards.companions, "companion")
        addBoard(deck.boards.mainboard, "mainboard")
        addBoard(deck.boards.sideboard, "sideboard")

        return cards
    }
}

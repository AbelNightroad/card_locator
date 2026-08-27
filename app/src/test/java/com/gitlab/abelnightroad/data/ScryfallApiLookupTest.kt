package com.gitlab.abelnightroad.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class ScryfallApiLookupTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val userAgent = "MtGCardTracker/1.0"

    @Serializable
    private data class ScryfallCardResponse(
        @SerialName("name") val name: String = "",
        @SerialName("set") val setCode: String = "",
        @SerialName("set_name") val setName: String = "",
        @SerialName("collector_number") val collectorNumber: String = "",
        @SerialName("rarity") val rarity: String = "",
        @SerialName("mana_cost") val manaCost: String? = null,
        @SerialName("type_line") val typeLine: String = "",
        @SerialName("oracle_text") val oracleText: String? = null,
        @SerialName("color_identity") val colorIdentity: List<String> = emptyList(),
        @SerialName("id") val id: String = "",
        @SerialName("prices") val prices: Prices? = null
    )

    @Serializable
    private data class Prices(
        @SerialName("usd") val usd: String? = null
    )

    @Serializable
    private data class ScryfallListResponse(
        @SerialName("data") val data: List<ScryfallCardResponse> = emptyList(),
        @SerialName("total_cards") val totalCards: Int = 0
    )

    private fun fetchCard(name: String): ScryfallCardResponse? {
        val encoded = java.net.URLEncoder.encode(name, "UTF-8")
        val url = URL("https://api.scryfall.com/cards/named?fuzzy=$encoded")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            conn.connect()
            if (conn.responseCode == 200) {
                json.decodeFromString<ScryfallCardResponse>(conn.inputStream.bufferedReader().readText())
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun searchCards(query: String): ScryfallListResponse? {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = URL("https://api.scryfall.com/cards/search?q=$encoded")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            conn.connect()
            if (conn.responseCode == 200) {
                json.decodeFromString<ScryfallListResponse>(conn.inputStream.bufferedReader().readText())
            } else null
        } finally {
            conn.disconnect()
        }
    }

    @Test
    fun `lookup English card by exact name`() = runBlocking {
        val card = fetchCard("Lightning Bolt")
        assertNotNull("Card should be found", card)
        assertEquals("Lightning Bolt", card!!.name)
        assertTrue("Set code should not be blank", card.setCode.isNotBlank())
        assertTrue("Rarity should not be blank", card.rarity.isNotBlank())
        assertEquals("Type should be Instant", "Instant", card.typeLine)
        assertTrue("Mana cost should contain R", card.manaCost?.contains("{R}") == true)
    }

    @Test
    fun `lookup card returns color identity`() = runBlocking {
        val card = fetchCard("Sol Ring")
        assertNotNull("Card should be found", card)
        assertEquals("Sol Ring", card!!.name)
        assertTrue("Color identity should be empty for colorless", card.colorIdentity.isEmpty())
    }

    @Test
    fun `lookup card returns price`() = runBlocking {
        val card = fetchCard("Lightning Bolt")
        assertNotNull("Card should be found", card)
        assertNotNull("Price should exist", card!!.prices)
    }

    @Test
    fun `lookup card with special characters`() = runBlocking {
        val card = fetchCard("Jace, the Mind Sculptor")
        assertNotNull("Card should be found", card)
        assertEquals("Jace, the Mind Sculptor", card!!.name)
    }

    @Test
    fun `lookup returns null for fake card`() = runBlocking {
        val card = fetchCard("Xyzzy Fakecard That Does Not Exist 12345")
        assertEquals("Fake card should return null", null, card)
    }

    @Test
    fun `search returns multiple results for broad query`() = runBlocking {
        val results = searchCards("Lightning Bolt")
        assertNotNull("Results should not be null", results)
        assertTrue("Should find multiple Lightning Bolt printings", results!!.totalCards > 1)
    }

    @Test
    fun `lookup card has valid UUID format`() = runBlocking {
        val card = fetchCard("Lightning Bolt")
        assertNotNull("Card should be found", card)
        val id = card!!.id
        assertTrue("Scryfall ID should be a valid UUID format",
            id.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
    }
}

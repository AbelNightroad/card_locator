package com.gitlab.abelnightroad.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteUrlImportTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "missing fixture $name"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `moxfield id keeps underscores and dashes`() {
        assertEquals(
            "aSGf97rAHkKrxR_5OfihbA",
            MoxfieldApiClient.extractDeckId("https://www.moxfield.com/decks/aSGf97rAHkKrxR_5OfihbA")
        )
        assertEquals(
            "abc-123_x",
            MoxfieldApiClient.extractDeckId("https://moxfield.com/decks/abc-123_x")
        )
    }

    @Test
    fun `moxfield id stops at trailing deck name and query params`() {
        assertEquals(
            "aSGf97rAHkKrxR_5OfihbA",
            MoxfieldApiClient.extractDeckId(
                "https://www.moxfield.com/decks/aSGf97rAHkKrxR_5OfihbA/tom-bert-william?foo=1"
            )
        )
        assertEquals(
            "abc123",
            MoxfieldApiClient.extractDeckId("moxfield.com/decks/abc123")
        )
    }

    @Test
    fun `moxfield rejects non deck urls`() {
        assertNull(MoxfieldApiClient.extractDeckId("https://www.moxfield.com/about"))
        assertNull(MoxfieldApiClient.extractDeckId("not a url"))
    }

    @Test
    fun `moxfield v3 fixture decodes from response root with boards`() {
        val deck = json.decodeFromString<MoxfieldDeckData>(fixture("moxfield_deck.json"))
        assertEquals("Tom, Bert, and William - Brawl MTG Arena #01", deck.name)

        val cards = MoxfieldApiClient.toMetaDeckCards(deck)
        assertTrue("expected >= 90 cards, got ${cards.size}", cards.size >= 90)
        assertTrue(cards.any { it.cardName == "Rotting Regisaur" })
        assertTrue(cards.any { it.slot == "commander" })
        assertTrue(cards.all { it.quantity >= 1 })
    }

    @Test
    fun `edhrec average deck slug extracted`() {
        assertEquals(
            "kess-dissident-mage",
            EdhrecApiClient.extractSlug("https://edhrec.com/average-decks/kess-dissident-mage")
        )
        assertEquals(
            "kess-dissident-mage",
            EdhrecApiClient.extractSlug("https://www.edhrec.com/average-decks/kess-dissident-mage?theme=")
        )
        assertNull(EdhrecApiClient.extractSlug("https://edhrec.com/deckpreview/abc123"))
    }

    @Test
    fun `edhrec deckpreview id extracted for user link`() {
        assertEquals(
            "4Rm5X7VN7c_H1V33ojqsAw",
            EdhrecApiClient.extractDeckPreviewId("https://edhrec.com/deckpreview/4Rm5X7VN7c_H1V33ojqsAw")
        )
        assertEquals(
            "4Rm5X7VN7c_H1V33ojqsAw",
            EdhrecApiClient.extractDeckPreviewId(
                "https://www.edhrec.com/deckpreview/4Rm5X7VN7c_H1V33ojqsAw/average-name"
            )
        )
        assertNull(EdhrecApiClient.extractDeckPreviewId("https://edhrec.com/average-decks/kess"))
    }

    @Test
    fun `edhrec deckpreview html fixture decodes next data deck`() {
        val deck = EdhrecApiClient.parseNextDataDeck(fixture("edhrec_deckpreview.html"))
        assertNotNull("expected __NEXT_DATA__ deck", deck)

        val cards = EdhrecApiClient.toMetaDeckCards(deck!!)
        assertTrue("expected deck cards, got ${cards.size}", cards.size > 20)
        assertTrue(cards.any { it.slot == "commander" && it.cardName.contains("Golbez") })
        assertTrue(cards.any { it.cardName == "Academy Ruins" })
    }

    @Test
    fun `edhrec commander_v2 pairs decode while commander strings are ignored`() {
        val deck = json.decodeFromString<EdhrecDeck>(
            """
            {
              "commander": ["Golbez, Crystal Collector"],
              "commander_v2": [["Golbez, Crystal Collector", 1]],
              "cards": {"Land": [["Island", 1]]}
            }
            """.trimIndent()
        )
        assertEquals(1, deck.commander.size)
        assertEquals(1, deck.cards.size)

        val cards = EdhrecApiClient.toMetaDeckCards(deck)
        assertTrue(cards.any { it.slot == "commander" && it.cardName == "Golbez, Crystal Collector" })
        assertTrue(cards.any { it.slot == "mainboard" && it.cardName == "Island" })
    }

    @Test
    fun `goldfish id extracted from user link variants`() {
        assertEquals(
            "2016013",
            MtgGoldfishApiClient.extractDeckId("https://www.mtggoldfish.com/deck/2016013")
        )
        assertEquals(
            "2016013",
            MtgGoldfishApiClient.extractDeckId("https://mtggoldfish.com/deck/2016013/some-deck-name")
        )
        assertEquals(
            "2016013",
            MtgGoldfishApiClient.extractDeckId("http://www.mtggoldfish.com/deck/2016013?utm_source=x")
        )
        assertNull(MtgGoldfishApiClient.extractDeckId("https://www.mtggoldfish.com/"))
    }

    @Test
    fun `goldfish decklist textarea extracted and parsed`() {
        val text = MtgGoldfishApiClient.extractDecklist(fixture("goldfish_decklist.html"))
        assertNotNull(text)

        val cards = UniversalDecklistParser.parse(text!!)
        assertTrue(cards.any { it.cardName == "Forest" && it.quantity == 4 })
        assertTrue(cards.any { it.cardName == "Teferi's Protection" })
        assertTrue(cards.any { it.slot == "sideboard" && it.cardName == "Sol Ring" })
    }

    @Test
    fun `cloudflare challenge page detected`() {
        assertTrue(MtgGoldfishApiClient.isChallengePage(fixture("goldfish_blocked.html")))
        assertFalse(MtgGoldfishApiClient.isChallengePage(fixture("goldfish_decklist.html")))
    }
}

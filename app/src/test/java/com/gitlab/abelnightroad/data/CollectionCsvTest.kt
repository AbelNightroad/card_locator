package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.CardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class CollectionCsvTest {

    private fun card(
        name: String = "Lightning Bolt",
        tag: String = "Box-A1",
        quantity: Int = 4
    ) = CardEntity(
        name = name,
        setCode = "LEA",
        setName = "Limited Edition Alpha",
        collectorNumber = "161",
        foil = "normal",
        rarity = "common",
        quantity = quantity,
        manaBoxId = "",
        scryfallId = "9f76f9c1-4e3b-4b7e-9f6b-1a2b3c4d5e6f",
        purchasePrice = 1.25,
        misprint = false,
        altered = true,
        condition = "NM",
        language = "English",
        purchasePriceCurrency = "USD",
        added = "2026-01-01",
        tag = tag
    )

    @Test
    fun `full field round trip preserves every column`() {
        val cards = listOf(
            card(),
            card(name = "Sol Ring", tag = "Binder-02", quantity = 2).copy(
                foil = "etched", rarity = "uncommon", purchasePrice = 0.0,
                misprint = true, altered = false, condition = "HP",
                language = "Japanese", added = "2025-12-31", manaBoxId = "MB-99",
                scryfallId = "", setName = "", collectorNumber = "", setCode = ""
            )
        )
        assertEquals(cards, CollectionCsv.parse(CollectionCsv.encode(cards)))
    }

    @Test
    fun `names with commas quotes and apostrophes round trip`() {
        val cards = listOf(
            card(name = "Jyoti, Moag Ancient"),
            card(name = "Teferi's Protection"),
            card(name = "Say \"Cheese\""),
            card(name = "Forest, Full Art \"Signed\"")
        )
        assertEquals(cards, CollectionCsv.parse(CollectionCsv.encode(cards)))
    }

    @Test
    fun `empty tag round trips`() {
        val cards = listOf(card(tag = ""))
        assertEquals(cards, CollectionCsv.parse(CollectionCsv.encode(cards)))
    }

    @Test
    fun `crlf rows parse`() {
        val text = CollectionCsv.encode(listOf(card()))
        val lfOnly = text.replace("\r\n", "\n")
        assertEquals(
            CollectionCsv.parse(lfOnly),
            CollectionCsv.parse(text.replace("\n", "\r\n"))
        )
    }

    @Test
    fun `wrong header is rejected`() {
        try {
            CollectionCsv.parse("name,tag\r\nLightning Bolt,Box-A1\r\n")
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun `empty input is rejected`() {
        try {
            CollectionCsv.parse("")
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
        }
    }
}

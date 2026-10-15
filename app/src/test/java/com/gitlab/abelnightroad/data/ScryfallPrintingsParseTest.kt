package com.gitlab.abelnightroad.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScryfallPrintingsParseTest {

    private fun fixture(): String =
        javaClass.getResourceAsStream("/fixtures/printings.json")!!
            .bufferedReader().readText()

    @Test
    fun `entries are sorted newest release first`() {
        val printings = ScryfallPrintings.parsePrintings(fixture())
        assertEquals(3, printings.size)
        assertEquals("2x2", printings[0].setCode)
        assertEquals("mh2", printings[1].setCode)
        assertEquals("leb", printings[2].setCode)
    }

    @Test
    fun `fields are parsed exactly`() {
        val mh2 = ScryfallPrintings.parsePrintings(fixture())[1]
        assertEquals("mh2", mh2.setCode)
        assertEquals("Modern Horizons 2", mh2.setName)
        assertEquals("242", mh2.collectorNumber)
        assertEquals("common", mh2.rarity)
        assertEquals("9f0e1a2b-1111-2222-3333-444455556666", mh2.id)
        assertEquals("2021-06-11", mh2.releasedAt)
        assertEquals(0.25, mh2.priceUsd!!, 0.0)
    }

    @Test
    fun `missing prices yield null price`() {
        val leb = ScryfallPrintings.parsePrintings(fixture())[2]
        assertNull(leb.priceUsd)
    }

    @Test
    fun `malformed json throws explicit error`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ScryfallPrintings.parsePrintings("not json at all")
        }
        assertTrue(error.message!!.contains("Malformed"))
    }

    @Test
    fun `json without data field throws explicit error`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScryfallPrintings.parsePrintings("{}")
        }
    }

    @Test
    fun `printings url encodes exact-name query with unique=prints`() {
        assertEquals(
            "https://api.scryfall.com/cards/search?q=%21%22Lightning+Bolt%22" +
                "&unique=prints&order=released&dir=desc",
            ScryfallPrintings.printingsUrl("Lightning Bolt")
        )
    }

    @Test
    fun `has_more page exposes next_page url`() {
        val page = ScryfallPrintings.parsePage(
            javaClass.getResourceAsStream("/fixtures/printings_has_more.json")!!
                .bufferedReader().readText()
        )
        assertEquals(2, page.printings.size)
        assertEquals(
            "https://api.scryfall.com/cards/search?q=abc&page=2",
            page.nextPage
        )
    }

    @Test
    fun `last page yields no next_page`() {
        val page = ScryfallPrintings.parsePage(fixture())
        assertNull(page.nextPage)
    }
}

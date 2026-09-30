package com.gitlab.abelnightroad.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThirdPartyImportTest {

    private val manaboxHeader =
        "Name,Set code,Set name,Collector number,Foil,Rarity,Quantity,ManaBox ID," +
            "Scryfall ID,Purchase price,Misprint,Altered,Condition,Language," +
            "Purchase price currency,Added"

    @Test
    fun `quantity list is not detected as csv`() {
        val text = """
            1 Jyoti, Moag Ancient (M3C) 8 *F*
            1 Dread Return (CMM) 153
            2 Many Partings (LTR) 176
        """.trimIndent()
        assertFalse(ThirdPartyImport.looksLikeManaBoxCsv(text))
    }

    @Test
    fun `manabox header row is detected as csv`() {
        assertTrue(ThirdPartyImport.looksLikeManaBoxCsv(manaboxHeader))
    }

    @Test
    fun `headerless sixteen column row is detected as csv`() {
        val row = (1..16).joinToString(",") { "v$it" }
        assertTrue(ThirdPartyImport.looksLikeManaBoxCsv(row))
    }

    @Test
    fun `narrow comma row stays on the quantity list path`() {
        assertFalse(ThirdPartyImport.looksLikeManaBoxCsv("1 Forest, full art, 5"))
    }

    @Test
    fun `empty input is not detected as csv`() {
        assertFalse(ThirdPartyImport.looksLikeManaBoxCsv(""))
        assertFalse(ThirdPartyImport.looksLikeManaBoxCsv("\n\n   \n"))
    }

    @Test
    fun `quantity list with commas in card names parses cleanly`() {
        val text = "1 Jyoti, Moag Ancient (M3C) 8 *F*\n1 Omo, Queen of Vesuva (MH3) 214"
        val result = QtyListImport.parse(text, "Test")
        assertEquals(0, result.skipped)
        assertEquals(listOf("Jyoti, Moag Ancient", "Omo, Queen of Vesuva"), result.cards.map { it.name })
    }
}

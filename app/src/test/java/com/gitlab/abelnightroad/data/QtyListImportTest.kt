package com.gitlab.abelnightroad.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QtyListImportTest {

    @Test
    fun `parses a plain row`() {
        val result = QtyListImport.parse("1 Dread Return (CMM) 153", "Test")
        assertEquals(0, result.skipped)
        val card = result.cards.single()
        assertEquals(1, card.quantity)
        assertEquals("Dread Return", card.name)
        assertEquals("CMM", card.setCode)
        assertEquals("153", card.collectorNumber)
        assertEquals("normal", card.foil)
        assertEquals("Test", card.tag)
    }

    @Test
    fun `parses foil marker`() {
        val card = QtyListImport.parse("1 Rancor (2X2) 156 *F*", "T").cards.single()
        assertEquals("foil", card.foil)
    }

    @Test
    fun `parses etched marker`() {
        val card = QtyListImport.parse("1 Livaan, Cultist of Tiamat (CLB) 508 *E*", "T").cards.single()
        assertEquals("etched", card.foil)
    }

    @Test
    fun `parses etched foil marker both orders`() {
        assertEquals("etched foil", QtyListImport.parse("1 Krenko (SLD) 12 *FE*", "T").cards.single().foil)
        assertEquals("etched foil", QtyListImport.parse("1 Krenko (SLD) 12 *EF*", "T").cards.single().foil)
    }

    @Test
    fun `merges duplicate rows summing quantity`() {
        val result = QtyListImport.parse(
            "2 Forest (BLB) 280\n3 Forest (BLB) 280", "T"
        )
        assertEquals(1, result.cards.size)
        assertEquals(5, result.cards[0].quantity)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `does not merge rows with different finish`() {
        val result = QtyListImport.parse(
            "2 Forest (BLB) 280\n1 Forest (BLB) 280 *F*", "T"
        )
        assertEquals(2, result.cards.size)
        assertEquals(2, result.cards[0].quantity)
        assertEquals(1, result.cards[1].quantity)
    }

    @Test
    fun `merge is case-insensitive on name and set`() {
        val result = QtyListImport.parse(
            "1 rancor (2x2) 156\n1 Rancor (2X2) 156", "T"
        )
        assertEquals(1, result.cards.size)
        assertEquals(2, result.cards[0].quantity)
        assertEquals("rancor", result.cards[0].name)
    }

    @Test
    fun `keeps card name with commas and apostrophes`() {
        val card = QtyListImport.parse("6 Man-o'-War (DMR) 58", "T").cards.single()
        assertEquals("Man-o'-War", card.name)
        assertEquals(6, card.quantity)
    }

    @Test
    fun `keeps split card name`() {
        val card = QtyListImport.parse(
            "1 Invasion of Ulgrotha // Grandmother Ravi Sengir (MOM) 116", "T"
        ).cards.single()
        assertEquals("Invasion of Ulgrotha // Grandmother Ravi Sengir", card.name)
        assertEquals("MOM", card.setCode)
        assertEquals("116", card.collectorNumber)
    }

    @Test
    fun `keeps same-set art variants as separate rows`() {
        val result = QtyListImport.parse(
            "1 Goggles of Night (CLB) 74 *F*\n1 Goggles of Night (CLB) 384 *F*", "T"
        )
        assertEquals(2, result.cards.size)
        assertEquals("74", result.cards[0].collectorNumber)
        assertEquals("384", result.cards[1].collectorNumber)
    }

    @Test
    fun `keeps foil and non-foil rows separate`() {
        val result = QtyListImport.parse(
            "1 Momentary Blink (DMR) 15 *F*\n1 Momentary Blink (DMR) 15", "T"
        )
        assertEquals(2, result.cards.size)
        assertEquals("foil", result.cards[0].foil)
        assertEquals("normal", result.cards[1].foil)
    }

    @Test
    fun `parses multiple copies`() {
        val card = QtyListImport.parse("4 Heart of Kiran (AER) 153", "T").cards.single()
        assertEquals(4, card.quantity)
    }

    @Test
    fun `handles CRLF line endings`() {
        val result = QtyListImport.parse("1 Opt (OTC) 104\r\n1 Negate (MOM) 68\r\n", "T")
        assertEquals(0, result.skipped)
        assertEquals(2, result.cards.size)
    }

    @Test
    fun `ignores blank lines`() {
        val result = QtyListImport.parse("1 Opt (OTC) 104\n\n   \n", "T")
        assertEquals(0, result.skipped)
        assertEquals(1, result.cards.size)
    }

    @Test
    fun `skips malformed line with reason`() {
        val result = QtyListImport.parse("this is not a card line", "T")
        assertEquals(1, result.skipped)
        assertEquals("this is not a card line", result.skippedRows[0].row)
        assertTrue(result.skippedRows[0].reason.contains("Expected"))
    }

    @Test
    fun `skips unknown finish marker`() {
        val result = QtyListImport.parse("1 Opt (OTC) 104 *XYZ*", "T")
        assertEquals(1, result.skipped)
        assertTrue(result.skippedRows[0].reason.contains("Unknown finish marker"))
    }

    @Test
    fun `parses real sample rows`() {
        val text = """
            1 Jyoti, Moag Ancient (M3C) 8 *F*
            1 Triskaidekaphobia (INR) 136
            2 Magus of the Candelabra (M3C) 236
            1 Sea Hag // Aquatic Ingress (CLB) 95
        """.trimIndent()
        val result = QtyListImport.parse(text, "Box-1")
        assertEquals(0, result.skipped)
        assertEquals(4, result.cards.size)
        assertEquals("Jyoti, Moag Ancient", result.cards[0].name)
        assertEquals("foil", result.cards[0].foil)
        assertEquals("M3C", result.cards[0].setCode)
        assertEquals(1, result.cards[1].quantity)
        assertEquals(2, result.cards[2].quantity)
        assertEquals("Box-1", result.cards[3].tag)
    }
}

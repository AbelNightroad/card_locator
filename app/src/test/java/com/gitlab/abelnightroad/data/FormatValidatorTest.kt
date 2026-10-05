package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.DeckCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatValidatorTest {

    private fun card(
        name: String,
        slot: String = "mainboard",
        colorIdentity: String = "",
        legalities: String = ""
    ) = DeckCardEntity(
        deckId = 1,
        scryfallId = "id-$name",
        cardName = name,
        setCode = "TST",
        setName = "Test",
        collectorNumber = "1",
        rarity = "rare",
        quantity = 1,
        manaCost = "",
        typeLine = "Creature",
        slot = slot,
        colorIdentity = colorIdentity
    )

    private val legalities = mapOf("id-Sol Ring" to """{"commander":"legal"}""")

    private fun validate(cards: List<DeckCardEntity>) =
        FormatValidator.forFormat("Commander").validate("Commander", cards, legalities)

    @Test
    fun `commander deck without a commander is rejected`() {
        val result = validate(listOf(card("Sol Ring")))

        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.first().contains("Missing commander"))
    }

    @Test
    fun `single commander deck with cards inside the identity is valid`() {
        val result = validate(
            listOf(
                card("Tymna the Weaver", slot = "commander", colorIdentity = "W,B"),
                card("Sol Ring", colorIdentity = "")
            )
        )

        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `partner pair validates against the union of both commanders`() {
        val result = validate(
            listOf(
                card("Kraum, Ludevic's Opus", slot = "commander", colorIdentity = "R,U"),
                card("Tymna the Weaver", slot = "commander", colorIdentity = "W,B"),
                card("Swords to Plowshares", colorIdentity = "W"),
                card("Lightning Bolt", colorIdentity = "R")
            )
        )

        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `partner pair still rejects cards outside the union`() {
        val result = validate(
            listOf(
                card("Kraum, Ludevic's Opus", slot = "commander", colorIdentity = "R,U"),
                card("Tymna the Weaver", slot = "commander", colorIdentity = "W,B"),
                card("Llanowar Elves", colorIdentity = "G")
            )
        )

        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.single().contains("Llanowar Elves"))
    }

    @Test
    fun `more than two commanders is rejected`() {
        val result = validate(
            listOf(
                card("Kraum", slot = "commander", colorIdentity = "R,U"),
                card("Tymna", slot = "commander", colorIdentity = "W,B"),
                card("Thrasios", slot = "commander", colorIdentity = "U,G")
            )
        )

        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errors.first().contains("Too many commanders"))
    }

    @Test
    fun `mergeColorIdentities unions and sorts identity codes`() {
        assertEquals("B,W", DeckRepository.mergeColorIdentities("", "B,W"))
        assertEquals("R,U", DeckRepository.mergeColorIdentities("U,R", "R"))
        assertEquals("G,U,W", DeckRepository.mergeColorIdentities("W,U", "G"))
        assertEquals("", DeckRepository.mergeColorIdentities("", ""))
    }
}

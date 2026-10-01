package com.gitlab.abelnightroad.ui

import com.gitlab.abelnightroad.db.DeckCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckStatsTest {

    private fun card(
        name: String,
        typeLine: String,
        manaCost: String = "",
        quantity: Int = 1,
        slot: String = "mainboard",
        rarity: String = "common"
    ) = DeckCardEntity(
        deckId = 1,
        scryfallId = "id-$name",
        cardName = name,
        setCode = "TST",
        setName = "Test Set",
        collectorNumber = "1",
        rarity = rarity,
        quantity = quantity,
        manaCost = manaCost,
        typeLine = typeLine,
        slot = slot
    )

    private fun DeckStats.allPercentages(): List<Float> =
        typeStats.map { it.percentage } +
            rarityStats.map { it.percentage } +
            colorStats.map { it.percentage } +
            listOf(avgCmc)

    private fun assertNoNaN(stats: DeckStats) {
        for (value in stats.allPercentages()) {
            assertTrue("expected finite value, got $value", value.isFinite())
        }
    }

    @Test
    fun `empty card list returns zeroed stats`() {
        val stats = computeDeckStats(emptyList())
        assertEquals(0, stats.totalCards)
        assertEquals(0f, stats.avgCmc)
        assertTrue(stats.typeStats.isEmpty())
        assertTrue(stats.rarityStats.isEmpty())
        assertTrue(stats.colorStats.isEmpty())
        assertNoNaN(stats)
    }

    @Test
    fun `sideboard-only deck does not divide by zero`() {
        val stats = computeDeckStats(
            listOf(card("Negate", "Instant", "{1}{U}", slot = "sideboard"))
        )
        assertEquals(0, stats.totalCards)
        assertTrue(stats.typeStats.isEmpty())
        assertNoNaN(stats)
    }

    @Test
    fun `all-land deck reports lands without NaN average`() {
        val stats = computeDeckStats(
            listOf(card("Forest", "Basic Land — Forest", quantity = 24))
        )
        assertEquals(24, stats.totalCards)
        assertEquals(24, stats.landCount)
        assertEquals(0f, stats.avgCmc)
        assertEquals(1, stats.typeStats.size)
        assertEquals("Land", stats.typeStats[0].type)
        assertEquals(100f, stats.typeStats[0].percentage)
        assertNoNaN(stats)
    }

    @Test
    fun `typical deck computes finite distributions`() {
        val stats = computeDeckStats(
            listOf(
                card("Sol Ring", "Artifact", "{1}", quantity = 1, rarity = "uncommon"),
                card("Llanowar Elves", "Creature — Elf Druid", "{G}", quantity = 4),
                card("Lightning Bolt", "Instant", "{R}", quantity = 4, rarity = "common"),
                card("Mountain", "Basic Land — Mountain", quantity = 10),
                card("Jace", "Planeswalker — Jace", "{3}{U}{U}", quantity = 1, rarity = "mythic"),
                card("Sideboard Card", "Sorcery", "{2}", slot = "sideboard")
            )
        )
        assertEquals(20, stats.totalCards)
        assertEquals(10, stats.landCount)
        assertEquals(4, stats.creatureCount)
        assertTrue(stats.avgCmc > 0f)
        assertNoNaN(stats)

        val typeTotal = stats.typeStats.sumOf { it.count }
        assertEquals(20, typeTotal)
        assertTrue(stats.typeStats.all { it.percentage in 0f..100f })

        assertEquals(
            listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9+"),
            stats.cmcDistribution.map { it.label }
        )
        assertTrue(stats.colorStats.isNotEmpty())
    }

    @Test
    fun `mana cost pips drive color distribution`() {
        val stats = computeDeckStats(
            listOf(card("Counterspell", "Instant", "{U}{U}", quantity = 2))
        )
        assertEquals(1, stats.colorStats.size)
        assertEquals("U", stats.colorStats[0].color)
        assertEquals(100f, stats.colorStats[0].percentage)
        assertEquals(2f, stats.avgCmc)
        assertNoNaN(stats)
    }

    @Test
    fun `cmc above eight buckets into nine plus`() {
        val stats = computeDeckStats(
            listOf(card("Emrakul", "Creature — Eldrazi", "{15}", quantity = 1))
        )
        assertEquals(1, stats.cmcDistribution[9].count)
        assertEquals("9+", stats.cmcDistribution[9].label)
        assertTrue(stats.allPercentages().all { it.isFinite() })
    }
}

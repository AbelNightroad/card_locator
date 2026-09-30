package com.gitlab.abelnightroad.ui

import com.gitlab.abelnightroad.data.primaryType
import com.gitlab.abelnightroad.db.DeckCardEntity

data class TypeStat(val type: String, val count: Int, val percentage: Float)
data class RarityStat(val rarity: String, val count: Int, val percentage: Float)
data class ColorStat(val color: String, val count: Int, val percentage: Float)
data class CmcStat(val cmc: Int, val count: Int)

data class DeckStats(
    val totalCards: Int = 0,
    val avgCmc: Float = 0f,
    val typeStats: List<TypeStat> = emptyList(),
    val rarityStats: List<RarityStat> = emptyList(),
    val colorStats: List<ColorStat> = emptyList(),
    val cmcDistribution: List<CmcStat> = emptyList(),
    val landCount: Int = 0,
    val creatureCount: Int = 0
)

private val pipRegex = Regex("\\{([WUBRG])}")
private val numRegex = Regex("\\{(\\d+)\\}")

/**
 * Computes deck statistics from the deck's cards. Pure function (no ViewModel
 * allocation in composition). All percentage divisions are guarded so a deck
 * with an empty mainboard produces `0f` instead of `NaN`/`Infinity` reaching
 * the chart composables.
 */
internal fun computeDeckStats(cards: List<DeckCardEntity>): DeckStats {
    if (cards.isEmpty()) return DeckStats()

    val mainboard = cards.filter {
        it.slot == "mainboard" || it.slot == "commander" || it.slot == "companion"
    }
    val totalCards = mainboard.sumOf { it.quantity }

    var totalCmc = 0.0
    var nonLandCards = 0
    val colorPips = mutableMapOf("W" to 0, "U" to 0, "B" to 0, "R" to 0, "G" to 0)
    val typeCounts = mutableMapOf<String, Int>()
    val rarityCounts = mutableMapOf<String, Int>()
    val cmcCounts = mutableMapOf<Int, Int>()
    var landCount = 0
    var creatureCount = 0

    for (card in mainboard) {
        val qty = card.quantity
        val type = primaryType(card.typeLine)
        val rarity = card.rarity

        typeCounts[type] = (typeCounts[type] ?: 0) + qty
        rarityCounts[rarity] = (rarityCounts[rarity] ?: 0) + qty

        if (type == "Land") {
            landCount += qty
            continue
        }

        creatureCount += if (type == "Creature") qty else 0

        val cmc = parseCmc(card.manaCost)
        totalCmc += cmc * qty
        nonLandCards += qty

        val cmcBucket = cmc.toInt().coerceIn(0, 10)
        cmcCounts[cmcBucket] = (cmcCounts[cmcBucket] ?: 0) + qty

        pipRegex.findAll(card.manaCost).forEach { match ->
            val color = match.groupValues[1]
            colorPips[color] = (colorPips[color] ?: 0) + qty
        }
    }

    val avgCmc = if (nonLandCards > 0) (totalCmc / nonLandCards).toFloat() else 0f
    val totalForPct = totalCards.takeIf { it > 0 } ?: 1

    val typeStats = typeCounts.entries
        .sortedByDescending { it.value }
        .map { TypeStat(it.key, it.value, it.value * 100f / totalForPct) }

    val rarityStats = rarityCounts.entries
        .sortedByDescending { it.value }
        .map { RarityStat(it.key, it.value, it.value * 100f / totalForPct) }

    val totalPips = colorPips.values.sum().coerceAtLeast(1)
    val colorStats = colorPips.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
        .map { ColorStat(it.key, it.value, it.value * 100f / totalPips) }

    val cmcDistribution = (0..10).map { i -> CmcStat(i, cmcCounts[i] ?: 0) }

    return DeckStats(
        totalCards = totalCards,
        avgCmc = avgCmc,
        typeStats = typeStats,
        rarityStats = rarityStats,
        colorStats = colorStats,
        cmcDistribution = cmcDistribution,
        landCount = landCount,
        creatureCount = creatureCount
    )
}

private fun parseCmc(manaCost: String): Double {
    var cmc = 0.0
    numRegex.findAll(manaCost).forEach { cmc += it.groupValues[1].toDoubleOrNull() ?: 0.0 }
    cmc += pipRegex.findAll(manaCost).count()
    return cmc
}

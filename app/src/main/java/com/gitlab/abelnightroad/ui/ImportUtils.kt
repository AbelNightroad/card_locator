package com.gitlab.abelnightroad.ui

import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.MetaDeckCard
import com.gitlab.abelnightroad.data.ScryfallRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal val ALL_FORMATS = listOf(
    "Standard", "Modern", "Pioneer", "Pauper", "Legacy", "Vintage",
    "Premodern", "Commander"
)

internal val DECK_FORMATS = listOf(
    "All", "Standard", "Modern", "Pioneer", "Pauper", "Legacy", "Vintage",
    "Premodern", "Commander"
)

internal val FORMATS = DECK_FORMATS

internal suspend fun importDeckCards(
    deckId: Long,
    cards: List<MetaDeckCard>,
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    format: String = "Commander",
    onProgress: suspend (String) -> Unit = {}
) {
    val isCommanderFormat = format == "Commander"
    val resolved = coroutineScope {
        cards.map { c ->
            async {
                val scryfallCard = scryfall.lookupByNameResilient(c.cardName)
                c to scryfallCard
            }
        }.awaitAll()
    }

    val declaredCommander = cards.any { it.slot == "commander" }
    val slots = cards.mapIndexed { i, card ->
        if (isCommanderFormat && !declaredCommander && i == 0) "commander" else card.slot
    }

    val commanderColors = if (!isCommanderFormat) "" else {
        resolved.filterIndexed { i, _ -> slots[i] == "commander" }
            .mapNotNull { it.second?.colorIdentity }
            .filter { it.isNotBlank() }
            .fold("") { acc, colors -> DeckRepository.mergeColorIdentities(acc, colors) }
    }

    for ((i, pair) in resolved.withIndex()) {
        val (c, scryfallCard) = pair
        val slot = slots[i]
        if (scryfallCard != null) {
            if (isCommanderFormat && slot != "commander" && commanderColors.isNotBlank()
                && !DeckRepository.isColorIdentityValid(scryfallCard.colorIdentity, commanderColors)) {
                continue
            }
            deckRepository.addCardToDeck(
                deckId = deckId,
                scryfallId = scryfallCard.id,
                cardName = scryfallCard.name,
                setCode = scryfallCard.setCode,
                setName = scryfallCard.setName,
                collectorNumber = scryfallCard.collectorNumber,
                rarity = scryfallCard.rarity,
                quantity = c.quantity,
                manaCost = scryfallCard.manaCost,
                typeLine = scryfallCard.typeLine,
                slot = slot
            )
        } else {
            deckRepository.addCardToDeck(
                deckId = deckId,
                scryfallId = "",
                cardName = c.cardName,
                setCode = "", setName = "",
                collectorNumber = "", rarity = "",
                quantity = c.quantity, slot = slot
            )
        }
    }
}

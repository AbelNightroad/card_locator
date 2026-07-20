package com.github.abelnightroad.data

/**
 * Builds a Scryfall card image URL from a card's Scryfall UUID.
 * Reference: https://scryfall.com/docs/api/images
 */
object ScryfallImage {
    fun normal(scryfallId: String): String =
        "https://cards.scryfall.io/normal/front/${scryfallId[0]}/${scryfallId[1]}/$scryfallId.jpg"

    fun large(scryfallId: String): String =
        "https://cards.scryfall.io/large/front/${scryfallId[0]}/${scryfallId[1]}/$scryfallId.jpg"
}

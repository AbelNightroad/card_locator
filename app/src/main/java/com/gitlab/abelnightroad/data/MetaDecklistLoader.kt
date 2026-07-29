package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

data class MetaDeckCard(
    val quantity: Int,
    val cardName: String,
    val slot: String
)

object MetaDecklistLoader {

    private const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private fun connect(url: String) = Jsoup.connect(url)
        .userAgent(USER_AGENT)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Referer", "https://www.mtggoldfish.com/")
        .header("DNT", "1")
        .header("Connection", "keep-alive")
        .header("Sec-Fetch-Dest", "document")
        .header("Sec-Fetch-Mode", "navigate")
        .header("Sec-Fetch-Site", "same-origin")
        .header("Sec-Fetch-User", "?1")
        .header("Upgrade-Insecure-Requests", "1")
        .timeout(20000)

    suspend fun load(archetypeUrl: String): List<MetaDeckCard> = withContext(Dispatchers.IO) {
        val deckUrl = findFirstDeckUrl(archetypeUrl)
            ?: throw IOException("No decks found on archetype page")
        parseDeckPage(deckUrl)
    }

    private fun findFirstDeckUrl(archetypeUrl: String): String? {
        val fullUrl = if (archetypeUrl.startsWith("http")) archetypeUrl
            else "https://www.mtggoldfish.com$archetypeUrl"
        val doc = connect(fullUrl).get()
        val link = doc.selectFirst("a[href^=\"/deck/\"]") ?: return null
        return "https://www.mtggoldfish.com${link.attr("href")}"
    }

    private fun parseDeckPage(deckUrl: String): List<MetaDeckCard> {
        val doc = connect(deckUrl).get()
        val cards = mutableListOf<MetaDeckCard>()

        val table = doc.selectFirst(".deck-view-deck-table")
        if (table != null) {
            var currentSlot = "mainboard"
            for (row in table.select("tr")) {
                val header = row.selectFirst(".deck-category-header")
                if (header != null) {
                    val text = header.text()
                    currentSlot = if (text.contains("Sideboard", ignoreCase = true)) "sideboard" else "mainboard"
                    continue
                }
                val qtyEl = row.selectFirst(".deck-card-number, .card-qty, td:eq(0)")
                val nameEl = row.selectFirst(".deck-card-name, a:has(.card-name), td:eq(1) a")
                if (nameEl != null) {
                    val qty = qtyEl?.text()?.trim()?.toIntOrNull() ?: 1
                    val name = nameEl.text().trim()
                    cards.add(MetaDeckCard(qty, name, currentSlot))
                }
            }
        } else {
            val entries = doc.select(".card-entry, .deck-card")
            for (entry in entries) {
                val qtyEl = entry.selectFirst(".qty, .card-count")
                val nameEl = entry.selectFirst(".name, .card-name")
                if (nameEl != null) {
                    val qty = qtyEl?.text()?.trim()?.toIntOrNull() ?: 1
                    val name = nameEl.text().trim()
                    cards.add(MetaDeckCard(qty, name, "mainboard"))
                }
            }
        }

        if (cards.isEmpty()) throw IOException("Could not parse decklist from page")
        return cards
    }
}

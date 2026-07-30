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

data class MTGTop8Archetype(
    val name: String,
    val coverUrl: String,
    val metaPercent: String,
    val archetypeId: Int,
    val url: String
)

object MetaDecklistLoader {

    private const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val formatCodes = mapOf(
        "standard" to "ST",
        "pioneer" to "PI",
        "modern" to "MO",
        "legacy" to "LE",
        "vintage" to "VI",
        "pauper" to "PAU",
        "premodern" to "PREM",
        "commander" to "cEDH"
    )

    private fun connect(url: String) = Jsoup.connect(url)
        .userAgent(USER_AGENT)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Referer", "https://mtgtop8.com/")
        .timeout(20000)

    suspend fun loadFormat(format: String): List<MTGTop8Archetype> = withContext(Dispatchers.IO) {
        val code = formatCodes[format.lowercase()] ?: throw IOException("Unsupported format: $format")
        val doc = connect("https://mtgtop8.com/format?f=$code").get()

        val leftPanel = doc.selectFirst("td[width=40%]")
            ?: throw IOException("Could not find metagame data on page")

        val entries = leftPanel.select("div.hover_tr, div.chosen_tr")
        if (entries.isEmpty()) throw IOException("No archetypes found for $format")

        entries.mapNotNull { entry ->
            val link = entry.selectFirst("div.S14 a")
            val name = link?.text()?.trim() ?: return@mapNotNull null
            if (name.isBlank()) return@mapNotNull null

            val href = link.attr("href")
            val archetypeId = Regex("""a=(\d+)""").find(href)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null

            val thumb = entry.selectFirst("img[src*=/metas_thumbs/]")
            val coverUrl = thumb?.attr("src")?.let { "https://mtgtop8.com/$it" } ?: ""

            val allS14 = entry.select("div.S14")
            val metaPct = allS14.firstOrNull { it.selectFirst("a") == null }?.text()?.trim() ?: ""

            MTGTop8Archetype(name, coverUrl, metaPct, archetypeId, href)
        }
    }

    suspend fun load(archetypeUrl: String): List<MetaDeckCard> = withContext(Dispatchers.IO) {
        val fullUrl = if (archetypeUrl.startsWith("http")) archetypeUrl
            else "https://mtgtop8.com/$archetypeUrl"

        val archetypeDoc = connect(fullUrl).get()
        val firstDeckLink = archetypeDoc.selectFirst("tr.hover_tr a[href*='/event?'], tr.chosen_tr a[href*='/event?']")
            ?: throw IOException("No decks found on archetype page")
        val deckPath = firstDeckLink.attr("href")
        val deckUrl = if (deckPath.startsWith("http")) deckPath else "https://mtgtop8.com/$deckPath"

        val deckDoc = connect(deckUrl).get()
        val decklistContainer = deckDoc.selectFirst("div[style*='display:flex'][style*='align-content:stretch']")
            ?: deckDoc.selectFirst("div[style*='display: flex'][style*='align-content: stretch']")
            ?: throw IOException("Could not find decklist on event page")

        val cards = mutableListOf<MetaDeckCard>()

        val sideboardHeader = decklistContainer.selectFirst("div.O14:contains(SIDEBOARD)")
        val maindeckDivs = decklistContainer.select("div[id^=\"md\"].deck_line")
        val sideboardDivs = decklistContainer.select("div[id^=\"sb\"].deck_line")

        for (div in maindeckDivs) {
            val qty = div.ownText().trim().toIntOrNull() ?: 1
            val name = div.selectFirst("span")?.text()?.trim() ?: continue
            if (name.isBlank()) continue
            cards.add(MetaDeckCard(qty, name, "mainboard"))
        }

        for (div in sideboardDivs) {
            val qty = div.ownText().trim().toIntOrNull() ?: 1
            val name = div.selectFirst("span")?.text()?.trim() ?: continue
            if (name.isBlank()) continue
            cards.add(MetaDeckCard(qty, name, "sideboard"))
        }

        if (cards.isEmpty()) throw IOException("Could not parse decklist from page")
        cards
    }
}

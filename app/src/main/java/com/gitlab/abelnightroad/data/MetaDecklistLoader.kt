package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException

import kotlinx.serialization.Serializable

@Serializable
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

    private const val BASE_URL = "https://mtgtop8.com"

    private const val ARCHETYPE_ROWS =
        "div.hover_tr:has(div.S14 a[href*=archetype]), div.chosen_tr:has(div.S14 a[href*=archetype])"

    private val xhrRequest = Regex("""RequestContent\("(\w+_decks)\?""")
    private val pageMeta = Regex("""(?m)^\s*meta=(\d+);\s*$""")
    private val archetypeId = Regex("""a=(\d+)""")

    private val slotOrder = mapOf("commander" to 0, "mainboard" to 1, "sideboard" to 2)

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

    private fun formatUrl(code: String): String {
        val meta = if (code == "cEDH") "&meta=300" else ""
        return "$BASE_URL/format?f=$code$meta"
    }

    internal fun xhrDecksUrl(pageHtml: String, code: String): String? {
        val endpoint = xhrRequest.find(pageHtml)?.groupValues?.get(1) ?: return null
        val meta = pageMeta.find(pageHtml)?.groupValues?.get(1) ?: return null
        return "$BASE_URL/$endpoint?f=$code&show=pop&cid=&meta=$meta&gamerid1=&gamerid2=&cEDH_cp=1"
    }

    internal fun parseArchetypes(root: Element): List<MTGTop8Archetype> {
        val entries = root.select(ARCHETYPE_ROWS)
        if (entries.isNotEmpty()) {
            return entries.mapNotNull { entry ->
                val link = entry.selectFirst("div.S14 a") ?: return@mapNotNull null
                val name = link.text().trim()
                if (name.isBlank()) return@mapNotNull null

                val href = link.attr("href")
                val id = archetypeId.find(href)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null

                val thumb = entry.selectFirst("img[src*=/metas_thumbs/]")
                val coverUrl = thumb?.attr("src")?.let { "$BASE_URL/$it" } ?: ""

                val allS14 = entry.select("div.S14")
                val metaPct = allS14.firstOrNull { it.selectFirst("a") == null }?.text()?.trim() ?: ""

                MTGTop8Archetype(name, coverUrl, metaPct, id, href)
            }
        }

        return root.select("a[href*='/archetype?']").mapNotNull { link ->
            val name = link.text().trim()
            if (name.isBlank()) return@mapNotNull null
            val href = link.attr("href")
            val id = archetypeId.find(href)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            val thumb = link.parent()?.selectFirst("img[src*=/metas_thumbs/]")
            val coverUrl = thumb?.attr("src")?.let { "$BASE_URL/$it" } ?: ""
            MTGTop8Archetype(name, coverUrl, "", id, href)
        }
    }

    suspend fun loadFormat(format: String): List<MTGTop8Archetype> = withContext(Dispatchers.IO) {
        val code = formatCodes[format.lowercase()] ?: throw IOException("Unsupported format: $format")
        val doc = connect(formatUrl(code)).get()

        val rendered = parseArchetypes(doc)
        if (rendered.isNotEmpty()) return@withContext rendered

        val requestUrl = xhrDecksUrl(doc.outerHtml(), code)
            ?: throw IOException("No archetypes found for $format")
        val fragment = connect(requestUrl).post()
        parseArchetypes(fragment).ifEmpty { throw IOException("No archetypes found for $format") }
    }

    suspend fun load(archetypeUrl: String): List<MetaDeckCard> = withContext(Dispatchers.IO) {
        val fullUrl = if (archetypeUrl.startsWith("http")) archetypeUrl
            else "$BASE_URL/$archetypeUrl"

        val archetypeDoc = connect(fullUrl).get()
        val firstDeckLink = archetypeDoc.selectFirst("tr.hover_tr a[href*='/event?'], tr.chosen_tr a[href*='/event?']")
            ?: throw IOException("No decks found on archetype page")
        val deckPath = firstDeckLink.attr("href")
        val deckUrl = if (deckPath.startsWith("http")) deckPath else "$BASE_URL/$deckPath"

        val deckDoc = connect(deckUrl).get()
        val decklistContainer = deckDoc.selectFirst("div[style*='display:flex'][style*='align-content:stretch']")
            ?: deckDoc.selectFirst("div[style*='display: flex'][style*='align-content: stretch']")
            ?: throw IOException("Could not find decklist on event page")

        val cards = parseDecklist(decklistContainer)
        if (cards.isEmpty()) throw IOException("Could not parse decklist from page")
        cards
    }

    internal fun parseDecklist(container: Element): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()
        var commanderSection = false

        val nodes = container.select("div.O14, div[id^=\"md\"].deck_line, div[id^=\"sb\"].deck_line")
        for (node in nodes) {
            if (node.`is`("div.O14")) {
                commanderSection = node.text().trim().equals("COMMANDER", ignoreCase = true)
                continue
            }

            val qty = node.ownText().trim().toIntOrNull() ?: 1
            val name = node.selectFirst("span")?.text()?.trim() ?: continue
            if (name.isBlank()) continue

            val slot = when {
                commanderSection -> "commander"
                node.id().startsWith("sb") -> "sideboard"
                else -> "mainboard"
            }
            cards.add(MetaDeckCard(qty, name, slot))
        }

        return cards.sortedBy { slotOrder[it.slot] ?: 3 }
    }
}

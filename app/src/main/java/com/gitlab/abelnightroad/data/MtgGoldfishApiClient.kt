package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object MtgGoldfishApiClient {

    private const val BASE_URL = "https://www.mtggoldfish.com/deck"
    private const val USER_AGENT = "MtGCardTracker/1.0"
    private const val BLOCKED_MESSAGE =
        "Goldfish blocked the request — copy the deck list and paste it instead"

    fun extractDeckId(url: String): String? {
        val pattern = Regex("""mtggoldfish\.com/deck/(\d+)""", RegexOption.IGNORE_CASE)
        return pattern.find(url)?.groupValues?.get(1)
    }

    suspend fun fetchDecklist(deckId: String): String = withContext(Dispatchers.IO) {
        val conn = URL("$BASE_URL/$deckId").openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            connectTimeout = 15000
            readTimeout = 15000
        }

        try {
            val responseCode = conn.responseCode
            if (responseCode == 403) throw Exception(BLOCKED_MESSAGE)
            if (responseCode != 200) {
                throw Exception("MTG Goldfish returned HTTP $responseCode — deck may be private/deleted")
            }
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            if (isChallengePage(html)) throw Exception(BLOCKED_MESSAGE)
            extractDecklist(html)
                ?: throw Exception("Could not find deck list in MTG Goldfish page")
        } finally {
            conn.disconnect()
        }
    }

    internal fun isChallengePage(html: String): Boolean =
        html.contains("Just a moment", ignoreCase = true) ||
            html.contains("challenge-platform", ignoreCase = true)

    internal fun extractDecklist(html: String): String? {
        val match = Regex(
            """<textarea[^>]*id=["']deck_input_deck["'][^>]*>(.*?)</textarea>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).find(html) ?: return null
        val text = unescapeHtml(match.groupValues[1]).trim()
        return text.ifEmpty { null }
    }

    private fun unescapeHtml(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
}

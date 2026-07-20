package com.github.abelnightroad.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScryfallBulkClientTest {

    @Test
    fun `prefers jsonl download uri`() {
        val json = """
            {
              "object": "bulk_data",
              "type": "default_cards",
              "updated_at": "2026-07-20T09:11:37.117+00:00",
              "download_uri": "https://data.scryfall.io/default-cards/x.json",
              "jsonl_download_uri": "https://data.scryfall.io/default-cards/x.jsonl.gz"
            }
        """.trimIndent()
        val meta = ScryfallBulkClient.parseMeta(json)
        assertEquals("https://data.scryfall.io/default-cards/x.jsonl.gz", meta.downloadUri)
        assertEquals("2026-07-20T09:11:37.117+00:00", meta.updatedAt)
    }

    @Test
    fun `falls back to download uri when jsonl absent`() {
        val json = """
            {
              "updated_at": "2026-01-01T00:00:00.000+00:00",
              "download_uri": "https://data.scryfall.io/default-cards/x.json"
            }
        """.trimIndent()
        val meta = ScryfallBulkClient.parseMeta(json)
        assertEquals("https://data.scryfall.io/default-cards/x.json", meta.downloadUri)
    }
}

package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.ScryfallCardEntity
import kotlin.text.Charsets
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ScryfallBulkImportTest {

    private val json = """
        [
          {
            "id": "a1",
            "name": "Black Lotus",
            "set": "lea",
            "set_name": "Limited Edition Alpha",
            "collector_number": "232",
            "rarity": "mythic",
            "mana_cost": "{0}",
            "type_line": "Artifact",
            "oracle_text": "tap"
          },
          {
            "id": "",
            "name": "",
            "set": "lea"
          },
          {
            "id": "b2",
            "name": "Ancestral Recall",
            "set": "lea",
            "set_name": "Limited Edition Alpha",
            "collector_number": "1",
            "rarity": "mythic"
          }
        ]
    """.trimIndent()

    @Test
    fun `imports valid cards and skips blank ones`() {
        val inserted = mutableListOf<ScryfallCardEntity>()
        val count = runBlocking {
            ScryfallBulkImport.import(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))) {
                inserted += it
            }
        }
        assertEquals(2, count)
        assertEquals("Black Lotus", inserted[0].name)
        assertEquals("lea", inserted[0].setCode)
        assertEquals("Limited Edition Alpha", inserted[0].setName)
        assertEquals("{0}", inserted[0].manaCost)
        assertEquals("Ancestral Recall", inserted[1].name)
        assertEquals("", inserted[1].manaCost)
    }

    @Test
    fun `imports gzipped json lines`() {
        val jsonl = """
            {"id":"c1","name":"Black Lotus","set":"lea","set_name":"Limited Edition Alpha","collector_number":"232","rarity":"mythic"}
            {"id":"","name":""}
            {"id":"c2","name":"Ancestral Recall","set":"lea","set_name":"Limited Edition Alpha","collector_number":"1","rarity":"mythic"}
        """.trimIndent()
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(jsonl.toByteArray(Charsets.UTF_8)) }
        val inserted = mutableListOf<ScryfallCardEntity>()
        val count = runBlocking {
            ScryfallBulkImport.import(ByteArrayInputStream(baos.toByteArray())) {
                inserted += it
            }
        }
        assertEquals(2, count)
        assertEquals("Black Lotus", inserted[0].name)
        assertEquals("Ancestral Recall", inserted[1].name)
    }
}

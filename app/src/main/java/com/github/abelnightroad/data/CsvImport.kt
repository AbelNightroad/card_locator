package com.github.abelnightroad.data

import com.github.abelnightroad.db.CardEntity
import java.io.InputStream

/**
 * Parses a ManaBox-style CSV export into [CardEntity] rows.
 *
 * The CSV has 16 columns. Card names may contain a ` // ` separator for split
 * cards; we keep the full name verbatim (search matches either half).
 * Quoted fields (e.g. names with commas) are unquoted. Quantity/Purchase price
 * are coerced defensively; malformed rows are skipped.
 */
object CsvImport {

    private val HEADER = listOf(
        "Name", "Set code", "Set name", "Collector number", "Foil", "Rarity",
        "Quantity", "ManaBox ID", "Scryfall ID", "Purchase price", "Misprint",
        "Altered", "Condition", "Language", "Purchase price currency", "Added"
    )

    data class Result(val cards: List<CardEntity>, val skipped: Int)

    fun parse(input: InputStream, tag: String): Result {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parse(text, tag)
    }

    fun parse(text: String, tag: String): Result {
        val rows = splitCsv(text)
        if (rows.isEmpty()) return Result(emptyList(), 0)

        val nameIdx = HEADER.indexOf("Name")
        val setCodeIdx = HEADER.indexOf("Set code")
        val setNameIdx = HEADER.indexOf("Set name")
        val collIdx = HEADER.indexOf("Collector number")
        val foilIdx = HEADER.indexOf("Foil")
        val rarityIdx = HEADER.indexOf("Rarity")
        val qtyIdx = HEADER.indexOf("Quantity")
        val mbxIdx = HEADER.indexOf("ManaBox ID")
        val scryIdx = HEADER.indexOf("Scryfall ID")
        val priceIdx = HEADER.indexOf("Purchase price")
        val misIdx = HEADER.indexOf("Misprint")
        val altIdx = HEADER.indexOf("Altered")
        val condIdx = HEADER.indexOf("Condition")
        val langIdx = HEADER.indexOf("Language")
        val curIdx = HEADER.indexOf("Purchase price currency")
        val addedIdx = HEADER.indexOf("Added")

        val cards = mutableListOf<CardEntity>()
        var skipped = 0

        for (i in 1 until rows.size) {
            val cols = parseLine(rows[i])
            if (cols.size < HEADER.size) {
                skipped++
                continue
            }
            val qty = cols[qtyIdx].toIntOrNull() ?: 0
            if (qty <= 0) {
                skipped++
                continue
            }
            cards.add(
                CardEntity(
                    name = cols[nameIdx].trim(),
                    setCode = cols[setCodeIdx].trim(),
                    setName = cols[setNameIdx].trim(),
                    collectorNumber = cols[collIdx].trim(),
                    foil = cols[foilIdx].trim(),
                    rarity = cols[rarityIdx].trim(),
                    quantity = qty,
                    manaBoxId = cols[mbxIdx].trim(),
                    scryfallId = cols[scryIdx].trim(),
                    purchasePrice = cols[priceIdx].toDoubleOrNull() ?: 0.0,
                    misprint = cols[misIdx].trim() == "true",
                    altered = cols[altIdx].trim() == "true",
                    condition = cols[condIdx].trim(),
                    language = cols[langIdx].trim(),
                    purchasePriceCurrency = cols[curIdx].trim(),
                    added = cols[addedIdx].trim(),
                    tag = tag
                )
            )
        }
        return Result(cards, skipped)
    }

    /** Splits text into raw CSV rows, respecting quoted fields spanning commas. */
    private fun splitCsv(text: String): List<String> {
        val rows = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        val chars = text.replace("\r\n", "\n")
        for (c in chars) {
            when {
                c == '"' -> {
                    if (inQuotes && sb.isNotEmpty() && sb.last() == '"') {
                        sb.append('"')
                        sb.deleteAt(sb.length - 2)
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == '\n' && !inQuotes -> {
                    if (sb.isNotEmpty() || rows.isNotEmpty()) rows.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        if (sb.isNotEmpty()) rows.add(sb.toString())
        return rows
    }

    /** Parses a single CSV line into fields, unquoting as needed. */
    private fun parseLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var started = false
        for (c in line) {
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                    started = true
                }
                c == ',' && !inQuotes -> {
                    fields.add(sb.toString())
                    sb.clear()
                    started = false
                }
                else -> {
                    sb.append(c)
                    started = true
                }
            }
        }
        fields.add(sb.toString())
        return if (!started && fields.size == 1 && fields[0].isEmpty()) emptyList() else fields
    }
}

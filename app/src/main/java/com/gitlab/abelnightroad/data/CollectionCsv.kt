package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.CardEntity

object CollectionCsv {

    private val header = listOf(
        "tag", "name", "set_code", "set_name", "collector_number", "foil",
        "rarity", "quantity", "mana_box_id", "scryfall_id", "purchase_price",
        "misprint", "altered", "condition", "language",
        "purchase_price_currency", "added"
    )

    fun encode(cards: List<CardEntity>): String = buildString {
        append(header.joinToString(",") { quote(it) })
        append("\r\n")
        for (card in cards) {
            append(
                listOf(
                    card.tag, card.name, card.setCode, card.setName,
                    card.collectorNumber, card.foil, card.rarity,
                    card.quantity.toString(), card.manaBoxId, card.scryfallId,
                    card.purchasePrice.toString(), card.misprint.toString(),
                    card.altered.toString(), card.condition, card.language,
                    card.purchasePriceCurrency, card.added
                ).joinToString(",") { quote(it) }
            )
            append("\r\n")
        }
    }

    fun parse(text: String): List<CardEntity> {
        val rows = parseRows(text)
        if (rows.isEmpty()) throw IllegalArgumentException("Empty CSV file")
        if (rows.first() != header) {
            throw IllegalArgumentException("Not a Card Tracker collection CSV")
        }
        return rows.drop(1).filter { row -> row.any { it.isNotEmpty() } }.map { row ->
            if (row.size != header.size) {
                throw IllegalArgumentException("Malformed CSV row: expected ${header.size} columns, got ${row.size}")
            }
            CardEntity(
                tag = row[0],
                name = row[1],
                setCode = row[2],
                setName = row[3],
                collectorNumber = row[4],
                foil = row[5],
                rarity = row[6],
                quantity = row[7].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid quantity: ${row[7]}"),
                manaBoxId = row[8],
                scryfallId = row[9],
                purchasePrice = row[10].toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid purchase price: ${row[10]}"),
                misprint = row[11].toBoolean(),
                altered = row[12].toBoolean(),
                condition = row[13],
                language = row[14],
                purchasePriceCurrency = row[15],
                added = row[16]
            )
        }
    }

    private fun quote(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun parseRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes && ch == '"' -> {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                !inQuotes && ch == '"' -> inQuotes = true
                !inQuotes && ch == ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                !inQuotes && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(field.toString())
                    field.setLength(0)
                    rows.add(row)
                    row = mutableListOf()
                }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}

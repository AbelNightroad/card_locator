package com.gitlab.abelnightroad.data

object TappedOutCsvParser {

    fun parse(text: String): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) return emptyList()

        val header = lines.first()
        val isHeader = header.contains("Board", ignoreCase = true) ||
            header.contains("Name", ignoreCase = true)

        val dataLines = if (isHeader) lines.drop(1) else lines

        for (line in dataLines) {
            val parts = parseCsvLine(line)
            if (parts.size < 3) continue

            val board = parts[0].lowercase().trim()
            val qty = parts[1].toIntOrNull() ?: continue
            val name = parts[2].removeSurrounding("\"").trim()

            if (name.isBlank()) continue

            val slot = when {
                board.startsWith("side") -> "sideboard"
                board.startsWith("main") || board == "main" -> "mainboard"
                board.startsWith("command") -> "commander"
                board.startsWith("companion") -> "companion"
                board.startsWith("maybe") || board.startsWith("consider") -> "mainboard"
                else -> "mainboard"
            }

            cards.add(MetaDeckCard(qty, name, slot))
        }

        return cards
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}

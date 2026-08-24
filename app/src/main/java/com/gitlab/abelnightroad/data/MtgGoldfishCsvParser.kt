package com.gitlab.abelnightroad.data

object MtgGoldfishCsvParser {

    private val CSV_SPLIT = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")

    fun parse(text: String): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) return emptyList()

        val header = lines.first()
        val isHeader = header.contains("Card", ignoreCase = true) ||
            header.contains("Name", ignoreCase = true) ||
            header.contains("Quantity", ignoreCase = true)

        val dataLines = if (isHeader) lines.drop(1) else lines

        for (line in dataLines) {
            val parts = parseCsvLine(line)
            if (parts.isEmpty()) continue

            val (name, quantity) = when {
                parts.size >= 4 && parts[0].contains("Card", ignoreCase = true) -> {
                    parts[0] to (parts.getOrNull(3)?.toIntOrNull() ?: 1)
                }
                parts.size >= 4 && parts[0].toDoubleOrNull() != null -> {
                    (parts.getOrNull(2) ?: "") to (parts.getOrNull(3)?.toIntOrNull() ?: 1)
                }
                parts.size >= 2 -> {
                    parts[0] to (parts.getOrNull(1)?.toIntOrNull() ?: 1)
                }
                else -> continue
            }

            val cleanName = name.removeSurrounding("\"").trim()
            if (cleanName.isNotBlank()) {
                cards.add(MetaDeckCard(quantity, cleanName, "mainboard"))
            }
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

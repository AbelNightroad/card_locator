package com.gitlab.abelnightroad.data

object EdhPlayDecklistParser {

    private val SECTION_HEADERS = listOf(
        "commander", "mainboard", "main deck", "sideboard",
        "companion", "maybeboard", "considering"
    )

    private val LINE_PATTERN = Regex("""^\s*(\d+)\s+x?\s*(.+?)\s*$""")
    private val URL_PATTERN = Regex("""^https?://""", RegexOption.IGNORE_CASE)
    private val TOTAL_PATTERN = Regex("""^total[:\s]""", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()
        var currentSlot = "mainboard"

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank()) continue

            if (TOTAL_PATTERN.containsMatchIn(line)) continue
            if (URL_PATTERN.containsMatchIn(line)) continue

            val lower = line.lowercase().removePrefix("//").trim()
            if (lower in SECTION_HEADERS || SECTION_HEADERS.any { h -> lower.startsWith(h) }) {
                currentSlot = when {
                    lower.startsWith("command") -> "commander"
                    lower.startsWith("side") -> "sideboard"
                    lower.startsWith("companion") -> "companion"
                    else -> "mainboard"
                }
                continue
            }

            val match = LINE_PATTERN.matchEntire(line)
            if (match != null) {
                val qty = match.groupValues[1].toIntOrNull() ?: 1
                val name = match.groupValues[2].trim()
                if (name.isNotBlank()) {
                    cards.add(MetaDeckCard(qty, name, currentSlot))
                }
            } else {
                val name = line.trim()
                if (name.isNotBlank() && !name.startsWith("//")) {
                    cards.add(MetaDeckCard(1, name, currentSlot))
                }
            }
        }

        return cards
    }
}

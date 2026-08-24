package com.gitlab.abelnightroad.data

object TappedOutDckParser {

    private val DCK_LINE = Regex("""^\s*(\d+)\s+\[([A-Z0-9]+):\?\]\s+(.+?)\s*$""")
    private val SIMPLE_LINE = Regex("""^\s*(\d+)\s+[xX]?\s+(.+?)\s*$""")
    private val SECTION_HEADER = Regex("""^\s*(Sideboard|Commander|Companion|Maybeboard)\s*[:]*$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()
        var currentSlot = "mainboard"

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank()) continue

            val sectionMatch = SECTION_HEADER.matchEntire(line)
            if (sectionMatch != null) {
                currentSlot = when (sectionMatch.groupValues[1].lowercase()) {
                    "sideboard" -> "sideboard"
                    "commander" -> "commander"
                    "companion" -> "companion"
                    else -> "mainboard"
                }
                continue
            }

            val dckMatch = DCK_LINE.matchEntire(line)
            if (dckMatch != null) {
                val qty = dckMatch.groupValues[1].toIntOrNull() ?: 1
                val name = dckMatch.groupValues[3].trim()
                if (name.isNotBlank()) {
                    cards.add(MetaDeckCard(qty, name, currentSlot))
                }
                continue
            }

            val simpleMatch = SIMPLE_LINE.matchEntire(line)
            if (simpleMatch != null) {
                val qty = simpleMatch.groupValues[1].toIntOrNull() ?: 1
                val name = simpleMatch.groupValues[2].trim()
                if (name.isNotBlank()) {
                    cards.add(MetaDeckCard(qty, name, currentSlot))
                }
            }
        }

        return cards
    }
}

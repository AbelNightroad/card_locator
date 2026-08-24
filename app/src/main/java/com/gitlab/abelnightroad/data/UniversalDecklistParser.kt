package com.gitlab.abelnightroad.data

object UniversalDecklistParser {

    private val SECTION_HEADERS = listOf(
        "commander", "mainboard", "main deck", "sideboard",
        "companion", "maybeboard", "considering", "deck",
        "partner", "background"
    )

    private val LINE_PATTERN = Regex("""^\s*(\d+)\s+[xX]?\s*(.+?)\s*$""")
    private val SET_EXTRACTOR = Regex("""^(.+?)\s*\(([A-Z0-9]{2,5})\)\s*$""")
    private val URL_PATTERN = Regex("""^https?://""", RegexOption.IGNORE_CASE)
    private val TOTAL_PATTERN = Regex("""^total[:\s]""", RegexOption.IGNORE_CASE)
    private val SIDEBOARD_HEADER = Regex("""^sideboard\s*[:]*$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<MetaDeckCard> {
        val cards = mutableListOf<MetaDeckCard>()
        var currentSlot = "mainboard"

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank()) continue

            if (TOTAL_PATTERN.containsMatchIn(line)) continue
            if (URL_PATTERN.containsMatchIn(line)) continue

            val stripped = line.removePrefix("//").trim()
            val lower = stripped.lowercase()

            if (SIDEBOARD_HEADER.matches(lower)) {
                currentSlot = "sideboard"
                continue
            }

            if (lower in SECTION_HEADERS || SECTION_HEADERS.any { h -> lower.startsWith("$h ") }) {
                currentSlot = when {
                    lower.startsWith("command") -> "commander"
                    lower.startsWith("side") -> "sideboard"
                    lower.startsWith("companion") -> "companion"
                    lower.startsWith("partner") || lower.startsWith("background") -> "commander"
                    else -> "mainboard"
                }
                continue
            }

            val match = LINE_PATTERN.matchEntire(line)
            if (match != null) {
                val qty = match.groupValues[1].toIntOrNull() ?: 1
                val rawName = match.groupValues[2].trim()
                val (name, setCode) = extractNameAndSet(rawName)
                if (name.isNotBlank()) {
                    cards.add(MetaDeckCard(qty, name, currentSlot))
                }
            } else {
                val name = stripped
                if (name.isNotBlank() && name.length > 1) {
                    cards.add(MetaDeckCard(1, name, currentSlot))
                }
            }
        }

        return cards
    }

    private fun extractNameAndSet(raw: String): Pair<String, String> {
        val match = SET_EXTRACTOR.matchEntire(raw) ?: return raw to ""
        val name = match.groupValues[1].trim()
        val setCode = match.groupValues[2].uppercase()
        return name to setCode
    }
}

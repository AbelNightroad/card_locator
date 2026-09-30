package com.gitlab.abelnightroad.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Detects the shape of a 3rd-party collection export and imports it into a tag.
 *
 * Two formats are supported:
 * - ManaBox CSV (16 comma-separated columns with a header row) → [CsvImport],
 *   stored as-is via [CardRepository.insertAll].
 * - Plain quantity list (`qty Name (SET) collector [*F*]`) → [QtyListImport],
 *   enriched from the local Scryfall reference table and merged into existing
 *   cards via [CardRepository.insertMergingDuplicates].
 *
 * Detection looks at the first non-blank line: a ManaBox header (or any line
 * with 16+ comma-separated fields) selects the CSV path; everything else falls
 * back to the quantity-list parser, so `.txt` scanner exports import correctly
 * instead of being rejected column-by-column.
 */
object ThirdPartyImport {

    data class SkippedRow(val row: String, val reason: String)

    data class Result(
        val imported: Int,
        val skippedRows: List<SkippedRow>,
        val unresolved: Int = 0
    )

    fun looksLikeManaBoxCsv(text: String): Boolean {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
        val lower = firstLine.lowercase()
        if (lower.contains("set code") && lower.contains("collector number")) return true
        return firstLine.count { it == ',' } >= 15
    }

    suspend fun import(
        text: String,
        tag: String,
        repository: CardRepository,
        scryfall: ScryfallRepository
    ): Result = withContext(Dispatchers.IO) {
        if (looksLikeManaBoxCsv(text)) {
            val csv = CsvImport.parse(text, tag)
            repository.insertAll(csv.cards)
            Result(
                imported = csv.cards.size,
                skippedRows = csv.skippedRows.map { SkippedRow(it.row, it.reason) }
            )
        } else {
            val parsed = QtyListImport.parse(text, tag)
            var unresolved = 0
            val enriched = parsed.cards.map { card ->
                val ref = scryfall.lookupBySetAndCollector(card.setCode, card.collectorNumber)
                    ?: scryfall.lookupByNameAndSet(card.name, card.setCode)
                if (ref == null) {
                    unresolved++
                    card
                } else {
                    card.copy(setName = ref.setName, rarity = ref.rarity, scryfallId = ref.id)
                }
            }
            repository.insertMergingDuplicates(enriched)
            Result(
                imported = enriched.size,
                skippedRows = parsed.skippedRows.map { SkippedRow(it.row, it.reason) },
                unresolved = unresolved
            )
        }
    }
}

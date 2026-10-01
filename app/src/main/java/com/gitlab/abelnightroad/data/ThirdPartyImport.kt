package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.CardEntity
import com.gitlab.abelnightroad.db.ScryfallCardEntity
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
        scryfall: ScryfallRepository,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result = withContext(Dispatchers.IO) {
        if (looksLikeManaBoxCsv(text)) {
            val csv = CsvImport.parse(text, tag)
            onProgress(0, csv.cards.size)
            repository.insertAll(csv.cards)
            onProgress(csv.cards.size, csv.cards.size)
            Result(
                imported = csv.cards.size,
                skippedRows = csv.skippedRows.map { SkippedRow(it.row, it.reason) }
            )
        } else {
            val parsed = QtyListImport.parse(text, tag)
            val enriched = enrichWithProgress(parsed.cards, onProgress) { card ->
                scryfall.lookupBySetAndCollector(card.setCode, card.collectorNumber)
                    ?: scryfall.lookupByNameAndSet(card.name, card.setCode)
            }
            repository.insertMergingDuplicates(enriched.cards)
            Result(
                imported = enriched.cards.size,
                skippedRows = parsed.skippedRows.map { SkippedRow(it.row, it.reason) },
                unresolved = enriched.unresolved
            )
        }
    }

    internal class EnrichmentOutcome(
        val cards: List<CardEntity>,
        val unresolved: Int
    )

    internal suspend fun enrichWithProgress(
        cards: List<CardEntity>,
        onProgress: (processed: Int, total: Int) -> Unit,
        lookup: suspend (CardEntity) -> ScryfallCardEntity?
    ): EnrichmentOutcome {
        var unresolved = 0
        val total = cards.size
        val enriched = ArrayList<CardEntity>(total)
        for ((index, card) in cards.withIndex()) {
            val ref = lookup(card)
            if (ref == null) {
                unresolved++
                enriched.add(card)
            } else {
                enriched.add(
                    card.copy(setName = ref.setName, rarity = ref.rarity, scryfallId = ref.id)
                )
            }
            onProgress(index + 1, total)
        }
        return EnrichmentOutcome(enriched, unresolved)
    }
}

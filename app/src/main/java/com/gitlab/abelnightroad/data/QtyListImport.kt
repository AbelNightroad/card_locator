package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.CardEntity
import java.time.LocalDateTime

/**
 * Parses a 3rd-party collection list into [CardEntity] rows.
 *
 * Expected line format (one card per line):
 * `qty Name (SET) collector_number [*finish*]`
 *
 * Examples:
 * `1 Jyoti, Moag Ancient (M3C) 8 *F*`
 * `2 Many Partings (LTR) 176`
 *
 * The name is kept verbatim (commas, apostrophes, `//` split cards); the
 * set/collector pair identifies the exact printing (art variants differ by
 * collector number). Finish markers map to [CardEntity.foil]:
 * absent -> "normal", F -> "foil", E -> "etched", FE/EF -> "foil etched".
 * Anything else in the marker is reported as a skipped row.
 *
 * Blank lines are ignored. Rows that do not match the format are reported in
 * [Result.skippedRows] with a reason; no data is silently dropped.
 *
 * Duplicates (same name + set + collector number + finish) are merged by
 * summing their quantities. Rows that differ in any of those fields stay
 * separate.
 */
object QtyListImport {

    private val LINE = Regex(
        """^\s*(\d+)\s+(.+)\s+\(([A-Za-z0-9]+)\)\s+(\S+)\s*(?:\*([A-Za-z0-9]+)\*)?\s*$"""
    )

    data class SkippedRow(val row: String, val reason: String)

    data class Result(val cards: List<CardEntity>, val skipped: Int, val skippedRows: List<SkippedRow>)

    fun parse(text: String, tag: String): Result {
        val cards = mutableListOf<CardEntity>()
        val indexByKey = HashMap<List<String>, Int>()
        val skippedRows = mutableListOf<SkippedRow>()

        for (raw in text.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
            if (raw.isBlank()) continue
            val match = LINE.matchEntire(raw)
            if (match == null) {
                skippedRows.add(SkippedRow(raw, "Expected: Qty Name (SET) Number"))
                continue
            }
            val (qtyText, name, setCode, collectorNumber) = match.destructured
            val qty = qtyText.toIntOrNull()
            if (qty == null || qty <= 0) {
                skippedRows.add(SkippedRow(raw, "Invalid quantity"))
                continue
            }
            val finish = finishOf(match.groupValues[5])
            if (finish == null) {
                skippedRows.add(SkippedRow(raw, "Unknown finish marker: ${match.groupValues[5]}"))
                continue
            }
            val card = CardEntity(
                name = name.trim(),
                setCode = setCode,
                setName = "",
                collectorNumber = collectorNumber,
                foil = finish,
                rarity = "",
                quantity = qty,
                manaBoxId = "",
                scryfallId = "",
                purchasePrice = 0.0,
                misprint = false,
                altered = false,
                condition = "",
                language = "",
                purchasePriceCurrency = "",
                added = LocalDateTime.now().toString(),
                tag = tag
            )
            val key = listOf(
                card.name.uppercase(), card.setCode.uppercase(),
                card.collectorNumber.uppercase(), card.foil
            )
            val existing = indexByKey[key]
            if (existing == null) {
                indexByKey[key] = cards.size
                cards.add(card)
            } else {
                cards[existing] = cards[existing].copy(quantity = cards[existing].quantity + card.quantity)
            }
        }
        return Result(cards, skippedRows.size, skippedRows)
    }

    /** Maps a finish marker to the stored foil value, or null when unrecognized. */
    private fun finishOf(marker: String): String? {
        if (marker.isEmpty()) return "normal"
        val tokens = marker.uppercase().toSet()
        return when {
            tokens == setOf('F') -> "foil"
            tokens == setOf('E') -> "etched"
            tokens == setOf('F', 'E') -> "etched foil"
            else -> null
        }
    }
}

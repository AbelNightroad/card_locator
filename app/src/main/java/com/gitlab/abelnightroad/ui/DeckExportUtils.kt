package com.gitlab.abelnightroad.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.gitlab.abelnightroad.db.DeckCardEntity
import com.gitlab.abelnightroad.data.primaryType
import java.io.File

internal fun exportDeckToTxt(cards: List<DeckCardEntity>): String {
    val sb = StringBuilder()
    val grouped = cards
        .filter { it.slot != "sideboard" }
        .groupBy { it.slot }
        .toSortedMap(compareBy { listOf("commander", "companion", "mainboard").indexOf(it) })

    for ((slot, slotCards) in grouped) {
        if (slot != "mainboard") {
            sb.appendLine("--- ${slot.replaceFirstChar { it.uppercase() }} ---")
        }
        val byType = slotCards.groupBy { primaryType(it.typeLine) }
        for ((type, typeCards) in byType) {
            if (slot == "mainboard") {
                sb.appendLine("// $type")
            }
            for (card in typeCards.sortedBy { it.cardName }) {
                sb.appendLine("${card.quantity} ${card.cardName} (${card.setCode})")
            }
        }
        sb.appendLine()
    }

    val sideboard = cards.filter { it.slot == "sideboard" }
    if (sideboard.isNotEmpty()) {
        sb.appendLine("--- Sideboard ---")
        for (card in sideboard.sortedBy { it.cardName }) {
            sb.appendLine("${card.quantity} ${card.cardName} (${card.setCode})")
        }
    }

    return sb.toString().trimEnd()
}

internal fun shareDeckFile(context: Context, deckName: String, content: String) {
    val file = File(context.cacheDir, "decks").apply { mkdirs() }
    val exportFile = File(file, "${deckName.replace(Regex("[^a-zA-Z0-9_]"), "_")}.txt")
    exportFile.writeText(content)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exportFile
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, deckName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export $deckName"))
}

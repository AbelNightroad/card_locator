package com.gitlab.abelnightroad.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.QtyListImport
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.ui.components.ImportResult
import com.gitlab.abelnightroad.ui.components.SkippedEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CardListViewModel(
    private val repository: CardRepository,
    private val scryfall: ScryfallRepository
) : ViewModel() {

    fun cardsByTag(tag: String): StateFlow<List<CardSearchResult>> =
        repository.cardsByTag(tag)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportToClipboard(tag: String, cards: List<CardSearchResult>, context: Context) {
        val text = cards.joinToString("\n") { "${it.quantity} ${it.name}" }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(tag, text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun exportToTxt(tag: String, cards: List<CardSearchResult>, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = cards.joinToString("\n") { "${it.quantity} ${it.name}" }
            val filename = "${tag.replace(" ", "_")}.txt"
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val file = File(downloadsDir, filename)
            file.writeText(text)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Exported to Downloads/$filename", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch { repository.deleteCard(cardId) }
    }

    /**
     * Imports a 3rd-party list into [tag] (no tag prompt — the tag comes from
     * the screen). Each row is enriched from the local Scryfall reference data
     * (set name, rarity, image id) and duplicate cards are merged by summing
     * quantities. Returns the outcome for the result dialog.
     */
    suspend fun importIntoTag(tag: String, text: String): ImportResult =
        withContext(Dispatchers.IO) {
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
            ImportResult(
                imported = enriched.size,
                skippedRows = parsed.skippedRows.map { SkippedEntry(it.row, it.reason) },
                unresolved = unresolved
            )
        }

    fun incrementQuantity(cardId: Long) {
        viewModelScope.launch { repository.incrementQuantity(cardId) }
    }

    fun decrementQuantity(cardId: Long) {
        viewModelScope.launch { repository.decrementQuantity(cardId) }
    }
}

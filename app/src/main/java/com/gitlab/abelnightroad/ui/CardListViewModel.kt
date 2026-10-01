package com.gitlab.abelnightroad.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.ThirdPartyImport
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
     * the screen). [ThirdPartyImport] detects the file format (ManaBox CSV or
     * quantity list), enriches rows from the local Scryfall reference data and
     * merges duplicates. Returns the outcome for the result dialog.
     */
    suspend fun importIntoTag(
        tag: String,
        text: String,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportResult {
        val result = ThirdPartyImport.import(text, tag, repository, scryfall, onProgress)
        return ImportResult(
            imported = result.imported,
            skippedRows = result.skippedRows.map { SkippedEntry(it.row, it.reason) },
            unresolved = result.unresolved
        )
    }

    fun incrementQuantity(cardId: Long) {
        viewModelScope.launch { repository.incrementQuantity(cardId) }
    }

    fun decrementQuantity(cardId: Long) {
        viewModelScope.launch { repository.decrementQuantity(cardId) }
    }
}

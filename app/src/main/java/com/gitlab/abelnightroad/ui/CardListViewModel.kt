package com.gitlab.abelnightroad.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.db.CardSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CardListViewModel(private val repository: CardRepository) : ViewModel() {

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

    fun incrementQuantity(cardId: Long) {
        viewModelScope.launch { repository.incrementQuantity(cardId) }
    }

    fun decrementQuantity(cardId: Long) {
        viewModelScope.launch { repository.decrementQuantity(cardId) }
    }
}

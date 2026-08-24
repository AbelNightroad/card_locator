package com.gitlab.abelnightroad.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.ui.components.EdhPlayWebView
import kotlinx.coroutines.launch

@Composable
internal fun EdhPlayWebViewScreen(
    deckUrl: String,
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit,
    onImportComplete: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    EdhPlayWebView(
        deckUrl = deckUrl,
        onBack = onBack,
        onDeckParsed = { cards ->
            if (cards.isEmpty()) {
                Toast.makeText(context, "No cards found on page", Toast.LENGTH_SHORT).show()
                return@EdhPlayWebView
            }
            scope.launch {
                try {
                    val name = "EDH Play Import"
                    val deckId = deckRepository.createDeck(name, "Commander", "edhplay")
                    importDeckCards(deckId, cards, deckRepository, scryfall, "Commander")
                    val msg = "Imported $name (${cards.size} cards)"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onImportComplete(deckId)
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    )
}

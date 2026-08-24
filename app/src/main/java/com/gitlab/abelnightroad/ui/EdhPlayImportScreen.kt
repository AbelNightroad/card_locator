package com.gitlab.abelnightroad.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import compose.icons.Octicons
import compose.icons.octicons.*
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.EdhPlayDecklistParser
import com.gitlab.abelnightroad.data.ScryfallRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EdhPlayImportScreen(
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit,
    onNavigateToWebView: (String) -> Unit = {},
    onImportComplete: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var decklistText by remember { mutableStateOf("") }
    var deckName by remember { mutableStateOf("") }
    var deckUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf("Commander") }
    var formatExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from EDH Play") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Paste your EDH Play decklist below:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp))

            OutlinedTextField(
                value = deckName,
                onValueChange = { deckName = it },
                label = { Text("Deck Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Box {
                OutlinedButton(onClick = { formatExpanded = true }) {
                    Text("Format: $selectedFormat")
                }
                DropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                    ALL_FORMATS.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format) },
                            onClick = {
                                selectedFormat = format
                                formatExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = decklistText,
                onValueChange = { decklistText = it },
                label = { Text("Decklist") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(Modifier.height(16.dp))

            Text("Or import from EDH Play URL:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp))

            OutlinedTextField(
                value = deckUrl,
                onValueChange = { deckUrl = it },
                label = { Text("EDH Play Deck URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://edhplay.com/decks/...") }
            )

            Spacer(Modifier.height(8.dp))

            val isUrlValid = deckUrl.isNotBlank() && Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(deckUrl)
            Button(
                onClick = { onNavigateToWebView(deckUrl) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isUrlValid
            ) {
                Text("Open in WebView")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (decklistText.isBlank()) return@Button
                    isImporting = true
                    scope.launch {
                        try {
                            val cards = EdhPlayDecklistParser.parse(decklistText)
                            if (cards.isEmpty()) {
                                Toast.makeText(context, "No cards found in decklist", Toast.LENGTH_SHORT).show()
                                isImporting = false
                                return@launch
                            }
                            val name = deckName.ifBlank { "EDH Play Import" }
                            val deckId = deckRepository.createDeck(name, selectedFormat, "edhplay")
                            importDeckCards(deckId, cards, deckRepository, scryfall, selectedFormat)
                            val msg = "Imported $name (${cards.size} cards)"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onImportComplete(deckId)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        isImporting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = decklistText.isNotBlank() && !isImporting
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Import Deck")
            }
        }
    }
}

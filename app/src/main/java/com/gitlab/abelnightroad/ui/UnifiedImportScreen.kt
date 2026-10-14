package com.gitlab.abelnightroad.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.MetaDeckCard
import com.gitlab.abelnightroad.data.ScryfallRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnifiedImportSheet(
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    onDismiss: () -> Unit,
    onImportComplete: (Long) -> Unit
) {
    val context = LocalContext.current
    val vm: UnifiedImportViewModel = viewModel {
        UnifiedImportViewModel(
            context.applicationContext as Application,
            deckRepository,
            scryfall
        )
    }
    val state by vm.state.collectAsState()
    val deckName by vm.deckName.collectAsState()
    val format by vm.format.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var formatExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state) {
        when (val s = state) {
            is ImportState.Success -> {
                Toast.makeText(context, "Deck imported successfully", Toast.LENGTH_SHORT).show()
                vm.clearInputs()
                onImportComplete(s.deckId)
            }
            is ImportState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                vm.reset()
            }
            else -> {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            vm.clearInputs()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Import Deck", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = deckName,
                onValueChange = { vm.setDeckName(it) },
                label = { Text("Deck Name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Box {
                OutlinedButton(onClick = { formatExpanded = true }) {
                    Text("Format: $format")
                }
                DropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                    ALL_FORMATS.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f) },
                            onClick = { vm.setFormat(f); formatExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val firstLine = inputText.trim().lineSequence().first()
            val detectedSource = ImportSource.detect(firstLine)
            val isUrl = firstLine.isNotBlank() && (
                Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(firstLine) ||
                    detectedSource != null
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Deck URL or decklist text") },
                placeholder = { Text("Paste a deck URL\nor decklist text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            detectedSource?.let { source ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Detected: ${source.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isUrl) vm.parseUrl(inputText) else vm.parseText(inputText)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputText.isNotBlank() && state !is ImportState.Parsing
            ) {
                Text(if (isUrl) "Fetch from URL" else "Parse Decklist")
            }

            if (state is ImportState.Parsing || state is ImportState.Importing) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(if (state is ImportState.Importing) "Importing..." else "Parsing...")
                }
            }
        }
    }

    if (state is ImportState.Preview) {
        val preview = state as ImportState.Preview
        PreviewDialog(
            cards = preview.cards,
            sourceLabel = preview.sourceLabel,
            onConfirm = { vm.importDeck() },
            onDismiss = { vm.reset() }
        )
    }
}

@Composable
private fun PreviewDialog(
    cards: List<MetaDeckCard>,
    sourceLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview: ${cards.size} cards from $sourceLabel") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().height(300.dp)) {
                items(cards) { card ->
                    ListItem(
                        headlineContent = { Text(card.cardName) },
                        supportingContent = { Text("${card.quantity}x \u00b7 ${card.slot}") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

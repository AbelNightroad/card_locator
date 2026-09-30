package com.gitlab.abelnightroad.ui

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.gitlab.abelnightroad.data.ScryfallRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnifiedImportScreen(
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit,
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

    var selectedSource by remember { mutableStateOf(ImportSource.MOXFIELD) }
    var inputText by remember { mutableStateOf("") }
    var formatExpanded by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.parseFile(it, selectedSource) }
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is ImportState.Success -> {
                Toast.makeText(context, "Deck imported successfully", Toast.LENGTH_SHORT).show()
                onImportComplete(s.deckId)
            }
            is ImportState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                vm.reset()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Deck") },
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
            Text("Import Source", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ImportSource.entries) { source ->
                    FilterChip(
                        selected = selectedSource == source,
                        onClick = { selectedSource = source },
                        label = { Text(source.label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
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

            when (selectedSource) {
                ImportSource.MOXFIELD -> UrlImportSection(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onFetchUrl = { vm.parseUrl(inputText, selectedSource) },
                    onParseText = { vm.parseText(inputText, selectedSource) },
                    onImportFile = { fileLauncher.launch(arrayOf("text/csv", "text/plain", "*/*")) },
                    urlPlaceholder = "https://moxfield.com/decks/...",
                    textLabel = "Moxfield URL or decklist text"
                )
                ImportSource.EDHREC -> UrlImportSection(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onFetchUrl = { vm.parseUrl(inputText, selectedSource) },
                    onParseText = { vm.parseText(inputText, selectedSource) },
                    onImportFile = { fileLauncher.launch(arrayOf("text/csv", "text/plain", "*/*")) },
                    urlPlaceholder = "https://edhrec.com/average-decks/...",
                    textLabel = "EDHREC URL or decklist text"
                )
                ImportSource.ARCHIDEKT -> UrlImportSection(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onFetchUrl = { vm.parseUrl(inputText, selectedSource) },
                    onParseText = { vm.parseText(inputText, selectedSource) },
                    onImportFile = { fileLauncher.launch(arrayOf("text/csv", "text/plain", "*/*")) },
                    urlPlaceholder = "https://archidekt.com/decks/...",
                    textLabel = "Archidekt URL or decklist text"
                )
                else -> TextFileImportSection(
                    source = selectedSource,
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onImport = { vm.parseText(inputText, selectedSource) },
                    onImportFile = { fileLauncher.launch(arrayOf("text/csv", "text/plain", "*/*")) }
                )
            }
        }
    }

    if (state is ImportState.Preview) {
        val preview = state as ImportState.Preview
        PreviewDialog(
            cards = preview.cards,
            source = preview.source,
            onConfirm = { vm.importDeck() },
            onDismiss = { vm.reset() }
        )
    }

    if (state is ImportState.Parsing || state is ImportState.Importing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text(if (state is ImportState.Importing) "Importing..." else "Parsing...")
            }
        }
    }
}

@Composable
private fun UrlImportSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onFetchUrl: () -> Unit,
    onParseText: () -> Unit,
    onImportFile: () -> Unit,
    urlPlaceholder: String,
    textLabel: String
) {
    Column {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            label = { Text(textLabel) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            placeholder = { Text("Paste URL\nor decklist text") }
        )
        Spacer(Modifier.height(8.dp))

        val isUrl = inputText.isNotBlank() && Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(inputText)
        Button(
            onClick = { if (isUrl) onFetchUrl() else onParseText() },
            modifier = Modifier.fillMaxWidth(),
            enabled = inputText.isNotBlank()
        ) {
            Text(if (isUrl) "Fetch from URL" else "Parse Decklist")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onImportFile, modifier = Modifier.fillMaxWidth()) {
            Text("Or choose file (CSV/TXT)")
        }
    }
}

@Composable
private fun TextFileImportSection(
    source: ImportSource,
    inputText: String,
    onInputChange: (String) -> Unit,
    onImport: () -> Unit,
    onImportFile: () -> Unit
) {
    Column {
        Text(
            when (source) {
                ImportSource.MTG_GOLDFISH -> "Paste CSV or text export from MTG Goldfish"
                ImportSource.TAPPED_OUT -> "Paste CSV or .dck text from TappedOut"
                else -> "Paste decklist"
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            label = { Text("Decklist") },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth(),
            enabled = inputText.isNotBlank()
        ) {
            Text("Parse Decklist")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onImportFile, modifier = Modifier.fillMaxWidth()) {
            Text("Or choose file (CSV/TXT)")
        }
    }
}

@Composable
private fun PreviewDialog(
    cards: List<com.gitlab.abelnightroad.data.MetaDeckCard>,
    source: ImportSource,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preview: ${cards.size} cards from ${source.label}") },
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

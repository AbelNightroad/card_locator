package com.gitlab.abelnightroad.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.db.DeckEntity
import com.gitlab.abelnightroad.ui.components.ScryfallAsyncImage
import com.gitlab.abelnightroad.data.ScryfallImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun DecksScreen(
    deckRepository: DeckRepository,
    initialFormat: String? = null,
    onBack: () -> Unit,
    onDeckClick: (Long, String) -> Unit,
    onImport: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    val decksVm: DecksViewModel = viewModel { DecksViewModel(deckRepository) }
    val formatCounts by decksVm.formatCounts.collectAsState()
    val decks by decksVm.decks.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    var deleteTargetDeck by remember { mutableStateOf<DeckEntity?>(null) }
    var cloneTargetDeck by remember { mutableStateOf<DeckEntity?>(null) }
    var renameTargetDeck by remember { mutableStateOf<DeckEntity?>(null) }
    var deleteTargetFormat by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialFormat) {
        if (initialFormat != null) decksVm.selectFormat(initialFormat)
    }

    if (selectedFormat != null) {
        Scaffold(
            bottomBar = bottomBar,
            topBar = {
                TopAppBar(
                    title = { Text(selectedFormat!!) },
                    navigationIcon = { IconButton(onClick = { selectedFormat = null }) { Text("\u2039") } }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(FontAwesomeIcons.Solid.Plus, "New Deck", Modifier.size(24.dp))
                }
            }
        ) { padding ->
            if (decks.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No decks in this format.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    Modifier.padding(padding).padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(decks, key = { it.id }) { deck ->
                        DeckGridCard(
                            deck = deck,
                            deckRepository = deckRepository,
                            onClick = { onDeckClick(deck.id, selectedFormat ?: deck.format) },
                            onDelete = { deleteTargetDeck = deck },
                            onClone = { cloneTargetDeck = deck },
                            onRename = { renameTargetDeck = deck }
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            bottomBar = bottomBar,
            topBar = {
                TopAppBar(
                    title = { Text("Decks") },
                    navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } },
                    actions = {
                        IconButton(onClick = onImport) {
                            Icon(FontAwesomeIcons.Solid.Download, "Import Deck", Modifier.size(24.dp))
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(FontAwesomeIcons.Solid.Plus, "New Deck", Modifier.size(24.dp))
                }
            }
        ) { padding ->
            if (formatCounts.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No decks yet. Tap + to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    Modifier.padding(padding).padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(formatCounts, key = { _, it -> it.format }) { index, fc ->
                        var showFormatMenu by remember { mutableStateOf(false) }
                        val formatColor = formatPalette[index % formatPalette.size]
                        Card(
                            Modifier.fillMaxWidth().combinedClickable(
                                onClick = {
                                    decksVm.selectFormat(fc.format)
                                    selectedFormat = fc.format
                                },
                                onLongClick = { showFormatMenu = true }
                            ),
                            colors = CardDefaults.cardColors(containerColor = formatColor.bg),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(fc.format, style = MaterialTheme.typography.titleMedium,
                                    color = formatColor.fg,
                                    textAlign = TextAlign.Center)
                                Spacer(Modifier.height(4.dp))
                                Text("${fc.deckCount} decks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = formatColor.fg.copy(alpha = 0.7f))
                            }
                            DropdownMenu(expanded = showFormatMenu, onDismissRequest = { showFormatMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Delete format", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showFormatMenu = false; deleteTargetFormat = fc.format },
                                    leadingIcon = { Icon(FontAwesomeIcons.Solid.Trash, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, format ->
                decksVm.createDeck(name, format) { deckId ->
                    showCreateDialog = false
                    onDeckClick(deckId, format)
                }
            }
        )
    }

    deleteTargetDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { deleteTargetDeck = null },
            title = { Text("Delete deck \"${deck.name}\"?") },
            text = { Text("This will permanently delete the deck and all its cards.") },
            confirmButton = {
                TextButton(onClick = {
                    decksVm.deleteDeck(deck)
                    deleteTargetDeck = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetDeck = null }) { Text("Cancel") }
            }
        )
    }

    cloneTargetDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { cloneTargetDeck = null },
            title = { Text("Clone deck \"${deck.name}\"?") },
            text = { Text("Creates a copy of this deck with all its cards.") },
            confirmButton = {
                TextButton(onClick = {
                    decksVm.cloneDeck(deck.id) { newId ->
                        cloneTargetDeck = null
                        onDeckClick(newId, deck.format)
                    }
                }) { Text("Clone") }
            },
            dismissButton = {
                TextButton(onClick = { cloneTargetDeck = null }) { Text("Cancel") }
            }
        )
    }

    renameTargetDeck?.let { deck ->
        var newName by remember(deck.id) { mutableStateOf(deck.name) }
        AlertDialog(
            onDismissRequest = { renameTargetDeck = null },
            title = { Text("Rename deck") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Deck name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        decksVm.renameDeck(deck.id, newName.trim())
                        renameTargetDeck = null
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetDeck = null }) { Text("Cancel") }
            }
        )
    }

    deleteTargetFormat?.let { format ->
        AlertDialog(
            onDismissRequest = { deleteTargetFormat = null },
            title = { Text("Delete format \"$format\"?") },
            text = { Text("All decks in this format will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    decksVm.deleteDecksByFormat(format) { deleteTargetFormat = null; selectedFormat = null }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetFormat = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckGridCard(
    deck: DeckEntity,
    deckRepository: DeckRepository,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit,
    onRename: () -> Unit
) {
    val cardCount by deckRepository.cardCountFlow(deck.id).collectAsState(initial = 0)
    var showMenu by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true }
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(5f / 3f),
                contentAlignment = Alignment.Center
            ) {
                if (deck.coverScryfallId != null) {
                    ScryfallAsyncImage(
                        url = ScryfallImage.artCrop(deck.coverScryfallId),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = deck.name
                    )
                } else {
                    Text("\u2660", style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                Spacer(Modifier.height(6.dp))
                Text(deck.name, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, maxLines = 2)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(deck.format, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("$cardCount cards", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Clone") },
                onClick = { showMenu = false; onClone() },
                leadingIcon = { Icon(FontAwesomeIcons.Solid.Copy, null, Modifier.size(24.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = { showMenu = false; onRename() },
                leadingIcon = { Icon(FontAwesomeIcons.Solid.PenToSquare, null, Modifier.size(24.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(FontAwesomeIcons.Solid.Trash, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

@Composable
private fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, format: String) -> Unit
) {
    var deckName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("Standard") }
    var formatExpanded by remember { mutableStateOf(false) }
    val scopeFormats = FORMATS.filter { it != "All" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Deck") },
        text = {
            Column {
                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    label = { Text("Deck name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedButton(onClick = { formatExpanded = true }) {
                        Text("Format: $selectedFormat")
                    }
                    DropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                        scopeFormats.forEach { format ->
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (deckName.isNotBlank()) onCreate(deckName, selectedFormat) },
                enabled = deckName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private data class FormatColor(val bg: Color, val fg: Color)

private val formatPalette = listOf(
    FormatColor(Color(0xFFD7B7A3), Color(0xFF3A2E26)),
    FormatColor(Color(0xFFB7C9A8), Color(0xFF2C3A26)),
    FormatColor(Color(0xFFA8C3C9), Color(0xFF26343A)),
    FormatColor(Color(0xFFC3B7D7), Color(0xFF322A3A)),
    FormatColor(Color(0xFFE3C9A3), Color(0xFF3A3026)),
    FormatColor(Color(0xFFD7A8B7), Color(0xFF3A2630)),
    FormatColor(Color(0xFFA3B7D7), Color(0xFF262E3A)),
    FormatColor(Color(0xFFC9CBA3), Color(0xFF343626)),
    FormatColor(Color(0xFFA3D7C3), Color(0xFF263A33)),
    FormatColor(Color(0xFFD7C3A3), Color(0xFF3A3326)),
    FormatColor(Color(0xFFB7A3D7), Color(0xFF2E263A)),
    FormatColor(Color(0xFFC9A3A3), Color(0xFF3A2E2E))
)

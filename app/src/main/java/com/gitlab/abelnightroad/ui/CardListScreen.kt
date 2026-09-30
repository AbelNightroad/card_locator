package com.gitlab.abelnightroad.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.ui.components.ImportResult
import com.gitlab.abelnightroad.ui.components.ImportResultDialog
import com.gitlab.abelnightroad.ui.components.QuantityStepper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardListScreen(
    repository: CardRepository,
    scryfall: ScryfallRepository,
    tag: String,
    onBack: () -> Unit,
    onCardClick: (CardSearchResult) -> Unit
) {
    val vm: CardListViewModel = viewModel { CardListViewModel(repository, scryfall) }
    val cards by vm.cardsByTag(tag).collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }

    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportUri = null
            },
            title = { Text("Import into \"$tag\"") },
            text = {
                Column {
                    Text("Cards from the chosen file are added to this tag.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            importFileLauncher.launch(
                                arrayOf("text/plain", "text/csv", "*/*")
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            pendingImportUri?.lastPathSegment
                                ?.let { "File: $it" } ?: "Choose file (TXT, CSV)"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pendingImportUri != null,
                    onClick = {
                        val uri = pendingImportUri
                        showImportDialog = false
                        pendingImportUri = null
                        uri?.let { picked ->
                            scope.launch {
                                try {
                                    val text = context.contentResolver.openInputStream(picked)
                                        ?.use { it.bufferedReader(Charsets.UTF_8).readText() }
                                        ?: throw Exception("Could not read file")
                                    importResult = vm.importIntoTag(tag, text)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Import failed: ${e.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pendingImportUri = null
                }) { Text("Cancel") }
            }
        )
    }

    importResult?.let { ImportResultDialog(result = it, onDismiss = { importResult = null }) }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Cards") },
            text = { Text("Choose export format:") },
            confirmButton = {
                TextButton(onClick = {
                    vm.exportToClipboard(tag, cards, context)
                    showExportDialog = false
                }) { Text("Copy to Clipboard") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.exportToTxt(tag, cards, context)
                    showExportDialog = false
                }) { Text("Save to File") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("\u2039") }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(FontAwesomeIcons.Solid.Upload, "Import into Tag", Modifier.size(24.dp))
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(FontAwesomeIcons.Solid.Download, "Export Cards", Modifier.size(24.dp))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 8.dp)) {
            items(cards, key = { it.id }) { card ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            vm.deleteCard(card.id)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Card(
                                Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                                    Text("\u2715  Delete",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 20.dp))
                                }
                            }
                        }
                    },
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true
                ) {
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable { onCardClick(card) },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(card.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${card.setName} \u00b7 ${card.rarity}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            QuantityStepper(
                                quantity = card.quantity,
                                onDecrease = { vm.decrementQuantity(card.id) },
                                onIncrease = { vm.incrementQuantity(card.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

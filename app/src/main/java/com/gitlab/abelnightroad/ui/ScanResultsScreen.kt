package com.gitlab.abelnightroad.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Xmark
import com.gitlab.abelnightroad.data.ScryfallImage
import com.gitlab.abelnightroad.db.ScannedCardEntity
import com.gitlab.abelnightroad.ui.components.ScryfallAsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScanResultsScreen(
    viewModel: ScanViewModel,
    sessionId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cards by viewModel.getScannedCards(sessionId).collectAsState(initial = emptyList())
    val isProcessing by viewModel.isProcessing.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val addedCount by viewModel.addedCount.collectAsState()

    var showTagDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var tagSuggestions by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(lastError) {
        lastError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanned Cards") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FontAwesomeIcons.Solid.Xmark, contentDescription = "Close", modifier = Modifier.size(24.dp))
                    }
                },
                actions = {
                    if (cards.isNotEmpty()) {
                        TextButton(onClick = { showTagDialog = true }) {
                            Text("Add to Collection")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (cards.isEmpty() && !isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No cards scanned yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Go back and capture a card", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isProcessing) {
                    item {
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Processing card...")
                            }
                        }
                    }
                }

                items(cards, key = { it.id }) { card ->
                    ScannedCardRow(
                        card = card,
                        onDelete = { viewModel.deleteCard(card.id) }
                    )
                }
            }
        }
    }

    if (showTagDialog) {
        TagPickerDialog(
            tagInput = tagInput,
            onTagInputChange = { tagInput = it },
            onConfirm = {
                viewModel.addToCollection(tagInput)
                Toast.makeText(context, "Added ${cards.size} cards to collection", Toast.LENGTH_SHORT).show()
                showTagDialog = false
                tagInput = ""
            },
            onDismiss = { showTagDialog = false }
        )
    }
}

@Composable
private fun ScannedCardRow(
    card: ScannedCardEntity,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScryfallAsyncImage(
                url = ScryfallImage.normal(card.scryfallId),
                modifier = Modifier.size(48.dp),
                contentDescription = card.name
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    card.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${card.setName} \u00b7 ${card.rarity} \u00b7 ${card.language.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (card.manaCost.isNotBlank()) {
                    Text(
                        card.manaCost,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Text("\u2715", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TagPickerDialog(
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Collection") },
        text = {
            Column {
                Text("Enter a tag (storage location) for these cards:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = onTagInputChange,
                    label = { Text("Tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = tagInput.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

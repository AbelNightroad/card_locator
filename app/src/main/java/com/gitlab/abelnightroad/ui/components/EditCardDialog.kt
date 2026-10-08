package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.gitlab.abelnightroad.data.CardConditions
import com.gitlab.abelnightroad.data.PrintingInfo
import com.gitlab.abelnightroad.db.CardEntity
import kotlinx.coroutines.launch

val CARD_FINISHES = listOf("normal", "foil", "etched", "etched foil")

fun displayFinish(finish: String): String = when (finish.lowercase()) {
    "normal" -> "Normal"
    "foil" -> "Foil"
    "etched" -> "Etched"
    "etched foil", "foil etched" -> "Etched Foil"
    else -> finish
}

@Composable
fun EditCardDialog(
    card: CardEntity,
    fetchPrintings: suspend (String) -> List<PrintingInfo>,
    onSave: (condition: String, foil: String) -> Unit,
    onPickPrinting: (PrintingInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var condition by remember(card.id) { mutableStateOf(card.condition) }
    var finish by remember(card.id) { mutableStateOf(card.foil) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var printings by remember { mutableStateOf<List<PrintingInfo>?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val conditions = remember(card.condition) {
        if (card.condition in CardConditions.ALL) {
            CardConditions.ALL
        } else {
            listOf(card.condition) + CardConditions.ALL
        }
    }
    val finishes = remember(card.foil) {
        if (card.foil in CARD_FINISHES) CARD_FINISHES else listOf(card.foil) + CARD_FINISHES
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit card") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Set", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${card.setName} (${card.setCode}) #${card.collectorNumber}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = {
                        loading = true
                        error = null
                        scope.launch {
                            try {
                                printings = fetchPrintings(card.name)
                                showPicker = true
                            } catch (e: Exception) {
                                error = e.message ?: "Could not fetch printings"
                            } finally {
                                loading = false
                            }
                        }
                    }) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Change printings…")
                        }
                    }
                }
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(12.dp))
                DropdownField(
                    label = "Condition",
                    value = CardConditions.displayName(condition),
                    options = conditions.map { it to CardConditions.displayName(it) },
                    onSelect = { condition = it }
                )

                Spacer(Modifier.height(12.dp))
                DropdownField(
                    label = "Finish",
                    value = displayFinish(finish),
                    options = finishes.map { it to displayFinish(it) },
                    onSelect = { finish = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(condition, finish)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showPicker) {
        PrintingsPickerDialog(
            printings = printings.orEmpty(),
            onPick = { printing ->
                onPickPrinting(printing)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Text(label, style = MaterialTheme.typography.labelMedium)
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(value)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (raw, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onSelect(raw)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PrintingsPickerDialog(
    printings: List<PrintingInfo>,
    onPick: (PrintingInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose printing") },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(printings, key = { it.id }) { printing ->
                    val price = printing.priceUsd?.let { " · $" + "%.2f".format(it) } ?: ""
                    TextButton(onClick = { onPick(printing) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${printing.setName} (${printing.setCode.uppercase()}) " +
                                "#${printing.collectorNumber} · ${printing.rarity}$price",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

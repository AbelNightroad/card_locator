package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A row that could not be imported, with the reason shown to the user. */
data class SkippedEntry(val row: String, val reason: String)

/**
 * Outcome of a collection import: how many rows were stored, which rows were
 * rejected (and why), and how many rows were stored without full reference
 * details (card unknown to the local Scryfall table).
 */
data class ImportResult(
    val imported: Int,
    val skippedRows: List<SkippedEntry>,
    val unresolved: Int = 0
)

/**
 * "Import Complete" dialog with the skipped-row breakdown, used by every
 * collection import (Settings and tag detail screens).
 */
@Composable
fun ImportResultDialog(result: ImportResult, onDismiss: () -> Unit) {
    var showSkipped by remember { mutableStateOf(false) }
    val skipped = result.skippedRows.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Complete") },
        text = {
            Column {
                Text("Imported ${result.imported} cards, skipped $skipped")
                if (result.unresolved > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${result.unresolved} imported without full details (not in reference data)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (skipped > 0) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showSkipped = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("View Skipped Cards ($skipped)") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )

    if (showSkipped) {
        SkippedRowsDialog(rows = result.skippedRows, onDismiss = { showSkipped = false })
    }
}

/** Scrollable list of rejected rows, each with its failure reason. */
@Composable
fun SkippedRowsDialog(rows: List<SkippedEntry>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skipped Cards (${rows.size})") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(rows) { skipped ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = skipped.row.take(80).ifBlank { "(empty row)" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = skipped.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

package com.gitlab.abelnightroad.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import compose.icons.Octicons
import compose.icons.octicons.*
import com.gitlab.abelnightroad.data.BackupStore
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.CsvImport
import com.gitlab.abelnightroad.ui.theme.FONTS
import com.gitlab.abelnightroad.ui.theme.THEMES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    viewModel: MainViewModel,
    repository: CardRepository,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeId by viewModel.themeId.collectAsState(initial = "nord")
    val fontId by viewModel.fontId.collectAsState(initial = "roboto")
    val hapticFeedback by viewModel.hapticFeedback.collectAsState(initial = true)
    val scryfallUpdatedAt by viewModel.scryfallUpdatedAt.collectAsState(initial = null)
    var expanded by remember { mutableStateOf(false) }
    var fontExpanded by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importTag by remember { mutableStateOf("MegaBox-01") }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val cards = repository.getAllCards()
                    val json = BackupStore.encode(cards)
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        backupStatus = "Exported ${cards.size} cards"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        backupStatus = "Export failed: ${e.message}"
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val text = context.contentResolver.openInputStream(it)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: throw Exception("Could not read file")
                    val cards = BackupStore.decode(text)
                    repository.replaceAll(cards)
                    withContext(Dispatchers.Main) {
                        backupStatus = "Restored ${cards.size} cards"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        backupStatus = "Restore failed: ${e.message}"
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Current Theme", style = MaterialTheme.typography.bodyLarge)
                        Box {
                            OutlinedButton(onClick = { expanded = true }) {
                                Text(THEMES.first { it.id == themeId }.label)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                THEMES.forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme.label) },
                                        onClick = {
                                            viewModel.setTheme(theme.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Font", style = MaterialTheme.typography.bodyLarge)
                        Box {
                            OutlinedButton(onClick = { fontExpanded = true }) {
                                Text(FONTS.first { it.id == fontId }.label)
                            }
                            DropdownMenu(expanded = fontExpanded, onDismissRequest = { fontExpanded = false }) {
                                FONTS.forEach { font ->
                                    DropdownMenuItem(
                                        text = { Text(font.label) },
                                        onClick = {
                                            viewModel.setFont(font.id)
                                            fontExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Card Scan", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Haptic feedback on capture", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = hapticFeedback,
                            onCheckedChange = { viewModel.setHapticFeedback(it) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Backup & Restore", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { exportLauncher.launch("card_tracker_backup.json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export collection as JSON")
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore collection from JSON")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import from 3rd-Party")
                    }
                    if (backupStatus.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(backupStatus, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            ScryfallCard(scryfallUpdatedAt = scryfallUpdatedAt)

            Spacer(Modifier.height(12.dp))
            AboutCard()
        }
    }

    if (showImportDialog) {
        ImportDialog(
            importTag = importTag,
            onTagChange = { importTag = it },
            onChooseFile = {
                importFileLauncher.launch(arrayOf("text/csv", "application/json", "text/plain"))
            },
            selectedFileName = pendingImportUri?.lastPathSegment,
            onImport = {
                pendingImportUri?.let { uri ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            val path = uri.lastPathSegment?.lowercase() ?: ""
                            val input = context.contentResolver.openInputStream(uri)
                                ?: throw Exception("Could not read file")
                            val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
                            val importedCount = if (path.endsWith(".json")) {
                                val cards = BackupStore.decodeToTag(text, importTag)
                                repository.insertAll(cards)
                                cards.size
                            } else {
                                val result = CsvImport.parse(text, importTag)
                                repository.insertAll(result.cards)
                                result.cards.size
                            }
                            withContext(Dispatchers.Main) {
                                backupStatus = "Imported $importedCount cards"
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                backupStatus = "Import failed: ${e.message}"
                            }
                        }
                    }
                    showImportDialog = false
                    pendingImportUri = null
                }
            },
            onDismiss = {
                showImportDialog = false
                pendingImportUri = null
            }
        )
    }
}

@Composable
private fun ScryfallCard(scryfallUpdatedAt: String?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scryfall Reference Data", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            if (scryfallUpdatedAt != null) {
                Text(
                    "Last update: ${scryfallUpdatedAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Not yet synced — will update automatically on launch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Card images provided by Scryfall.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Octicons.Info24, null, Modifier.padding(end = 8.dp))
                Text("About", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "MtG Card Tracker helps Magic: The Gathering collectors find cards across " +
                    "their boxes and binders.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ImportDialog(
    importTag: String,
    onTagChange: (String) -> Unit,
    onChooseFile: () -> Unit,
    selectedFileName: String?,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from 3rd-Party") },
        text = {
            Column {
                OutlinedTextField(
                    value = importTag,
                    onValueChange = onTagChange,
                    label = { Text("Tag for imported cards") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onChooseFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedFileName != null) "File: $selectedFileName" else "Choose file (CSV, JSON, TXT)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onImport,
                enabled = selectedFileName != null
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

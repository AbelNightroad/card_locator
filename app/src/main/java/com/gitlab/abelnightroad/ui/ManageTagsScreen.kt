package com.gitlab.abelnightroad.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import com.gitlab.abelnightroad.data.CardRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageTagsScreen(
    repository: CardRepository,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val vm: ManageTagsViewModel = viewModel { ManageTagsViewModel(repository) }
    val tags by vm.tags.collectAsState()
    var deleteTag by remember { mutableStateOf<String?>(null) }
    var renameTag by remember { mutableStateOf<String?>(null) }
    var newTagName by remember { mutableStateOf("") }
    var addTagName by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = { Text("Manage Tags") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(FontAwesomeIcons.Solid.Plus, "New Tag", Modifier.size(24.dp))
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            items(tags, key = { it.tag }) { tagCount ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tagCount.tag, style = MaterialTheme.typography.titleSmall)
                            Text("${tagCount.cardCount} cards",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(
                            onClick = {
                                newTagName = tagCount.tag
                                renameTag = tagCount.tag
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("\u270E", fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { deleteTag = tagCount.tag },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(FontAwesomeIcons.Solid.Trash, null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    deleteTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleteTag = null },
            title = { Text("Delete tag \"$tag\"?") },
            text = { Text("All ${tags.firstOrNull { it.tag == tag }?.cardCount ?: 0} cards in this tag will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTag(tag)
                    deleteTag = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTag = null }) { Text("Cancel") }
            }
        )
    }

    renameTag?.let { oldTag ->
        AlertDialog(
            onDismissRequest = { renameTag = null },
            title = { Text("Rename tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagName.isNotBlank() && newTagName != oldTag) {
                        vm.renameTag(oldTag, newTagName)
                    }
                    renameTag = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameTag = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Tag") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = addTagName,
                            onValueChange = { addTagName = it },
                            label = { Text("Tag name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            val letter = ('A'..'Z').random()
                            val digit = ('0'..'9').random()
                            addTagName = "Box-$letter$digit"
                        }) {
                            Text("Random")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (addTagName.isNotBlank()) {
                        vm.createTag(addTagName)
                    }
                    showAddDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

package com.gitlab.abelnightroad.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.ScryfallRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualAddScreen(
    repository: CardRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit,
    initialTag: String = ""
) {
    val context = LocalContext.current
    val viewModel: ManualAddViewModel = viewModel { ManualAddViewModel(repository, scryfall) }
    val query by viewModel.query.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState(initial = emptyList())
    val selected by viewModel.selected.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val foil by viewModel.foil.collectAsState()
    val condition by viewModel.condition.collectAsState()
    val tag by viewModel.tag.collectAsState()
    val tagCounts by repository.tagCounts.collectAsState(initial = emptyList())

    LaunchedEffect(initialTag) {
        if (initialTag.isNotBlank() && tag.isBlank()) {
            viewModel.tag.value = initialTag
        }
    }

    LaunchedEffect(saved) {
        if (saved) {
            Toast.makeText(context, "Card added", Toast.LENGTH_SHORT).show()
            viewModel.resetForm()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Card") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                label = { Text("Card name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(FontAwesomeIcons.Solid.Search, null, modifier = Modifier.size(24.dp)) }
            )
            if (selected == null && query.length >= 2) {
                LazyColumn(Modifier.fillMaxWidth().height(200.dp)) {
                    items(suggestions) { card ->
                        ListItem(
                            headlineContent = { Text(card.name) },
                            supportingContent = { Text("${card.setName} \u00b7 ${card.rarity}") },
                            modifier = Modifier.clickable { viewModel.select(card) }
                        )
                    }
                }
            }

            selected?.let { card ->
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(card.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${card.setName} (${card.setCode.uppercase()}) \u00b7 #${card.collectorNumber} \u00b7 ${card.rarity}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (card.manaCost.isNotBlank())
                            Text("Mana: ${card.manaCost}", style = MaterialTheme.typography.bodySmall)
                        if (card.typeLine.isNotBlank())
                            Text(card.typeLine, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = quantity, onValueChange = { viewModel.quantity.value = it },
                label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = foil, onValueChange = { viewModel.foil.value = it },
                label = { Text("Foil (blank = normal)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true
            )
            OutlinedTextField(
                value = condition, onValueChange = { viewModel.condition.value = it },
                label = { Text("Condition") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true
            )
            OutlinedTextField(
                value = tag, onValueChange = { viewModel.tag.value = it },
                label = { Text("Tag (storage location)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true
            )
            val tagSuggestions = remember(tag, tagCounts) {
                tagCounts.map { it.tag }.filter { it.contains(tag, ignoreCase = true) }
                    .filter { it != tag }.take(5)
            }
            if (tagSuggestions.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        tagSuggestions.forEach { suggestion ->
                            ListItem(
                                headlineContent = { Text(suggestion) },
                                modifier = Modifier.clickable { viewModel.tag.value = suggestion }
                            )
                        }
                    }
                }
            }
            Button(
                onClick = viewModel::save,
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Add Card")
            }
        }
    }
}

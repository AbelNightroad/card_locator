package com.gitlab.abelnightroad.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.Octicons
import compose.icons.octicons.*
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.ui.components.QuantityStepper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardListScreen(
    repository: CardRepository,
    scryfall: ScryfallRepository,
    tag: String,
    onBack: () -> Unit,
    onCardClick: (CardSearchResult) -> Unit
) {
    val vm: CardListViewModel = viewModel { CardListViewModel(repository) }
    val cards by vm.cardsByTag(tag).collectAsState()
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("\u2039") }
                },
                actions = {
                    IconButton(onClick = { vm.exportToTxt(tag, cards, context) }) {
                        Icon(Octicons.Download24, "Export to TXT")
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

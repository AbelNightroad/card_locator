package com.gitlab.abelnightroad.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.Octicons
import compose.icons.octicons.*
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.db.MultiCopyCard
import com.gitlab.abelnightroad.db.TagCount
import com.gitlab.abelnightroad.ui.components.SearchFilterChips
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
    viewModel: MainViewModel,
    onTagClick: (String) -> Unit,
    onCardClick: (CardSearchResult) -> Unit,
    onAddCard: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val tags by viewModel.tagCounts.collectAsState(initial = emptyList())
    val search by viewModel.search.collectAsState()
    val multiOnly by viewModel.multiCopyOnly.collectAsState()
    val multiCards by viewModel.multiCopyCards.collectAsState(initial = emptyList())
    val dark by viewModel.darkMode.collectAsState(initial = true)
    val colorFilter by viewModel.colorFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val rarityFilter by viewModel.rarityFilter.collectAsState()

    val searchResults: List<CardSearchResult> by if (search.isBlank()) {
        flowOf(emptyList<CardSearchResult>())
    } else {
        viewModel.advancedSearchFlow(search, colorFilter, typeFilter, rarityFilter)
    }.collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = { Text("Card Tracker") },
                actions = {
                    IconButton(onClick = { viewModel.setDarkMode(!dark) }) {
                        Icon(
                            if (dark) Octicons.Sun24 else Octicons.Moon24,
                            if (dark) "Light mode" else "Dark mode"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCard) {
                Icon(Octicons.Plus24, "Add Card")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = viewModel::setSearch,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search cards by name") },
                    leadingIcon = { Icon(Octicons.Search24, null) },
                    singleLine = true
                )
                if (search.isNotBlank()) {
                    IconButton(onClick = viewModel::clearSearch) {
                        Icon(Octicons.X24, "Clear search")
                    }
                }
                IconButton(
                    onClick = viewModel::toggleMultiCopyOnly,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        Octicons.Filter24,
                        "More than 4 copies",
                        tint = if (multiOnly) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (search.isNotBlank()) {
                SearchFilterChips(
                    colorFilter = colorFilter,
                    typeFilter = typeFilter,
                    rarityFilter = rarityFilter,
                    onColorClick = viewModel::setColorFilter,
                    onTypeClick = viewModel::setTypeFilter,
                    onRarityClick = viewModel::setRarityFilter
                )
            }

            when {
                search.isNotBlank() -> CardResultList(searchResults, onCardClick)
                multiOnly -> CardResultList(
                    multiCards.map {
                        CardSearchResult(
                            0, it.name, it.setCode, it.setName, "", "", "",
                            it.totalQuantity, it.scryfallId, "collection"
                        )
                    }, onCardClick
                )
                else -> TagList(tags, onTagClick)
            }
        }
    }
}

@Composable
private fun TagList(tags: List<TagCount>, onTagClick: (String) -> Unit) {
    if (tags.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards yet. Tap Tags to import a CSV.")
        }
        return
    }
    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
        items(tags) { tag -> TagRow(tag, onTagClick) }
    }
}

@Composable
private fun TagRow(tagCount: TagCount, onClick: (String) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick(tagCount.tag) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tagCount.tag, style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${tagCount.cardCount} cards", style = MaterialTheme.typography.bodyMedium)
                Text("\$${"%.2f".format(tagCount.totalValue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CardResultList(
    cards: List<CardSearchResult>,
    onCardClick: (CardSearchResult) -> Unit
) {
    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
        items(cards) { card ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clickable { onCardClick(card) },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(card.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${card.setName} \u00b7 ${card.rarity} \u00b7 x${card.quantity}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(card.tag, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

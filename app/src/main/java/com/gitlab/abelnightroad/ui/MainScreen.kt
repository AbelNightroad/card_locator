package com.gitlab.abelnightroad.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.db.MultiCopyCard
import com.gitlab.abelnightroad.db.TagCount
import com.gitlab.abelnightroad.ui.components.CreateTagDialog
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
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    var showCreateTag by remember { mutableStateOf(false) }

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
                            if (dark) FontAwesomeIcons.Solid.Sun else FontAwesomeIcons.Solid.Moon,
                            if (dark) "Light mode" else "Dark mode",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        SpeedDialOption(
                            label = "Card",
                            icon = FontAwesomeIcons.Solid.PenToSquare,
                            contentDescription = "Add Card",
                            onClick = {
                                fabExpanded = false
                                onAddCard()
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        SpeedDialOption(
                            label = "Tag",
                            icon = FontAwesomeIcons.Solid.Tag,
                            contentDescription = "New Tag",
                            onClick = {
                                fabExpanded = false
                                showCreateTag = true
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                val rotation by animateFloatAsState(
                    targetValue = if (fabExpanded) 45f else 0f,
                    label = "fabRotation"
                )
                FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                    Icon(
                        FontAwesomeIcons.Solid.Plus,
                        contentDescription = if (fabExpanded) "Close" else "Add",
                        modifier = Modifier.size(24.dp).rotate(rotation)
                    )
                }
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
                    leadingIcon = { Icon(FontAwesomeIcons.Solid.Search, null, Modifier.size(24.dp)) },
                    singleLine = true
                )
                if (search.isNotBlank()) {
                    IconButton(onClick = viewModel::clearSearch) {
                        Icon(FontAwesomeIcons.Solid.Xmark, "Clear search", Modifier.size(24.dp))
                    }
                }
                IconButton(
                    onClick = viewModel::toggleMultiCopyOnly,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        FontAwesomeIcons.Solid.Filter,
                        "More than 4 copies",
                        modifier = Modifier.size(24.dp),
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

    if (showCreateTag) {
        CreateTagDialog(
            onDismiss = { showCreateTag = false },
            onCreate = viewModel::createTag
        )
    }
}

@Composable
private fun SpeedDialOption(
    label: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        SmallFloatingActionButton(onClick = onClick) {
            Icon(icon, contentDescription, Modifier.size(20.dp))
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

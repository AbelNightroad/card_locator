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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.ui.components.QuantityStepper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckViewScreen(
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    deckId: Long,
    onBack: () -> Unit,
    onCardClick: (String) -> Unit
) {
    val vm: DeckViewViewModel = viewModel(key = "deck-$deckId") {
        DeckViewViewModel(deckRepository, scryfall, deckId)
    }
    val deckWithCards by vm.deckWithCards.collectAsState()
    var showAddCardDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val removedCard by vm.removedCard.collectAsState()

    LaunchedEffect(removedCard) {
        removedCard?.let {
            val result = snackbarHostState.showSnackbar(
                message = "${it.card.cardName} removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                vm.undoRemove()
            } else {
                vm.clearRemovedCard()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(deckWithCards?.deck?.name ?: "Deck") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } },
                actions = {
                    IconButton(onClick = {
                        val cards = deckWithCards?.cards
                        val name = deckWithCards?.deck?.name ?: "deck"
                        if (!cards.isNullOrEmpty()) vm.exportDeck(context, name, cards)
                    }) {
                        Icon(FontAwesomeIcons.Solid.ShareNodes, "Export Deck", Modifier.size(24.dp))
                    }
                    IconButton(onClick = { showAddCardDialog = true }) {
                        Icon(FontAwesomeIcons.Solid.Plus, "Add Card to Deck", Modifier.size(24.dp))
                    }
                }
            )
        }
    ) { padding ->
        val cards = deckWithCards?.cards ?: emptyList()
        val pagerState = rememberPagerState(pageCount = { 2 })
        val selectedTab = pagerState.currentPage.coerceIn(0, 1)

        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                ) {
                    Text("Decklist", modifier = Modifier.padding(12.dp))
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                ) {
                    Text("Statistics", modifier = Modifier.padding(12.dp))
                }
            }
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> DecklistTab(cards, vm, onCardClick)
                    1 -> DeckStatisticsScreen(deckId, vm)
                }
            }
        }
    }

    if (showAddCardDialog) {
        AddCardToDeckDialog(
            vm = vm,
            format = deckWithCards?.deck?.format ?: "Standard",
            onDismiss = { showAddCardDialog = false }
        )
    }
}

@Composable
private fun DeckCardRow(
    card: com.gitlab.abelnightroad.db.DeckCardEntity,
    onQuantityChange: (Int) -> Unit,
    onCardClick: () -> Unit,
    onSetCover: () -> Unit,
    showCoverButton: Boolean = true
) {
    val context = LocalContext.current
    val conditionColor = when (card.condition) {
        "NM" -> MaterialTheme.colorScheme.primary
        "LP" -> MaterialTheme.colorScheme.tertiary
        "MP" -> MaterialTheme.colorScheme.secondary
        "HP" -> MaterialTheme.colorScheme.error
        "DM" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.cardName, style = MaterialTheme.typography.titleSmall)
                    if (card.condition != "NM") {
                        Spacer(Modifier.width(6.dp))
                        Text(card.condition,
                            style = MaterialTheme.typography.labelSmall,
                            color = conditionColor)
                    }
                }
                if (card.manaCost.isNotBlank()) {
                    Text(card.manaCost,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (card.priceUsd > 0) {
                Text("$${"%.2f".format(card.priceUsd * card.quantity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp))
            }
            QuantityStepper(
                quantity = card.quantity,
                onDecrease = { onQuantityChange(-1) },
                onIncrease = { onQuantityChange(1) }
            )
        }
        if (showCoverButton) {
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${card.rarity} \u00b7 ${card.setName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = {
                        onSetCover()
                        Toast.makeText(context, "Cover set", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(28.dp)
                ) { Text("Cover", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun AddCardToDeckDialog(
    vm: DeckViewViewModel,
    format: String = "Standard",
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val suggestions by if (query.length >= 2) {
        vm.autocomplete(query)
    } else {
        remember { flowOf(emptyList()) }
    }.collectAsState(initial = emptyList())
    var selectedCard by remember { mutableStateOf<com.gitlab.abelnightroad.db.ScryfallCardEntity?>(null) }
    var selectedSlot by remember { mutableStateOf("mainboard") }
    var selectedCondition by remember { mutableStateOf("NM") }
    val isCommander = format == "Commander"
    val slotOptions = if (isCommander) listOf("mainboard", "commander", "companion") else listOf("mainboard")

    val deckData by vm.deckWithCards.collectAsState()
    val hasCommander = deckData?.cards?.any { it.slot == "commander" } ?: false
    val hasCompanion = deckData?.cards?.any { it.slot == "companion" } ?: false
    val commanderColors = deckData?.cards?.find { it.slot == "commander" }?.colorIdentity ?: ""

    val colorConflict = if (selectedCard != null && isCommander && hasCommander && commanderColors.isNotBlank()) {
        !DeckRepository.isColorIdentityValid(selectedCard!!.colorIdentity, commanderColors)
    } else false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card to Deck") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; selectedCard = null },
                    label = { Text("Search card name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(FontAwesomeIcons.Solid.Search, null, Modifier.size(24.dp)) }
                )
                if (selectedCard == null && query.length >= 2) {
                    LazyColumn(Modifier.fillMaxWidth().height(200.dp)) {
                        items(suggestions) { card ->
                            ListItem(
                                headlineContent = { Text(card.name) },
                                supportingContent = { Text("${card.setName} \u00b7 ${card.rarity}") },
                                modifier = Modifier.clickable { selectedCard = card }
                            )
                        }
                    }
                }
                selectedCard?.let { card ->
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(card.name, style = MaterialTheme.typography.titleSmall)
                            Text("${card.setName} (${card.setCode}) \u00b7 #${card.collectorNumber}",
                                style = MaterialTheme.typography.bodySmall)
                            if (card.typeLine.isNotBlank())
                                Text(card.typeLine, style = MaterialTheme.typography.bodySmall)
                            if (card.colorIdentity.isNotBlank() && isCommander) {
                                Spacer(Modifier.height(4.dp))
                                Text("Color ID: ${card.colorIdentity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            if (colorConflict) {
                                Spacer(Modifier.height(4.dp))
                                Text("Color identity conflict \u2014 not within commander's (${commanderColors})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (slotOptions.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Slot:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        slotOptions.forEach { slot ->
                            FilterChip(
                                selected = selectedSlot == slot,
                                onClick = { selectedSlot = slot },
                                label = { Text(slot.replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Condition:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    com.gitlab.abelnightroad.data.CardConditions.ALL.forEach { cond ->
                        FilterChip(
                            selected = selectedCondition == cond,
                            onClick = { selectedCondition = cond },
                            label = { Text(cond, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCard?.let { card ->
                        vm.addCardToDeck(card, selectedSlot, hasCommander, hasCompanion, selectedCondition)
                    }
                    onDismiss()
                },
                enabled = selectedCard != null && !colorConflict
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private val MAINBOARD_TYPE_ORDER = listOf(
    "Creature", "Instant", "Sorcery", "Artifact", "Enchantment",
    "Planeswalker", "Battle", "Land", "Other"
)

@Composable
private fun DecklistTab(
    cards: List<com.gitlab.abelnightroad.db.DeckCardEntity>,
    vm: DeckViewViewModel,
    onCardClick: (String) -> Unit
) {
    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards in this deck yet. Tap + to add cards.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center)
        }
        return
    }
    val typeGroupsBySlot = cards.groupBy { it.slot }.mapValues { (_, slotCards) ->
        slotCards.groupBy { com.gitlab.abelnightroad.data.primaryType(it.typeLine) }
            .toList()
            .sortedBy { (type, _) ->
                MAINBOARD_TYPE_ORDER.indexOf(type).takeIf { it >= 0 } ?: MAINBOARD_TYPE_ORDER.size
            }
            .toMap()
    }
    val slotOrder = listOf("commander", "companion", "mainboard", "sideboard")
    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
        for (slot in slotOrder) {
            val typeGroups = typeGroupsBySlot[slot] ?: continue
            if (slot != "mainboard") {
                item {
                    Text(slot.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
            }
            if (slot == "sideboard") {
                val flatCards = typeGroups.values.flatten()
                items(flatCards, key = { it.id }) { card ->
                    DeckCardRow(
                        card = card,
                        onQuantityChange = { delta ->
                            if (delta < 0 && card.quantity <= 1) vm.removeCardWithUndo(card.id, cards)
                            else vm.updateCardQuantity(card.id, card.quantity + delta)
                        },
                        onCardClick = { onCardClick(card.scryfallId) },
                        onSetCover = { vm.setCover(card.scryfallId) },
                        showCoverButton = false
                    )
                }
            } else {
                for ((type, typeCards) in typeGroups) {
                    item {
                        Text(type,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    }
                    items(typeCards, key = { it.id }) { card ->
                        DeckCardRow(
                            card = card,
                            onQuantityChange = { delta ->
                                if (delta < 0 && card.quantity <= 1) vm.removeCardWithUndo(card.id, cards)
                                else vm.updateCardQuantity(card.id, card.quantity + delta)
                            },
                            onCardClick = { onCardClick(card.scryfallId) },
                            onSetCover = { vm.setCover(card.scryfallId) }
                        )
                    }
                }
            }
        }
    }
}

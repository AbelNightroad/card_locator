package com.github.abelnightroad.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.abelnightroad.R
import com.github.abelnightroad.data.CardRepository
import com.github.abelnightroad.data.ScryfallRepository
import com.github.abelnightroad.data.SettingsStore
import com.github.abelnightroad.db.CardSearchResult
import com.github.abelnightroad.db.MultiCopyCard
import com.github.abelnightroad.db.TagCount
import com.github.abelnightroad.ui.theme.THEMES
import kotlinx.coroutines.flow.flowOf

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    repository: CardRepository,
    scryfall: ScryfallRepository,
    settings: SettingsStore
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var selectedCard by remember { mutableStateOf<CardSearchResult?>(null) }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onImport = { screen = Screen.Import },
                    onAddCard = { screen = Screen.AddCard },
                    onSettings = { screen = Screen.Settings },
                    onMain = { screen = Screen.Main }
                )
            }
        }
    ) {
        when (val s = screen) {
            Screen.Main -> MainScreen(
                viewModel = mainViewModel,
                onTagClick = { screen = Screen.Cards(it) },
                onCardClick = { selectedCard = it }
            )
            is Screen.Cards -> CardListScreen(
                repository = repository,
                tag = s.tag,
                onBack = { screen = Screen.Main },
                onCardClick = { selectedCard = it }
            )
            Screen.Import -> ImportScreen(
                repository = repository,
                scryfall = scryfall,
                onBack = { screen = Screen.Main }
            )
            Screen.AddCard -> ManualAddScreen(
                repository = repository,
                scryfall = scryfall,
                onBack = { screen = Screen.Main },
                onSaved = { screen = Screen.Main }
            )
            Screen.Settings -> SettingsScreen(
                viewModel = mainViewModel,
                onBack = { screen = Screen.Main }
            )
        }
    }

    selectedCard?.let { card ->
        FullscreenOverlay(scryfallId = card.scryfallId, onDismiss = { selectedCard = null })
    }
}

sealed interface Screen {
    data object Main : Screen
    data class Cards(val tag: String) : Screen
    data object Import : Screen
    data object AddCard : Screen
    data object Settings : Screen
}

@Composable
private fun DrawerContent(
    onImport: () -> Unit,
    onAddCard: () -> Unit,
    onSettings: () -> Unit,
    onMain: () -> Unit
) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Text("Card Tracker", Modifier.padding(16.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Collection") },
            leadingContent = { Icon(painterResource(R.drawable.ic_three_bars), null) },
            modifier = Modifier.clickable(onClick = onMain)
        )
        ListItem(
            headlineContent = { Text("Import CSV") },
            leadingContent = { Icon(painterResource(R.drawable.ic_import), null) },
            modifier = Modifier.clickable(onClick = onImport)
        )
        ListItem(
            headlineContent = { Text("Add Card") },
            leadingContent = { Icon(painterResource(R.drawable.ic_add), null) },
            modifier = Modifier.clickable(onClick = onAddCard)
        )
        ListItem(
            headlineContent = { Text("Settings") },
            leadingContent = { Icon(painterResource(R.drawable.ic_gear), null) },
            modifier = Modifier.clickable(onClick = onSettings)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    onTagClick: (String) -> Unit,
    onCardClick: (CardSearchResult) -> Unit
) {
    val tags by viewModel.tagCounts.collectAsState(initial = emptyList())
    val search by viewModel.search.collectAsState()
    val multiOnly by viewModel.multiCopyOnly.collectAsState()
    val multiCards by viewModel.multiCopyCards.collectAsState(initial = emptyList())
    val dark by viewModel.darkMode.collectAsState(initial = true)

    val searchResults: List<CardSearchResult> by if (search.isBlank()) {
        flowOf(emptyList<CardSearchResult>())
    } else {
        viewModel.searchFlow(search)
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Tracker") },
                navigationIcon = {
                    IconButton(onClick = { /* drawer toggle handled by scaffold */ }) {
                        Icon(painterResource(R.drawable.ic_three_bars), "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setDarkMode(!dark) }) {
                        Icon(
                            painterResource(if (dark) R.drawable.ic_sun else R.drawable.ic_moon),
                            if (dark) "Light mode" else "Dark mode"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = viewModel::setSearch,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search cards by name") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                    singleLine = true
                )
                IconButton(
                    onClick = viewModel::toggleMultiCopyOnly,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_filter),
                        "More than 4 copies",
                        tint = if (multiOnly) androidx.compose.material3.MaterialTheme.colorScheme.primary
                        else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            Text("No cards yet. Import a CSV from the drawer.")
        }
        return
    }
    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
        items(tags) { tag -> TagRow(tag.tag, tag.cardCount, onTagClick) }
    }
}

@Composable
private fun TagRow(tag: String, count: Int, onClick: (String) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick(tag) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tag, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text("$count cards", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
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
                        Text(card.name, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                        Text(
                            "${card.setName} · ${card.rarity} · x${card.quantity}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(card.tag, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardListScreen(
    repository: CardRepository,
    tag: String,
    onBack: () -> Unit,
    onCardClick: (CardSearchResult) -> Unit
) {
    val cards by repository.cardsByTag(tag).collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("‹") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            CardResultList(cards, onCardClick)
        }
    }
}

@Composable
private fun FullscreenOverlay(scryfallId: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier.fillMaxSize().clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
            ) {
                CardImage(scryfallId = scryfallId, modifier = Modifier.fillMaxSize(), large = true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportScreen(
    repository: CardRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val importVm = viewModel { ImportViewModel(repository) }
    val bulkVm = viewModel { ScryfallImportViewModel(scryfall) }
    val state by importVm.state.collectAsState()
    val bulkState by bulkVm.state.collectAsState()
    var tag by remember { mutableStateOf("MegaBox-01") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importVm.importUri(context, it, tag.ifBlank { "Imported" }) }
    }

    val bulkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { bulkVm.importUri(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import CSV") },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = tag, onValueChange = { tag = it },
                label = { Text("Tag (storage location)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { launcher.launch("text/csv") },
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
            ) {
                Text("Choose CSV file")
            }
            when (val s = state) {
                is ImportViewModel.ImportState.Done -> {
                    Toast.makeText(
                        context,
                        "Imported ${s.imported} cards (${s.skipped} skipped)",
                        Toast.LENGTH_LONG
                    ).show()
                    Text("Imported ${s.imported} cards.", Modifier.padding(top = 16.dp))
                }
                is ImportViewModel.ImportState.Error -> Text(
                    "Error: ${s.message}", Modifier.padding(top = 16.dp)
                )
                is ImportViewModel.ImportState.Importing -> Text(
                    "Importing…", Modifier.padding(top = 16.dp)
                )
                else -> Unit
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text(
                "Scryfall reference data",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                "Reference data auto-syncs on launch: if the table is empty it is populated, " +
                    "and every 15 days it checks Scryfall for a newer \"Default Cards\" bulk " +
                    "file. You can also import a file manually. The downloaded jsonl.gz is " +
                    "always deleted after import.",
                Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = { bulkLauncher.launch("*/*") },
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
            ) {
                Text("Choose Scryfall bulk file (jsonl.gz / json)")
            }
            when (val b = bulkState) {
                is ScryfallImportViewModel.State.Done ->
                    Text("Imported ${b.inserted} reference cards.", Modifier.padding(top = 16.dp))
                is ScryfallImportViewModel.State.Error ->
                    Text("Error: ${b.message}", Modifier.padding(top = 16.dp))
                is ScryfallImportViewModel.State.Importing ->
                    Text("Importing reference cards...", Modifier.padding(top = 16.dp))
                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val themeId by viewModel.themeId.collectAsState(initial = "nord")
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Theme", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
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

            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
            AboutCard()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualAddScreen(
    repository: CardRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit
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

    LaunchedEffect(saved) {
        if (saved) {
            Toast.makeText(context, "Card added", Toast.LENGTH_SHORT).show()
            onSaved()
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
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) }
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
                        Text(card.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        Text(
                            "${card.setName} (${card.setCode.uppercase()}) \u00b7 #${card.collectorNumber} \u00b7 ${card.rarity}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                        if (card.manaCost.isNotBlank())
                            Text("Mana: ${card.manaCost}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        if (card.typeLine.isNotBlank())
                            Text(card.typeLine, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
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

@Composable
private fun AboutCard() {
    Card(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_info), null, Modifier.padding(end = 8.dp))
                Text("About", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            }
            Text(
                "Card Tracker helps Magic: The Gathering collectors find cards across their " +
                    "boxes and binders. Card images by Scryfall.",
                Modifier.padding(top = 8.dp)
            )
        }
    }
}

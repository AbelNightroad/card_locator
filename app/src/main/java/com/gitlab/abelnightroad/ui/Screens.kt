package com.gitlab.abelnightroad.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.gitlab.abelnightroad.R
import com.gitlab.abelnightroad.data.BackupStore
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.SettingsStore
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.db.MultiCopyCard
import com.gitlab.abelnightroad.db.TagCount
import com.gitlab.abelnightroad.ui.theme.THEMES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    repository: CardRepository,
    scryfall: ScryfallRepository,
    settings: SettingsStore
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var selectedCard by remember { mutableStateOf<CardSearchResult?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigateTo(target: Screen) {
        scope.launch {
            drawerState.close()
            screen = target
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(Modifier.width(280.dp)) {
                DrawerContent(
                    onImport = { navigateTo(Screen.Import) },
                    onManageTags = { navigateTo(Screen.ManageTags) },
                    onMeta = { navigateTo(Screen.Meta) },
                    onSettings = { navigateTo(Screen.Settings) },
                    onMain = { navigateTo(Screen.Main) }
                )
            }
        }
    ) {
        when (val s = screen) {
            Screen.Main -> MainScreen(
                viewModel = mainViewModel,
                onTagClick = { screen = Screen.Cards(it) },
                onCardClick = { selectedCard = it },
                onAddCard = { screen = Screen.AddCard },
                onMenuClick = { scope.launch { drawerState.open() } }
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
                onBack = { screen = Screen.Main }
            )
            Screen.ManageTags -> ManageTagsScreen(
                repository = repository,
                onBack = { screen = Screen.Main }
            )
            Screen.Settings -> SettingsScreen(
                viewModel = mainViewModel,
                repository = repository,
                onBack = { screen = Screen.Main }
            )
            Screen.Meta -> MetaScreen(
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
    data object ManageTags : Screen
    data object Settings : Screen
    data object Meta : Screen
}

@Composable
private fun DrawerContent(
    onImport: () -> Unit,
    onManageTags: () -> Unit,
    onMeta: () -> Unit,
    onSettings: () -> Unit,
    onMain: () -> Unit
) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Text("MtG Card Tracker", Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge)
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
            headlineContent = { Text("Manage Tags") },
            leadingContent = { Icon(painterResource(R.drawable.ic_add), null) },
            modifier = Modifier.clickable(onClick = onManageTags)
        )
        ListItem(
            headlineContent = { Text("Meta") },
            leadingContent = { Icon(painterResource(R.drawable.ic_meta), null) },
            modifier = Modifier.clickable(onClick = onMeta)
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
    onCardClick: (CardSearchResult) -> Unit,
    onAddCard: () -> Unit,
    onMenuClick: () -> Unit
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
                    IconButton(onClick = onMenuClick) {
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCard) {
                Icon(painterResource(R.drawable.ic_add), "Add Card")
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
                        tint = if (multiOnly) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
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
private fun TagRow(tag: String, count: Long, onClick: (String) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick(tag) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tag, style = MaterialTheme.typography.titleMedium)
            Text("$count cards", style = MaterialTheme.typography.bodyMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardListScreen(
    repository: CardRepository,
    tag: String,
    onBack: () -> Unit,
    onCardClick: (CardSearchResult) -> Unit
) {
    val cards by repository.cardsByTag(tag).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("\u2039") }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 8.dp)) {
            items(cards, key = { it.id }) { card ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            scope.launch { repository.deleteCard(card.id) }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { scope.launch { repository.decrementQuantity(card.id) } },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("\u2212", fontWeight = FontWeight.Bold)
                                }
                                Text("${card.quantity}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                IconButton(
                                    onClick = { scope.launch { repository.incrementQuantity(card.id) } },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
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
                modifier = Modifier.fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
            ) {
                Box(Modifier.aspectRatio(5f / 7f)) {
                    CardImage(scryfallId = scryfallId, modifier = Modifier.fillMaxSize(), large = true)
                }
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
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
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
                    "Importing\u2026", Modifier.padding(top = 16.dp)
                )
                else -> Unit
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text(
                "Scryfall reference data",
                style = MaterialTheme.typography.titleMedium
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
private fun SettingsScreen(
    viewModel: MainViewModel,
    repository: CardRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeId by viewModel.themeId.collectAsState(initial = "nord")
    val scryfallUpdatedAt by viewModel.scryfallUpdatedAt.collectAsState(initial = null)
    var expanded by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf("") }

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
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
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
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
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
}

@Composable
private fun ScryfallCard(scryfallUpdatedAt: String?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scryfall Reference Data", style = MaterialTheme.typography.titleMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualAddScreen(
    repository: CardRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetaScreen(onBack: () -> Unit) {
    val formats = listOf(
        "Standard", "Modern", "Pioneer", "Pauper", "Legacy", "Vintage",
        "Premodern", "Commander", "Brawl"
    )
    var selectedFormat by remember { mutableStateOf("Standard") }
    val metaViewModel: MetaViewModel = viewModel()
    val metaState by metaViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        metaViewModel.loadFormat("Standard")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metagame") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(formats) { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = {
                            selectedFormat = format
                            metaViewModel.loadFormat(format)
                        },
                        label = { Text(format) }
                    )
                }
            }

            when (val state = metaState) {
                is MetaState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a format to view metagame data",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center)
                    }
                }
                is MetaState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Loading metagame data...")
                        }
                    }
                }
                is MetaState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center)
                    }
                }
                is MetaState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.decks) { deck ->
                                    Card(
                                        Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.cardElevation(1.dp)
                                    ) {
                                        Column(
                                            Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                Modifier.fillMaxWidth().aspectRatio(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (deck.coverImageUrl.isNotBlank()) {
                                                    val painter = rememberAsyncImagePainter(
                                                        ImageRequest.Builder(LocalContext.current)
                                                            .data(deck.coverImageUrl)
                                                            .crossfade(true)
                                                            .setHeader("User-Agent", "MtGCardTracker/1.0")
                                                            .build()
                                                    )
                                                    androidx.compose.foundation.Image(
                                                        painter = painter,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(deck.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                textAlign = TextAlign.Center)
                                            Spacer(Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (deck.metaPercentage.isNotBlank()) {
                                                    Text(deck.metaPercentage,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary)
                                                }
                                                if (deck.cost.isNotBlank()) {
                                                    Text(deck.cost,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageTagsScreen(
    repository: CardRepository,
    onBack: () -> Unit
) {
    val tags by repository.tagCounts.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var deleteTag by remember { mutableStateOf<String?>(null) }
    var renameTag by remember { mutableStateOf<String?>(null) }
    var newTagName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Tags") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
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
                            Icon(painterResource(R.drawable.ic_delete), null,
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
                    scope.launch { repository.deleteByTag(tag) }
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
                        scope.launch { repository.renameTag(oldTag, newTagName) }
                    }
                    renameTag = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameTag = null }) { Text("Cancel") }
            }
        )
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
                Text("About", style = MaterialTheme.typography.titleMedium)
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

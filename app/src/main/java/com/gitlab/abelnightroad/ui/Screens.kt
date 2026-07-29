package com.gitlab.abelnightroad.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.Octicons
import compose.icons.octicons.*
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.gitlab.abelnightroad.ui.theme.FONTS
import com.gitlab.abelnightroad.ui.theme.fontFamilyFor
import com.gitlab.abelnightroad.data.BackupStore
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.CsvImport
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.MetaDeckCard
import com.gitlab.abelnightroad.data.MetaDecklistLoader
import com.gitlab.abelnightroad.data.ValidationResult
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.SettingsStore
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.db.DeckEntity
import com.gitlab.abelnightroad.db.DeckWithCards
import com.gitlab.abelnightroad.db.FormatCount
import com.gitlab.abelnightroad.db.MultiCopyCard
import com.gitlab.abelnightroad.db.TagCount
import com.gitlab.abelnightroad.ui.theme.THEMES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOf

private val NAV_ITEMS = listOf(
    SwayNavItem(Octicons.Home24, "Collection", Screen.Main),
    SwayNavItem(Octicons.Book24, "Decks", Screen.Decks()),
    SwayNavItem(Octicons.Tag24, "Tags", Screen.ManageTags),
    SwayNavItem(Octicons.Graph24, "Meta", Screen.Meta),
    SwayNavItem(Octicons.Gear24, "Settings", Screen.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    repository: CardRepository,
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    settings: SettingsStore
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var selectedCard by remember { mutableStateOf<CardSearchResult?>(null) }
    var selectedDeckCardScryfallId by remember { mutableStateOf<String?>(null) }

    val topLevelScreens = NAV_ITEMS.map { it.screen }.toSet()
    val isTopLevel = screen in topLevelScreens
    val selectedNavIndex = NAV_ITEMS.indexOfFirst { it.screen == screen }

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                SwayBottomNavigationBar(
                    items = NAV_ITEMS,
                    selectedIndex = selectedNavIndex.coerceAtLeast(0),
                    onItemSelected = { index -> screen = NAV_ITEMS[index].screen }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val s = screen) {
                Screen.Main -> MainScreen(
                    viewModel = mainViewModel,
                    onTagClick = { screen = Screen.Cards(it) },
                    onCardClick = { selectedCard = it },
                    onAddCard = { screen = Screen.AddCard() }
                )
                is Screen.Cards -> CardListScreen(
                    repository = repository,
                    tag = s.tag,
                    onBack = { screen = Screen.Main },
                    onCardClick = { selectedCard = it }
                )
                is Screen.AddCard -> ManualAddScreen(
                    repository = repository,
                    scryfall = scryfall,
                    onBack = { screen = Screen.Main },
                    initialTag = s.initialTag
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
                    deckRepository = deckRepository,
                    scryfall = scryfall,
                    onBack = { screen = Screen.Main },
                    onDeckClick = { deckId -> screen = Screen.DeckView(deckId, backTo = Screen.Meta) }
                )
                is Screen.Decks -> DecksScreen(
                    deckRepository = deckRepository,
                    initialFormat = s.format,
                    onBack = { screen = Screen.Main },
                    onDeckClick = { deckId, format -> screen = Screen.DeckView(deckId, backTo = Screen.Decks(format)) }
                )
                is Screen.DeckView -> DeckViewScreen(
                    deckRepository = deckRepository,
                    scryfall = scryfall,
                    deckId = s.deckId,
                    onBack = { screen = s.backTo },
                    onCardClick = { scryfallId -> selectedDeckCardScryfallId = scryfallId }
                )
            }
        }
    }

    BackHandler(enabled = screen !is Screen.Main || selectedCard != null || selectedDeckCardScryfallId != null) {
        if (selectedDeckCardScryfallId != null) {
            selectedDeckCardScryfallId = null
        } else if (selectedCard != null) {
            selectedCard = null
        } else {
            screen = Screen.Main
        }
    }

    selectedCard?.let { card ->
        FullscreenOverlay(scryfall = scryfall, scryfallId = card.scryfallId, onDismiss = { selectedCard = null })
    }

    selectedDeckCardScryfallId?.let { scryfallId ->
        FullscreenOverlay(scryfall = scryfall, scryfallId = scryfallId, onDismiss = { selectedDeckCardScryfallId = null })
    }
}

sealed interface Screen {
    data object Main : Screen
    data class Cards(val tag: String) : Screen
    data class AddCard(val initialTag: String = "") : Screen
    data object ManageTags : Screen
    data object Settings : Screen
    data object Meta : Screen
    data class Decks(val format: String? = null) : Screen
    data class DeckView(val deckId: Long, val backTo: Screen = Screen.Decks()) : Screen
}

data class SwayNavItem(
    val icon: ImageVector,
    val label: String,
    val screen: Screen
)

@Composable
private fun SwayBottomNavigationBar(
    items: List<SwayNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
) {
    val animatedProgress = items.indices.map { index ->
        animateFloatAsState(
            targetValue = if (index == selectedIndex) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val progress = animatedProgress[index].value
                val isSelected = index == selectedIndex
                Column(
                    Modifier.weight(1f).clickable { onItemSelected(index) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.offset(y = -(progress * 10).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (progress > 0f) {
                            Box(
                                Modifier.size(iconSize + 14.dp).graphicsLayer { alpha = progress }
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(iconSize),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        modifier = Modifier.graphicsLayer { alpha = progress },
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    onTagClick: (String) -> Unit,
    onCardClick: (CardSearchResult) -> Unit,
    onAddCard: () -> Unit
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
private fun FullscreenOverlay(scryfall: ScryfallRepository, scryfallId: String, onDismiss: () -> Unit) {
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
                    CardImage(scryfall = scryfall, scryfallId = scryfallId, modifier = Modifier.fillMaxSize(), large = true)
                }
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
    val fontId by viewModel.fontId.collectAsState(initial = "roboto")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualAddScreen(
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
                leadingIcon = { Icon(Octicons.Search24, null) }
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
private fun MetaScreen(
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit,
    onDeckClick: (Long) -> Unit
) {
    val formats = listOf(
        "Standard", "Modern", "Pioneer", "Pauper", "Legacy", "Vintage",
        "Premodern", "Commander", "Brawl"
    )
    var selectedFormat by remember { mutableStateOf("Standard") }
    val metaViewModel: MetaViewModel = viewModel()
    val metaState by metaViewModel.state.collectAsState()
    val decklistState by metaViewModel.decklistState.collectAsState()
    var selectedDeck by remember { mutableStateOf<MetaDeckEntry?>(null) }
    var showDecklistDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                                Modifier.fillMaxWidth().clickable {
                                    selectedDeck = deck
                                    showDecklistDialog = true
                                    metaViewModel.loadDecklist(deck.url)
                                },
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
                                        if (deck.metaPercentage.isNotBlank() && deck.cost.isNotBlank()) {
                                            Spacer(Modifier.width(12.dp))
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

    if (showDecklistDialog && selectedDeck != null) {
        val deck = selectedDeck!!
        AlertDialog(
            onDismissRequest = { showDecklistDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deck.name, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val cards = MetaDecklistLoader.load(deck.url)
                                if (cards.isEmpty()) {
                                    Toast.makeText(context, "No cards found to import", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val format = selectedFormat
                                val deckName = deck.name
                                val deckId = deckRepository.createDeck(deckName, format, "meta")
                                val isCommanderFormat = format == "Commander" || format == "Brawl"
                                var commanderColors = ""
                                var warningCount = 0
                                for ((i, c) in cards.withIndex()) {
                                    val actualSlot = if (isCommanderFormat && i == 0) "commander" else c.slot
                                    val scryfallCard = scryfall.lookupByName(c.cardName)
                                    if (scryfallCard != null) {
                                        if (isCommanderFormat && i == 0) {
                                            commanderColors = scryfallCard.colorIdentity
                                        }
                                        if (isCommanderFormat && i > 0 && commanderColors.isNotBlank()
                                            && !DeckRepository.isColorIdentityValid(scryfallCard.colorIdentity, commanderColors)) {
                                            warningCount++
                                            continue
                                        }
                                        deckRepository.addCardToDeck(
                                            deckId = deckId,
                                            scryfallId = scryfallCard.id,
                                            cardName = scryfallCard.name,
                                            setCode = scryfallCard.setCode,
                                            setName = scryfallCard.setName,
                                            collectorNumber = scryfallCard.collectorNumber,
                                            rarity = scryfallCard.rarity,
                                            quantity = c.quantity,
                                            manaCost = scryfallCard.manaCost,
                                            typeLine = scryfallCard.typeLine,
                                            slot = actualSlot
                                        )
                                    } else {
                                        deckRepository.addCardToDeck(
                                            deckId = deckId,
                                            scryfallId = "",
                                            cardName = c.cardName,
                                            setCode = "", setName = "",
                                            collectorNumber = "", rarity = "",
                                            quantity = c.quantity, slot = actualSlot
                                        )
                                    }
                                }
                                val validation = deckRepository.validateDeck(deckId)
                                showDecklistDialog = false
                                val msg = when {
                                    warningCount > 0 -> "Imported $deckName ($warningCount cards skipped for color identity)"
                                    validation is ValidationResult.Invalid -> "Imported $deckName (${validation.errors.size} warnings)"
                                    else -> "Imported $deckName"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                onDeckClick(deckId)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(Octicons.Download24, "Import to Decks")
                    }
                }
            },
            text = {
                when (val dlState = decklistState) {
                    is DecklistState.Loading -> {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Loading decklist...")
                            }
                        }
                    }
                    is DecklistState.Success -> {
                        val grouped = dlState.cards.groupBy { it.slot }
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            for ((slot, cards) in grouped) {
                                if (slot != "mainboard") {
                                    Text(slot.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                }
                                cards.forEach { card ->
                                    Text("${card.quantity}x ${card.cardName}",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    is DecklistState.Error -> {
                        Column {
                            Text("Could not load decklist from MTGGoldfish.",
                                color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(4.dp))
                            Text("${dlState.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    null -> {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDecklistDialog = false }) { Text("Close") }
            }
        )
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
    var addTagName by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Tags") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Octicons.Plus24, "New Tag")
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
                            Icon(Octicons.Trash24, null,
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
                        scope.launch { repository.createTag(addTagName) }
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

private val FORMATS = listOf(
    "All", "Standard", "Modern", "Pioneer", "Pauper", "Legacy", "Vintage",
    "Premodern", "Commander", "Brawl"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DecksScreen(
    deckRepository: DeckRepository,
    initialFormat: String? = null,
    onBack: () -> Unit,
    onDeckClick: (Long, String) -> Unit
) {
    val decksVm: DecksViewModel = viewModel { DecksViewModel(deckRepository) }
    val formatCounts by decksVm.formatCounts.collectAsState()
    val decks by decksVm.decks.collectAsState()
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    var deleteTargetDeck by remember { mutableStateOf<DeckEntity?>(null) }
    var cloneTargetDeck by remember { mutableStateOf<DeckEntity?>(null) }
    var deleteTargetFormat by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialFormat) {
        if (initialFormat != null) decksVm.selectFormat(initialFormat)
    }

    if (selectedFormat != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedFormat!!) },
                    navigationIcon = { IconButton(onClick = { selectedFormat = null }) { Text("\u2039") } }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Octicons.Plus24, "New Deck")
                }
            }
        ) { padding ->
            if (decks.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No decks in this format.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    Modifier.padding(padding).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(decks, key = { it.id }) { deck ->
                        DeckGridCard(
                            deck = deck,
                            deckRepository = deckRepository,
                            onClick = { onDeckClick(deck.id, selectedFormat ?: deck.format) },
                            onDelete = { deleteTargetDeck = deck },
                            onClone = { cloneTargetDeck = deck }
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Decks") },
                    navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Octicons.Plus24, "New Deck")
                }
            }
        ) { padding ->
            if (formatCounts.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No decks yet. Tap + to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    Modifier.padding(padding).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(formatCounts, key = { it.format }) { fc ->
                        var showFormatMenu by remember { mutableStateOf(false) }
                        Card(
                            Modifier.fillMaxWidth().combinedClickable(
                                onClick = {
                                    decksVm.selectFormat(fc.format)
                                    selectedFormat = fc.format
                                },
                                onLongClick = { showFormatMenu = true }
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(fc.format, style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center)
                                Spacer(Modifier.height(4.dp))
                                Text("${fc.deckCount} decks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = showFormatMenu, onDismissRequest = { showFormatMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Delete format", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showFormatMenu = false; deleteTargetFormat = fc.format },
                                    leadingIcon = { Icon(Octicons.Trash24, null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, format ->
                decksVm.createDeck(name, format) { deckId ->
                    showCreateDialog = false
                    onDeckClick(deckId, format)
                }
            }
        )
    }

    deleteTargetDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { deleteTargetDeck = null },
            title = { Text("Delete deck \"${deck.name}\"?") },
            text = { Text("This will permanently delete the deck and all its cards.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { deckRepository.deleteDeck(deck) }
                    deleteTargetDeck = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetDeck = null }) { Text("Cancel") }
            }
        )
    }

    cloneTargetDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { cloneTargetDeck = null },
            title = { Text("Clone deck \"${deck.name}\"?") },
            text = { Text("Creates a copy of this deck with all its cards.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val newId = deckRepository.cloneDeck(deck.id)
                        cloneTargetDeck = null
                        onDeckClick(newId, deck.format)
                    }
                }) { Text("Clone") }
            },
            dismissButton = {
                TextButton(onClick = { cloneTargetDeck = null }) { Text("Cancel") }
            }
        )
    }

    deleteTargetFormat?.let { format ->
        AlertDialog(
            onDismissRequest = { deleteTargetFormat = null },
            title = { Text("Delete format \"$format\"?") },
            text = { Text("All decks in this format will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { deckRepository.deleteDecksByFormat(format) }
                    deleteTargetFormat = null
                    selectedFormat = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetFormat = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckGridCard(
    deck: DeckEntity,
    deckRepository: DeckRepository,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit
) {
    val cardCount by deckRepository.cardCountFlow(deck.id).collectAsState(initial = 0)
    var showMenu by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true }
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                if (deck.coverScryfallId != null) {
                    CardImage(scryfallId = deck.coverScryfallId, modifier = Modifier.fillMaxSize())
                } else {
                    Text("\u2660", style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(deck.name, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, maxLines = 2)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(deck.format, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("$cardCount cards", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Clone") },
                onClick = { showMenu = false; onClone() },
                leadingIcon = { Icon(Octicons.Copy24, null) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Octicons.Trash24, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

@Composable
private fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, format: String) -> Unit
) {
    var deckName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("Standard") }
    var formatExpanded by remember { mutableStateOf(false) }
    val scopeFormats = FORMATS.filter { it != "All" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Deck") },
        text = {
            Column {
                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    label = { Text("Deck name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedButton(onClick = { formatExpanded = true }) {
                        Text("Format: $selectedFormat")
                    }
                    DropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                        scopeFormats.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format) },
                                onClick = {
                                    selectedFormat = format
                                    formatExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (deckName.isNotBlank()) onCreate(deckName, selectedFormat) },
                enabled = deckName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun primaryType(typeLine: String): String {
    val t = typeLine.trim()
    val emdash = t.indexOf("—")
    val typePart = if (emdash > 0) t.substring(0, emdash).trim() else t
    val types = typePart.split(" ").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    return when {
        "Land" in types -> "Land"
        "Creature" in types -> "Creature"
        "Planeswalker" in types -> "Planeswalker"
        "Kindred" in types || "Tribal" in types ->
            types.firstOrNull { it != "Kindred" && it != "Tribal" } ?: "Other"
        types.firstOrNull() != null -> types.first()
        else -> "Other"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckViewScreen(
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    deckId: Long,
    onBack: () -> Unit,
    onCardClick: (String) -> Unit
) {
    val deckWithCards by deckRepository.getDeckWithCards(deckId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var showAddCardDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deckWithCards?.deck?.name ?: "Deck") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCardDialog = true }) {
                Icon(Octicons.Plus24, "Add Card to Deck")
            }
        }
    ) { padding ->
        val cards = deckWithCards?.cards ?: emptyList()
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No cards in this deck yet. Tap + to add cards.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center)
            }
        } else {
            val typeGroupsBySlot = cards.groupBy { it.slot }.mapValues { (_, slotCards) ->
                slotCards.groupBy { primaryType(it.typeLine) }
            }
            val slotOrder = listOf("commander", "companion", "mainboard")
            LazyColumn(Modifier.padding(padding).padding(horizontal = 8.dp)) {
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
                    for ((type, typeCards) in typeGroups) {
                        item {
                            Text(type,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                        }
                        items(typeCards, key = { it.id }) { card ->
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .clickable { onCardClick(card.scryfallId) },
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(card.cardName, style = MaterialTheme.typography.titleSmall)
                                        if (card.manaCost.isNotBlank()) {
                                            Text(card.manaCost,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    if (card.quantity > 1) {
                                                        deckRepository.updateCardQuantity(card.id, card.quantity - 1)
                                                    } else {
                                                        deckRepository.removeCard(card.id)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) { Text("\u2212", fontWeight = FontWeight.Bold) }
                                        Text("${card.quantity}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold)
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    deckRepository.updateCardQuantity(card.id, card.quantity + 1)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) { Text("+", fontWeight = FontWeight.Bold) }
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${card.rarity} \u00b7 ${card.setName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                deckRepository.updateDeckCover(deckId, card.scryfallId)
                                                Toast.makeText(context, "Cover set", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.height(28.dp)
                                    ) { Text("Cover", fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCardDialog) {
        AddCardToDeckDialog(
            scryfall = scryfall,
            deckId = deckId,
            deckRepository = deckRepository,
            format = deckWithCards?.deck?.format ?: "Standard",
            onDismiss = { showAddCardDialog = false }
        )
    }
}

@Composable
private fun AddCardToDeckDialog(
    scryfall: ScryfallRepository,
    deckId: Long,
    deckRepository: DeckRepository,
    format: String = "Standard",
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val suggestions by if (query.length >= 2) {
        scryfall.autocomplete(query)
    } else {
        remember { flowOf(emptyList()) }
    }.collectAsState(initial = emptyList())
    var selectedCard by remember { mutableStateOf<com.gitlab.abelnightroad.db.ScryfallCardEntity?>(null) }
    var selectedSlot by remember { mutableStateOf("mainboard") }
    val scope = rememberCoroutineScope()
    val isCommander = format == "Commander" || format == "Brawl"
    val slotOptions = if (isCommander) listOf("mainboard", "commander", "companion") else listOf("mainboard")
    val context = LocalContext.current

    val deckData by deckRepository.getDeckWithCards(deckId).collectAsState(initial = null)
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
                    leadingIcon = { Icon(Octicons.Search24, null) }
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
                                Text("Color identity conflict — not within commander's (${commanderColors})",
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCard?.let { card ->
                        scope.launch {
                            if (selectedSlot == "commander" && hasCommander) {
                                deckRepository.removeCardsBySlot(deckId, "commander")
                            }
                            if (selectedSlot == "companion" && hasCompanion) {
                                deckRepository.removeCardsBySlot(deckId, "companion")
                            }
                            deckRepository.addCardToDeck(
                                deckId = deckId,
                                scryfallId = card.id,
                                cardName = card.name,
                                setCode = card.setCode,
                                setName = card.setName,
                                collectorNumber = card.collectorNumber,
                                rarity = card.rarity,
                                manaCost = card.manaCost,
                                typeLine = card.typeLine,
                                slot = selectedSlot
                            )
                        }
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

package com.gitlab.abelnightroad.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.SettingsStore
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.ui.navigation.Screen
import com.gitlab.abelnightroad.ui.navigation.FilledBottomNavigationBar
import com.gitlab.abelnightroad.ui.navigation.NAV_ITEMS
import com.gitlab.abelnightroad.ui.components.FullscreenOverlay

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
    val backStack = remember { mutableListOf<Screen>() }

    fun navigate(s: Screen) {
        backStack.add(screen)
        screen = s
    }

    fun goBack(): Boolean {
        if (backStack.isEmpty()) return false
        screen = backStack.removeLast()
        return true
    }

    val selectedNavIndex = NAV_ITEMS.indexOfFirst { it.screen == screen }

    val bottomBar: @Composable () -> Unit = {
        FilledBottomNavigationBar(
            items = NAV_ITEMS,
            selectedIndex = selectedNavIndex.coerceAtLeast(0),
            onItemSelected = { index -> backStack.clear(); screen = NAV_ITEMS[index].screen }
        )
    }

    when (val s = screen) {
        Screen.Main -> MainScreen(
            viewModel = mainViewModel,
            onTagClick = { navigate(Screen.Cards(it)) },
            onCardClick = { selectedCard = it },
            onAddCard = { navigate(Screen.AddCard()) },
            bottomBar = bottomBar
        )
        is Screen.Cards -> CardListScreen(
            repository = repository,
            scryfall = scryfall,
            tag = s.tag,
            onBack = { goBack() },
            onCardClick = { selectedCard = it }
        )
        is Screen.AddCard -> ManualAddScreen(
            repository = repository,
            scryfall = scryfall,
            onBack = { goBack() },
            initialTag = s.initialTag
        )
        Screen.ManageTags -> ManageTagsScreen(
            repository = repository,
            onBack = { goBack() },
            bottomBar = bottomBar
        )
        Screen.Settings -> SettingsScreen(
            viewModel = mainViewModel,
            repository = repository,
            onBack = { goBack() },
            bottomBar = bottomBar
        )
        Screen.Meta -> MetaScreen(
            deckRepository = deckRepository,
            scryfall = scryfall,
            onBack = { goBack() },
            onDeckClick = { deckId -> navigate(Screen.DeckView(deckId)) },
            bottomBar = bottomBar
        )
        is Screen.Decks -> DecksScreen(
            deckRepository = deckRepository,
            initialFormat = s.format,
            onBack = { goBack() },
            onDeckClick = { deckId, format -> navigate(Screen.DeckView(deckId)) },
            onEdhPlayImport = { navigate(Screen.UnifiedImport) },
            bottomBar = bottomBar
        )
        is Screen.DeckView -> DeckViewScreen(
            deckRepository = deckRepository,
            scryfall = scryfall,
            deckId = s.deckId,
            onBack = { goBack() },
            onCardClick = { scryfallId -> selectedDeckCardScryfallId = scryfallId }
        )
        Screen.EdhPlayImport -> EdhPlayImportScreen(
            deckRepository = deckRepository,
            scryfall = scryfall,
            onBack = { goBack() },
            onNavigateToWebView = { url -> navigate(Screen.EdhPlayWebView(url)) },
            onImportComplete = { deckId -> goBack(); navigate(Screen.DeckView(deckId)) }
        )
        Screen.UnifiedImport -> UnifiedImportScreen(
            onBack = { goBack() },
            onImportComplete = { deckId -> goBack(); navigate(Screen.DeckView(deckId)) },
            onNavigateToWebView = { url -> navigate(Screen.EdhPlayWebView(url)) }
        )
        is Screen.EdhPlayWebView -> EdhPlayWebViewScreen(
            deckUrl = s.deckUrl,
            deckRepository = deckRepository,
            scryfall = scryfall,
            onBack = { goBack() },
            onImportComplete = { deckId -> goBack(); navigate(Screen.DeckView(deckId)) }
        )
    }

    BackHandler(enabled = backStack.isNotEmpty() || selectedCard != null || selectedDeckCardScryfallId != null) {
        if (selectedDeckCardScryfallId != null) {
            selectedDeckCardScryfallId = null
        } else if (selectedCard != null) {
            selectedCard = null
        } else {
            goBack()
        }
    }

    selectedCard?.let { card ->
        FullscreenOverlay(scryfallId = card.scryfallId, onDismiss = { selectedCard = null })
    }

    selectedDeckCardScryfallId?.let { scryfallId ->
        FullscreenOverlay(scryfallId = scryfallId, onDismiss = { selectedDeckCardScryfallId = null })
    }
}

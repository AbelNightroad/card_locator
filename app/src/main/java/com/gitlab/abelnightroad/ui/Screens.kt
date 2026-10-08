package com.gitlab.abelnightroad.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.ScanRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.SettingsStore
import com.gitlab.abelnightroad.data.TextRecognitionProcessor
import com.gitlab.abelnightroad.db.CardSearchResult
import com.gitlab.abelnightroad.ui.navigation.Screen
import com.gitlab.abelnightroad.ui.navigation.FilledBottomNavigationBar
import com.gitlab.abelnightroad.ui.navigation.NAV_ITEMS
import com.gitlab.abelnightroad.ui.components.FullscreenOverlay
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    repository: CardRepository,
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    scanRepository: ScanRepository,
    settings: SettingsStore
) {
    val onboardingComplete by settings.onboardingComplete.collectAsState(initial = true)
    val hapticFeedback by settings.hapticFeedback.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (!onboardingComplete) {
        OnboardingScreen(onComplete = {
            scope.launch { settings.setOnboardingComplete() }
        })
        return
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var selectedCard by remember { mutableStateOf<CardSearchResult?>(null) }
    var selectedDeckCardScryfallId by remember { mutableStateOf<String?>(null) }
    val backStack = remember { mutableListOf<Screen>() }
    var currentScanSessionId by remember { mutableStateOf<Long?>(null) }

    val scanProcessor = remember { TextRecognitionProcessor(context) }
    val scanViewModel: ScanViewModel = remember {
        ScanViewModel(scanRepository, repository, scryfall, scanProcessor)
    }
    val scanIsProcessing by scanViewModel.isProcessing.collectAsState()
    val scanCount by scanViewModel.scannedCount.collectAsState()
    val scanLast by scanViewModel.lastScan.collectAsState()

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
            selectedIndex = selectedNavIndex,
            onItemSelected = { index ->
                val target = NAV_ITEMS[index].screen
                if (target != screen) {
                    val history = backStack.toMutableList().apply { add(screen) }
                    val existing = history.indexOf(target)
                    if (existing >= 0) history.subList(existing, history.size).clear()
                    backStack.clear()
                    backStack.addAll(history)
                    screen = target
                }
            }
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
        Screen.Scan -> ScanCameraScreen(
            onBack = { goBack() },
            hapticFeedback = hapticFeedback,
            isProcessing = scanIsProcessing,
            scannedCount = scanCount,
            lastScan = scanLast,
            onImageCaptured = { filePath -> scanViewModel.processImage(filePath) },
            onOpenList = {
                scope.launch {
                    navigate(Screen.ScanResults(scanViewModel.ensureSession()))
                }
            }
        )
        is Screen.ScanResults -> ScanResultsScreen(
            viewModel = scanViewModel,
            sessionId = s.sessionId,
            onBack = { goBack() }
        )
        is Screen.Cards -> CardListScreen(
            repository = repository,
            scryfall = scryfall,
            tag = s.tag,
            onBack = { goBack() }
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
            scryfall = scryfall,
            onBack = { goBack() },
            onManageTags = { navigate(Screen.ManageTags) },
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
            onImport = { navigate(Screen.UnifiedImport) },
            bottomBar = bottomBar
        )
        is Screen.DeckView -> DeckViewScreen(
            deckRepository = deckRepository,
            scryfall = scryfall,
            deckId = s.deckId,
            onBack = { goBack() },
            onCardClick = { scryfallId -> selectedDeckCardScryfallId = scryfallId }
        )
        Screen.UnifiedImport -> UnifiedImportScreen(
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

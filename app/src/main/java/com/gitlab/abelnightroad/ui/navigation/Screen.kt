package com.gitlab.abelnightroad.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Book
import compose.icons.fontawesomeicons.solid.Camera
import compose.icons.fontawesomeicons.solid.ChartBar
import compose.icons.fontawesomeicons.solid.Gear
import compose.icons.fontawesomeicons.solid.House

sealed interface Screen {
    data object Main : Screen
    data object Scan : Screen
    data class ScanResults(val sessionId: Long) : Screen
    data class Cards(val tag: String) : Screen
    data class AddCard(val initialTag: String = "") : Screen
    data object ManageTags : Screen
    data object Settings : Screen
    data object Meta : Screen
    data class Decks(val format: String? = null) : Screen
    data class DeckView(val deckId: Long) : Screen
    data object UnifiedImport : Screen
}

data class SwayNavItem(
    val icon: ImageVector,
    val label: String,
    val screen: Screen
)

val NAV_ITEMS = listOf(
    SwayNavItem(FontAwesomeIcons.Solid.House, "Collection", Screen.Main),
    SwayNavItem(FontAwesomeIcons.Solid.Book, "Decks", Screen.Decks()),
    SwayNavItem(FontAwesomeIcons.Solid.ChartBar, "Meta", Screen.Meta),
    SwayNavItem(FontAwesomeIcons.Solid.Camera, "Scan", Screen.Scan),
    SwayNavItem(FontAwesomeIcons.Solid.Gear, "Settings", Screen.Settings),
)
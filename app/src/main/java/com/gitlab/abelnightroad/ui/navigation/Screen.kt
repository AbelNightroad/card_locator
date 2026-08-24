package com.gitlab.abelnightroad.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.Octicons
import compose.icons.octicons.Book24
import compose.icons.octicons.Gear24
import compose.icons.octicons.Graph24
import compose.icons.octicons.Home24
import compose.icons.octicons.Tag24

sealed interface Screen {
    data object Main : Screen
    data class Cards(val tag: String) : Screen
    data class AddCard(val initialTag: String = "") : Screen
    data object ManageTags : Screen
    data object Settings : Screen
    data object Meta : Screen
    data class Decks(val format: String? = null) : Screen
    data class DeckView(val deckId: Long) : Screen
    data object UnifiedImport : Screen
    data object EdhPlayImport : Screen
    data class EdhPlayWebView(val deckUrl: String) : Screen
}

data class SwayNavItem(
    val icon: ImageVector,
    val label: String,
    val screen: Screen
)

val NAV_ITEMS = listOf(
    SwayNavItem(Octicons.Home24, "Collection", Screen.Main),
    SwayNavItem(Octicons.Book24, "Decks", Screen.Decks()),
    SwayNavItem(Octicons.Tag24, "Tags", Screen.ManageTags),
    SwayNavItem(Octicons.Graph24, "Meta", Screen.Meta),
    SwayNavItem(Octicons.Gear24, "Settings", Screen.Settings),
)
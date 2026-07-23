package com.gitlab.abelnightroad.ui.theme

import androidx.compose.material3.ColorScheme

/**
 * A named theme with a light and dark ColorScheme. Each concrete theme (Catppuccin,
 * Nord, Shades of Purple) is defined in its own file and registers itself here.
 */
data class AppTheme(
    val id: String,
    val label: String,
    val light: androidx.compose.material3.ColorScheme,
    val dark: androidx.compose.material3.ColorScheme
)

val THEMES: List<AppTheme> = listOf(
    CatppuccinTheme,
    NordTheme,
    ShadesOfPurpleTheme
)

fun themeById(id: String): AppTheme = THEMES.firstOrNull { it.id == id } ?: NordTheme

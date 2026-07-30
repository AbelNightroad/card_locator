package com.gitlab.abelnightroad.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.gitlab.abelnightroad.R

enum class AppFont(val id: String, val label: String) {
    ROBOTO("roboto", "Roboto"),
    INTER("inter", "Inter"),
    PLUS_JAKARTA_SANS("plus_jakarta_sans", "Plus Jakarta Sans"),
    COMIC_NEUE("comic_neue", "Comic Neue")
}

val FONTS = AppFont.entries

fun fontFamilyFor(fontId: String): FontFamily = when (fontId) {
    "inter" -> FontFamily(Font(R.font.inter))
    "plus_jakarta_sans" -> FontFamily(Font(R.font.plus_jakarta_sans))
    "comic_neue" -> FontFamily(Font(R.font.comic_neue))
    else -> FontFamily.Default
}

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
    ShadesOfPurpleTheme,
    Cobalt2Theme
)

fun themeById(id: String): AppTheme = THEMES.firstOrNull { it.id == id } ?: NordTheme

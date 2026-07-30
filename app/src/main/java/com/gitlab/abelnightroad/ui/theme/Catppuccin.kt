package com.gitlab.abelnightroad.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Rosewater = Color(0xFFF5E0DC)
private val Flamingo = Color(0xFFF2CDCD)
private val Mauve = Color(0xFF8839EF)
private val Pink = Color(0xFFF5C2E7)
private val Red = Color(0xFFF38BA8)
private val Maroon = Color(0xFFEBA0AC)
private val Peach = Color(0xFFFAB387)
private val Yellow = Color(0xFFF9E2AF)
private val Green = Color(0xFF40A02B)
private val Teal = Color(0xFF179299)
private val Sky = Color(0xFF04A5E5)
private val Sapphire = Color(0xFF209FB5)
private val Blue = Color(0xFF1E66F5)
private val Lavender = Color(0xFF7287FD)
private val TextLight = Color(0xFF4C4F69)
private val SubtextLight = Color(0xFF6C6F85)
private val SurfaceLight = Color(0xFFFDF6E3)
private val BaseLight = Color(0xFFF5E6D0)
private val MantleLight = Color(0xFFF0E2C5)
private val CrustLight = Color(0xFFEBDBBA)
private val TextDark = Color(0xFFCAD3F5)
private val SubtextDark = Color(0xFFA6ADC8)
private val SurfaceDark = Color(0xFF313244)
private val BaseDark = Color(0xFF1E1E2E)
private val MantleDark = Color(0xFF181825)
private val CrustDark = Color(0xFF11111B)

val CatppuccinTheme = AppTheme(
    id = "catppuccin",
    label = "Catppuccin",
    light = lightColorScheme(
        primary = Mauve,
        onPrimary = BaseLight,
        secondary = Blue,
        onSecondary = BaseLight,
        tertiary = Teal,
        background = BaseLight,
        onBackground = TextLight,
        surface = SurfaceLight,
        onSurface = TextLight,
        surfaceVariant = MantleLight,
        onSurfaceVariant = SubtextLight,
        outline = CrustLight,
        error = Red
    ),
    dark = darkColorScheme(
        primary = Mauve,
        onPrimary = BaseDark,
        secondary = Blue,
        onSecondary = BaseDark,
        tertiary = Teal,
        background = BaseDark,
        onBackground = TextDark,
        surface = SurfaceDark,
        onSurface = TextDark,
        surfaceVariant = MantleDark,
        onSurfaceVariant = SubtextDark,
        outline = CrustDark,
        error = Red
    )
)

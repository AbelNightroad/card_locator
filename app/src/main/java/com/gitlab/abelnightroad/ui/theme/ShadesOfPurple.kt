package com.gitlab.abelnightroad.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val BackgroundLight = Color(0xFFF5F3FF)
private val SurfaceLight = Color(0xFFFFFFFF)
private val PrimaryLight = Color(0xFF3B0A91)
private val SecondaryLight = Color(0xFF5B1ED6)
private val AccentLight = Color(0xFF9D4EDD)
private val TextLight = Color(0xFF1A1033)
private val SubtextLight = Color(0xFF5A4B7C)

private val BackgroundDark = Color(0xFF1E1333)
private val SurfaceDark = Color(0xFF2A1B47)
private val PrimaryDark = Color(0xFFB388FF)
private val SecondaryDark = Color(0xFF9D4EDD)
private val AccentDark = Color(0xFFE0AAFF)
private val TextDark = Color(0xFFF3E9FF)
private val SubtextDark = Color(0xFFB9A6D9)

val ShadesOfPurpleTheme = AppTheme(
    id = "shades_of_purple",
    label = "Shades of Purple",
    light = lightColorScheme(
        primary = Color(0xFF4A0E8F),
        onPrimary = SurfaceLight,
        secondary = SecondaryLight,
        onSecondary = SurfaceLight,
        tertiary = Color(0xFFB07CE6),
        background = Color(0xFFEDE4F3),
        onBackground = TextLight,
        surface = Color(0xFFF9F6FC),
        onSurface = TextLight,
        surfaceVariant = BackgroundLight,
        onSurfaceVariant = SubtextLight,
        outline = AccentLight,
        error = Color(0xFFC0392B)
    ),
    dark = darkColorScheme(
        primary = PrimaryDark,
        onPrimary = BackgroundDark,
        secondary = SecondaryDark,
        onSecondary = BackgroundDark,
        tertiary = AccentDark,
        background = BackgroundDark,
        onBackground = TextDark,
        surface = SurfaceDark,
        onSurface = TextDark,
        surfaceVariant = SurfaceDark,
        onSurfaceVariant = SubtextDark,
        outline = AccentDark,
        error = Color(0xFFFF6B6B)
    )
)

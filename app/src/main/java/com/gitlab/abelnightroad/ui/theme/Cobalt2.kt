package com.gitlab.abelnightroad.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val DarkBg = Color(0xFF132738)
private val DarkSurface = Color(0xFF1A3345)
private val DarkSurfaceVariant = Color(0xFF234459)
private val DarkText = Color(0xFFF5F5F5)
private val DarkSubtext = Color(0xFFA0C4D8)
private val Blue = Color(0xFF0088FF)
private val Yellow = Color(0xFFFFDD00)
private val Green = Color(0xFF3AD900)
private val Red = Color(0xFFFF0040)
private val Magenta = Color(0xFFFF0080)
private val Cyan = Color(0xFF00E5FF)
private val Orange = Color(0xFFFF8C00)

private val LightBg = Color(0xFFE8F0F8)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFD0E0F0)
private val LightText = Color(0xFF132738)
private val LightSubtext = Color(0xFF3A6A8A)
private val LightBlue = Color(0xFF0066CC)
private val LightYellow = Color(0xFFCCAA00)
private val LightGreen = Color(0xFF2BA000)
private val LightRed = Color(0xFFCC0033)

val Cobalt2Theme = AppTheme(
    id = "cobalt2",
    label = "Cobalt2",
    light = lightColorScheme(
        primary = LightBlue,
        onPrimary = LightSurface,
        secondary = LightBlue,
        onSecondary = LightSurface,
        tertiary = LightGreen,
        background = LightBg,
        onBackground = LightText,
        surface = LightSurface,
        onSurface = LightText,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightSubtext,
        outline = LightBlue,
        error = LightRed
    ),
    dark = darkColorScheme(
        primary = Blue,
        onPrimary = DarkBg,
        secondary = Cyan,
        onSecondary = DarkBg,
        tertiary = Yellow,
        background = DarkBg,
        onBackground = DarkText,
        surface = DarkSurface,
        onSurface = DarkText,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkSubtext,
        outline = Blue,
        error = Red
    )
)

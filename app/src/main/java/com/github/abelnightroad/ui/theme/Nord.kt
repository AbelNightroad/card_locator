package com.github.abelnightroad.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val PolarNight0 = Color(0xFF2E3440)
private val PolarNight1 = Color(0xFF3B4252)
private val PolarNight2 = Color(0xFF434C5E)
private val PolarNight3 = Color(0xFF4C566A)
private val SnowStorm0 = Color(0xFFD8DEE9)
private val SnowStorm1 = Color(0xFFE5E9F0)
private val SnowStorm2 = Color(0xFFECEFF4)
private val Frost0 = Color(0xFF8FBCBB)
private val Frost1 = Color(0xFF88C0D0)
private val Frost2 = Color(0xFF81A1C1)
private val Frost3 = Color(0xFF5E81AC)
private val AuroraRed = Color(0xFFBF616A)
private val AuroraOrange = Color(0xFFD08770)
private val AuroraYellow = Color(0xFFEBCB8B)
private val AuroraGreen = Color(0xFFA3BE8C)
private val AuroraPurple = Color(0xFFB48EAD)

val NordTheme = AppTheme(
    id = "nord",
    label = "Nord",
    light = lightColorScheme(
        primary = Frost3,
        onPrimary = SnowStorm2,
        secondary = Frost2,
        onSecondary = SnowStorm2,
        tertiary = AuroraPurple,
        background = SnowStorm2,
        onBackground = PolarNight0,
        surface = SnowStorm1,
        onSurface = PolarNight0,
        surfaceVariant = SnowStorm0,
        onSurfaceVariant = PolarNight3,
        outline = SnowStorm0,
        error = AuroraRed
    ),
    dark = darkColorScheme(
        primary = Frost1,
        onPrimary = PolarNight0,
        secondary = Frost0,
        onSecondary = PolarNight0,
        tertiary = AuroraPurple,
        background = PolarNight0,
        onBackground = SnowStorm1,
        surface = PolarNight1,
        onSurface = SnowStorm1,
        surfaceVariant = PolarNight2,
        onSurfaceVariant = SnowStorm0,
        outline = PolarNight3,
        error = AuroraRed
    )
)

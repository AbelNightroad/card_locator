package com.github.abelnightroad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.abelnightroad.ui.theme.themeById

@Composable
fun AppTheme(
    themeId: String,
    dark: Boolean,
    content: @Composable () -> Unit
) {
    val theme = remember(themeId) { themeById(themeId) }
    val colorScheme = if (dark) theme.dark else theme.light
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

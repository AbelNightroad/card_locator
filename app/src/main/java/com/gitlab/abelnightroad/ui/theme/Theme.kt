package com.gitlab.abelnightroad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.gitlab.abelnightroad.ui.theme.themeById

@Composable
fun AppTheme(
    themeId: String,
    dark: Boolean,
    fontId: String = "roboto",
    content: @Composable () -> Unit
) {
    val theme = remember(themeId) { themeById(themeId) }
    val colorScheme = if (dark) theme.dark else theme.light
    val fontFamily = remember(fontId) { fontFamilyFor(fontId) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

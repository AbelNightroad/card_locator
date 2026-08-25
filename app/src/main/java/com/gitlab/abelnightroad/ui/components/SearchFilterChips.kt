package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchFilterChips(
    colorFilter: String?,
    typeFilter: String?,
    rarityFilter: String?,
    onColorClick: (String?) -> Unit,
    onTypeClick: (String?) -> Unit,
    onRarityClick: (String?) -> Unit
) {
    val colors = listOf("W" to "White", "U" to "Blue", "B" to "Black", "R" to "Red", "G" to "Green")
    val types = listOf("Creature", "Instant", "Sorcery", "Artifact", "Enchantment", "Planeswalker", "Land")
    val rarities = listOf("common" to "Common", "uncommon" to "Uncommon", "rare" to "Rare", "mythic" to "Mythic")

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        colors.forEach { (code, label) ->
            FilterChip(
                selected = colorFilter == code,
                onClick = { onColorClick(code) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        types.forEach { type ->
            FilterChip(
                selected = typeFilter == type,
                onClick = { onTypeClick(type) },
                label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        rarities.forEach { (value, label) ->
            FilterChip(
                selected = rarityFilter == value,
                onClick = { onRarityClick(value) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

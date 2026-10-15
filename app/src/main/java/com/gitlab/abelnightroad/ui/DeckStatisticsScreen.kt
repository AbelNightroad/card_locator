package com.gitlab.abelnightroad.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gitlab.abelnightroad.ui.components.BarChart
import com.gitlab.abelnightroad.ui.components.DonutChart
import com.gitlab.abelnightroad.ui.components.PieChartWithLegend
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowDown
import compose.icons.fontawesomeicons.solid.ArrowUp
import compose.icons.fontawesomeicons.solid.PenToSquare
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

private val rarityColorMap = mapOf(
    "common" to Color(0xFF888888),
    "uncommon" to Color(0xFFC0C0C0),
    "rare" to Color(0xFFDAA520),
    "mythic" to Color(0xFFFF6347),
)

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = tooltipState,
        tooltip = { PlainTooltip { Text(label) } },
        enableUserInput = false
    ) {
        Box(
            Modifier
                .size(48.dp)
                .combinedClickable(
                    onClickLabel = label,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = { scope.launch { tooltipState.show() } }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EditGameRecordDialog(
    games: Int,
    wins: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var gamesText by remember { mutableStateOf(games.coerceAtLeast(0).toString()) }
    var winsText by remember { mutableStateOf(wins.coerceAtLeast(0).toString()) }
    val gamesValue = gamesText.toIntOrNull()
    val winsValue = winsText.toIntOrNull()
    val winsExceeds = gamesValue != null && winsValue != null && winsValue > gamesValue
    val valid = gamesValue != null && winsValue != null && isValidGameRecord(gamesValue, winsValue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Game Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gamesText,
                    onValueChange = { gamesText = it.filter(Char::isDigit) },
                    label = { Text("Games") },
                    singleLine = true,
                    isError = gamesValue == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = winsText,
                    onValueChange = { winsText = it.filter(Char::isDigit) },
                    label = { Text("Wins") },
                    singleLine = true,
                    isError = winsValue == null || winsExceeds,
                    supportingText = {
                        if (winsExceeds) Text("Wins cannot exceed games")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(gamesValue!!, winsValue!!) },
                enabled = valid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun DeckStatisticsScreen(deckId: Long, vm: DeckViewViewModel) {
    val deckWithCards by vm.deckWithCards.collectAsState()
    val cards = deckWithCards?.cards ?: emptyList()
    val stats = remember(cards) { computeDeckStats(cards) }
    val totalValue = remember(cards) { cards.sumOf { it.priceUsd * it.quantity } }
    val deck = deckWithCards?.deck
    val record = remember(deck?.gamesPlayed, deck?.gamesWon) {
        gameRecordSummary(deck?.gamesPlayed ?: 0, deck?.gamesWon ?: 0)
    }
    var showEditGameRecord by remember { mutableStateOf(false) }

    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards to show statistics.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center)
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Deck Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("Total", "${stats.totalCards}")
                    StatItem("Avg CMC", String.format("%.1f", stats.avgCmc))
                    StatItem("Lands", "${stats.landCount}")
                    StatItem("Creatures", "${stats.creatureCount}")
                }
                if (totalValue > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Total Value: $${"%.2f".format(totalValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("Wins", "${record.wins}")
                    StatItem("Games", "${record.games}")
                    StatItem("Win Rate", "%.0f%%".format(record.winRatePercent))
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipIconButton("Log Win", FontAwesomeIcons.Solid.ArrowUp) { vm.recordGame(won = true) }
            TooltipIconButton("Log Loss", FontAwesomeIcons.Solid.ArrowDown) { vm.recordGame(won = false) }
            TooltipIconButton("Edit record", FontAwesomeIcons.Solid.PenToSquare) { showEditGameRecord = true }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Mana Value Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                BarChart(data = stats.cmcDistribution)
            }
        }

        if (stats.colorStats.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Color Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    DonutChart(data = stats.colorStats)
                    Spacer(Modifier.height(8.dp))
                    stats.colorStats.forEach { cs ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${cs.color}: ${cs.count} pips")
                            Text("${String.format("%.0f", cs.percentage)}%")
                        }
                    }
                }
            }
        }

        if (stats.typeStats.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Card Types", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    PieChartWithLegend(entries = stats.typeStats.map { it.type to it.count })
                }
            }
        }

        if (stats.rarityStats.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Rarity Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    stats.rarityStats.forEach { rs ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${rs.rarity.replaceFirstChar { it.uppercase() }}: ${rs.count}")
                            Text("${String.format("%.0f", rs.percentage)}%")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showEditGameRecord) {
        EditGameRecordDialog(
            games = record.games,
            wins = record.wins,
            onDismiss = { showEditGameRecord = false },
            onSave = { games, wins ->
                vm.setGameRecord(games, wins)
                showEditGameRecord = false
            }
        )
    }
}

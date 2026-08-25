package com.gitlab.abelnightroad.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gitlab.abelnightroad.ui.components.BarChart
import com.gitlab.abelnightroad.ui.components.DonutChart
import com.gitlab.abelnightroad.ui.components.HorizontalBarChart
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

@Composable
internal fun DeckStatisticsScreen(deckId: Long, vm: DeckViewViewModel) {
    val deckWithCards by vm.deckWithCards.collectAsState()
    val cards = deckWithCards?.cards ?: emptyList()
    val stats = remember(cards) { DeckStatisticsViewModel().computeStats(cards) }
    val totalValue = remember(cards) { cards.sumOf { it.priceUsd * it.quantity } }

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
            }
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
                    HorizontalBarChart(entries = stats.typeStats.map { it.type to it.count })
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
}

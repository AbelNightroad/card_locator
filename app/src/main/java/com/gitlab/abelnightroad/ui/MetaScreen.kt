package com.gitlab.abelnightroad.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.*
import com.gitlab.abelnightroad.data.DeckRepository
import com.gitlab.abelnightroad.data.MetaDeckCard
import com.gitlab.abelnightroad.data.MetaDecklistLoader
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.ValidationResult
import com.gitlab.abelnightroad.ui.components.ScryfallAsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetaScreen(
    deckRepository: DeckRepository,
    scryfall: ScryfallRepository,
    onBack: () -> Unit,
    onDeckClick: (Long) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val formats = ALL_FORMATS
    var selectedFormat by remember { mutableStateOf("Standard") }
    val metaViewModel: MetaViewModel = viewModel()
    val metaState by metaViewModel.state.collectAsState()
    val decklistState by metaViewModel.decklistState.collectAsState()
    var selectedDeck by remember { mutableStateOf<MetaDeckEntry?>(null) }
    var showDecklistDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        metaViewModel.loadFormat("Standard")
    }

    Scaffold(
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = { Text("Metagame") },
                navigationIcon = { IconButton(onClick = onBack) { Text("\u2039") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(formats) { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = {
                            selectedFormat = format
                            metaViewModel.loadFormat(format)
                        },
                        label = { Text(format) }
                    )
                }
            }

            when (val state = metaState) {
                is MetaState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a format to view metagame data",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center)
                    }
                }
                is MetaState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Loading metagame data...")
                        }
                    }
                }
                is MetaState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center)
                    }
                }
                is MetaState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.decks) { deck ->
                            Card(
                                Modifier.fillMaxWidth().clickable {
                                    selectedDeck = deck
                                    showDecklistDialog = true
                                    metaViewModel.loadDecklist(deck.url)
                                },
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(
                                    Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth().height(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (deck.coverImageUrl.isNotBlank()) {
                                            ScryfallAsyncImage(
                                                url = deck.coverImageUrl,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit,
                                                loading = { CircularProgressIndicator() },
                                                error = { Text("X") }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(deck.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false))
                                        if (deck.metaPercentage.isNotBlank()) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(deck.metaPercentage,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDecklistDialog && selectedDeck != null) {
        val deck = selectedDeck!!
        AlertDialog(
            onDismissRequest = { showDecklistDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deck.name, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val cards = MetaDecklistLoader.load(deck.url)
                                if (cards.isEmpty()) {
                                    Toast.makeText(context, "No cards found to import", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val format = selectedFormat
                                val deckName = deck.name
                                val deckId = deckRepository.createDeck(deckName, format, "meta")
                                importDeckCards(deckId, cards, deckRepository, scryfall, format)
                                val validation = deckRepository.validateDeck(deckId)
                                showDecklistDialog = false
                                val msg = when (validation) {
                                    is ValidationResult.Invalid -> "Imported $deckName (${validation.errors.size} warnings)"
                                    else -> "Imported $deckName"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                onDeckClick(deckId)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(FontAwesomeIcons.Solid.Download, "Import to Decks")
                    }
                }
            },
            text = {
                when (val dlState = decklistState) {
                    is DecklistState.Loading -> {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Loading decklist...")
                            }
                        }
                    }
                    is DecklistState.Success -> {
                        val grouped = dlState.cards.groupBy { it.slot }
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            for ((slot, cards) in grouped) {
                                if (slot != "mainboard") {
                                    Text(slot.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                }
                                cards.forEach { card ->
                                    Text("${card.quantity}x ${card.cardName}",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    is DecklistState.Error -> {
                        Column {
                            Text("Could not load decklist from mtgtop8.",
                                color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(4.dp))
                            Text("${dlState.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    null -> {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDecklistDialog = false }) { Text("Close") }
            }
        )
    }
}

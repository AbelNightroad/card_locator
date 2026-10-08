package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowRightArrowLeft
import compose.icons.fontawesomeicons.solid.PenToSquare
import compose.icons.fontawesomeicons.solid.Trash
import com.gitlab.abelnightroad.data.CardConditions
import com.gitlab.abelnightroad.data.PrintingInfo
import com.gitlab.abelnightroad.data.ScryfallImage
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.db.CardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ADDED_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

fun formatAddedDate(raw: String): String {
    val formatted = try {
        ADDED_DATE_FORMAT.format(LocalDateTime.parse(raw))
    } catch (_: Exception) {
        try {
            ADDED_DATE_FORMAT.format(LocalDate.parse(raw))
        } catch (_: Exception) {
            raw
        }
    }
    return "Added on $formatted"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardBottomSheet(
    card: CardEntity,
    scryfall: ScryfallRepository,
    otherTags: List<String>,
    fetchPrintings: suspend (String) -> List<PrintingInfo>,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit,
    onSaveAttributes: (condition: String, foil: String) -> Unit,
    onPickPrinting: (PrintingInfo) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMove by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var priceUsd by remember(card.scryfallId) { mutableStateOf<Double?>(null) }

    LaunchedEffect(card.scryfallId) {
        priceUsd = if (card.scryfallId.isNotBlank()) {
            withContext(Dispatchers.IO) { scryfall.lookupById(card.scryfallId)?.priceUsd }
        } else {
            null
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(63f / 88f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (card.scryfallId.isNotBlank()) {
                    ScryfallAsyncImage(
                        url = ScryfallImage.normal(card.scryfallId),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "\u2660",
                        fontSize = 64.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "${card.quantity}x ${card.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${card.setName} #${card.collectorNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (card.language.isNotBlank()) {
                    SuggestionChip(onClick = {}, label = { Text(card.language) })
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(CardConditions.displayName(card.condition)) }
                )
                if (card.tag.isNotBlank()) {
                    SuggestionChip(onClick = {}, label = { Text(card.tag) })
                }
                if (card.rarity.isNotBlank()) {
                    SuggestionChip(onClick = {}, label = { Text(card.rarity) })
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                formatAddedDate(card.added),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            priceUsd?.let {
                Text(
                    "Market $" + "%.2f".format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showMove = true }) {
                    Icon(FontAwesomeIcons.Solid.ArrowRightArrowLeft, "Move to another tag")
                }
                IconButton(onClick = { showEdit = true }) {
                    Icon(FontAwesomeIcons.Solid.PenToSquare, "Edit card")
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        FontAwesomeIcons.Solid.Trash,
                        "Delete card",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showMove) {
        MoveToTagDialog(
            otherTags = otherTags,
            onMove = { tag -> onMove(tag); onDismiss() },
            onDismiss = { showMove = false }
        )
    }

    if (showEdit) {
        EditCardDialog(
            card = card,
            fetchPrintings = fetchPrintings,
            onSave = onSaveAttributes,
            onPickPrinting = onPickPrinting,
            onDismiss = { showEdit = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete card?") },
            text = { Text("Remove ${card.quantity}x ${card.name} from \"${card.tag}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MoveToTagDialog(
    otherTags: List<String>,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to tag") },
        text = {
            if (otherTags.isEmpty()) {
                Text("No other tags")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    otherTags.forEach { tag ->
                        TextButton(
                            onClick = { onMove(tag) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tag, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

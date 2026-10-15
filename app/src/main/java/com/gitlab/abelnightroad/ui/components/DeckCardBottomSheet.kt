package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
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
import compose.icons.fontawesomeicons.solid.PenToSquare
import compose.icons.fontawesomeicons.solid.Trash
import com.gitlab.abelnightroad.data.CardConditions
import com.gitlab.abelnightroad.data.PrintingInfo
import com.gitlab.abelnightroad.data.ScryfallImage
import com.gitlab.abelnightroad.db.DeckCardEntity

/**
 * Decklist card detail sheet — the deck counterpart of [CardBottomSheet]:
 * image (tap opens the fullscreen overlay), info chips and Edit/Remove
 * actions. Decks have slots instead of tags, so there is no Move action.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeckCardBottomSheet(
    card: DeckCardEntity,
    fetchPrintings: suspend (String) -> List<PrintingInfo>,
    onDismiss: () -> Unit,
    onImageClick: () -> Unit,
    onSaveAttributes: (condition: String) -> Unit,
    onPickPrinting: (PrintingInfo) -> Unit,
    onRemove: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showEdit by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (card.scryfallId.isNotBlank()) {
                            Modifier.clickable(onClick = onImageClick)
                        } else {
                            Modifier
                        }
                    ),
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
                "${card.quantity}x ${card.cardName}",
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
                SuggestionChip(
                    onClick = {},
                    label = { Text(CardConditions.displayName(card.condition)) }
                )
                SuggestionChip(onClick = {}, label = { Text(card.slot) })
                if (card.rarity.isNotBlank()) {
                    SuggestionChip(onClick = {}, label = { Text(card.rarity) })
                }
                if (card.priceUsd > 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("$" + "%.2f".format(card.priceUsd)) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showEdit = true }) {
                    Icon(
                        FontAwesomeIcons.Solid.PenToSquare,
                        "Edit card",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = { showRemoveConfirm = true }) {
                    Icon(
                        FontAwesomeIcons.Solid.Trash,
                        "Remove from deck",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showEdit) {
        EditCardDialog(
            cardKey = card.id,
            cardName = card.cardName,
            setCode = card.setCode,
            setName = card.setName,
            collectorNumber = card.collectorNumber,
            condition = card.condition,
            foil = "normal",
            showFinish = false,
            fetchPrintings = fetchPrintings,
            onSave = { c, _ -> onSaveAttributes(c) },
            onPickPrinting = onPickPrinting,
            onDismiss = { showEdit = false }
        )
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove card?") },
            text = { Text("Remove ${card.quantity}x ${card.cardName} from this deck?") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemove()
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

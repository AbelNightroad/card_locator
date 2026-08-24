package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = 1,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDecrease,
            modifier = Modifier.size(size),
            enabled = quantity > minQuantity
        ) {
            Text("\u2212", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(
            onClick = onIncrease,
            modifier = Modifier.size(size)
        ) {
            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
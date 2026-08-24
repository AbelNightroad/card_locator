package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gitlab.abelnightroad.data.ScryfallImage

@Composable
fun FullscreenOverlay(
    scryfallId: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val url: String = if (scryfallId.isNotBlank()) ScryfallImage.large(scryfallId) else ""

        Box(
            Modifier.fillMaxSize().clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Box(Modifier.aspectRatio(5f / 7f)) {
                    if (url.isNotBlank()) {
                        ScryfallAsyncImage(
                            url = url,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("No image available", modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}
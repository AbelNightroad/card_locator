package com.gitlab.abelnightroad.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.gitlab.abelnightroad.data.ScryfallImage

@Composable
fun CardImage(
    scryfallId: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    artCrop: Boolean = true
) {
    val url = when {
        scryfallId.isBlank() -> ""
        large -> ScryfallImage.large(scryfallId)
        artCrop -> ScryfallImage.artCrop(scryfallId)
        else -> ScryfallImage.normal(scryfallId)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .setHeader("User-Agent", "MtGCardTracker/1.0")
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = { CircularProgressIndicator() },
                error = { Text("Failed to load image") }
            )
        } else {
            Text("No image")
        }
    }
}

package com.gitlab.abelnightroad.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.gitlab.abelnightroad.data.ScryfallImage

@Composable
fun CardImage(
    scryfallId: String,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val url = if (large) ScryfallImage.large(scryfallId) else ScryfallImage.normal(scryfallId)
    val painter = coil.compose.rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .setHeader("User-Agent", "MtGCardTracker/1.0")
            .build()
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
            else -> Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

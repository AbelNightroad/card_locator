package com.gitlab.abelnightroad.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.gitlab.abelnightroad.data.ScryfallImage
import com.gitlab.abelnightroad.data.ScryfallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CardImage(
    scryfallId: String,
    scryfall: ScryfallRepository? = null,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    var cachedUrl by remember(scryfallId) { mutableStateOf<String?>(null) }
    LaunchedEffect(scryfallId) {
        if (scryfall != null && scryfallId.isNotBlank()) {
            val entity = withContext(Dispatchers.IO) { scryfall.lookupById(scryfallId) }
            cachedUrl = entity?.imageUrl
        }
    }
    val url = cachedUrl ?: if (large) ScryfallImage.large(scryfallId) else ScryfallImage.normal(scryfallId)
    val painter = coil.compose.rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .setHeader("User-Agent", "MtGCardTracker/1.0")
            .build()
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val state = painter.state) {
            is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
            is AsyncImagePainter.State.Error -> Text("Failed to load image")
            else -> Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
